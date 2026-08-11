package com.monitor.service;

import com.monitor.config.MonitorProperties;
import com.monitor.model.ApiLog;
import com.monitor.model.ApiService;
import com.monitor.repository.ApiLogRepository;
import com.monitor.repository.ApiServiceRepository;
import com.monitor.security.UrlSecurityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Performs the actual outbound health checks.
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li><b>Concurrency:</b> {@code Flux.fromIterable(...).flatMap(check, concurrency)} runs
 *       up to {@code monitor.health-check.concurrency} checks in parallel, non-blocking - one
 *       slow endpoint no longer delays every other endpoint's check.</li>
 *   <li><b>Retry:</b> a single dropped packet no longer means DOWN. Each check retries with
 *       exponential backoff before the result is recorded as a failure.</li>
 *   <li><b>Per-service intervals:</b> the scheduler tick ({@code HealthCheckScheduler}) runs
 *       frequently (default 15s), but each service is only actually checked once its own
 *       {@code intervalSeconds} has elapsed since {@code lastCheckedAt}. This is a plain
 *       in-memory filter over all services (see {@link #dueForCheck}) rather than a
 *       per-service scheduled job - simple, correct, and entirely adequate at the number of
 *       services a self-hosted monitor actually has (tens to low hundreds), without the
 *       operational overhead of one Spring @Scheduled task per row.</li>
 *   <li><b>SSRF / DNS rebinding:</b> a URL was already validated when the service was
 *       registered (see {@code ApiServiceService.create}), but DNS answers can change
 *       between registration and any given check. Every single check re-resolves and
 *       re-validates the target immediately beforehand via {@link UrlSecurityValidator}
 *       - if it no longer resolves to a public address, the request is never made, and a
 *       failed check is recorded instead (see {@link #buildBlockedLog}).</li>
 *   <li><b>Blocking DB writes on non-blocking threads:</b> WebClient callbacks run on Netty's
 *       event-loop threads. Each save/update (including the DNS/SSRF re-check above, which
 *       does a blocking lookup) is moved onto {@code Schedulers.boundedElastic()} first -
 *       the standard Reactor pattern for mixing blocking work into an otherwise
 *       non-blocking pipeline.</li>
 * </ul>
 */
@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final WebClient webClient;
    private final ApiServiceRepository serviceRepository;
    private final ApiLogRepository logRepository;
    private final MonitorProperties properties;
    private final UrlSecurityValidator urlSecurityValidator;

    public HealthCheckService(WebClient healthCheckWebClient,
                               ApiServiceRepository serviceRepository,
                               ApiLogRepository logRepository,
                               MonitorProperties properties,
                               UrlSecurityValidator urlSecurityValidator) {
        this.webClient = healthCheckWebClient;
        this.serviceRepository = serviceRepository;
        this.logRepository = logRepository;
        this.properties = properties;
        this.urlSecurityValidator = urlSecurityValidator;
    }

    /**
     * Checks every service that is currently due (see {@link #dueForCheck}) concurrently,
     * persisting one {@link ApiLog} and updating {@code lastCheckedAt} per service. Blocks
     * the calling (scheduler) thread until the sweep finishes or the total sweep timeout
     * elapses, whichever comes first.
     */
    public void checkDueServices() {
        Instant now = Instant.now();
        List<ApiService> due = serviceRepository.findAll().stream()
                .filter(service -> dueForCheck(service, now))
                .toList();

        if (due.isEmpty()) {
            return;
        }

        log.info("Starting health-check sweep for {} due service(s)", due.size());

        Flux.fromIterable(due)
                .flatMap(this::checkOneWithRetry, properties.healthCheck().concurrency())
                .timeout(Duration.ofMillis(properties.healthCheck().totalSweepTimeoutMs()))
                .doOnError(ex -> log.warn("Health-check sweep did not finish cleanly: {}", ex.toString()))
                .onErrorResume(ex -> Flux.empty())
                .blockLast();

        log.info("Completed health-check sweep for {} service(s)", due.size());
    }

    private boolean dueForCheck(ApiService service, Instant now) {
        if (service.getLastCheckedAt() == null) {
            return true;
        }
        long elapsedSeconds = Duration.between(service.getLastCheckedAt(), now).getSeconds();
        return elapsedSeconds >= service.getIntervalSeconds();
    }

    private Mono<ApiLog> checkOneWithRetry(ApiService service) {
        Instant start = Instant.now();

        // The SSRF re-check does a blocking DNS lookup, so it runs on boundedElastic,
        // same as the DB writes below - never block Netty's event-loop threads.
        return Mono.fromCallable(() -> urlSecurityValidator.isCurrentlySafe(service.getUrl()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(safe -> safe ? performRequest(service, start) : Mono.just(buildBlockedLog(service, start)))
                .publishOn(Schedulers.boundedElastic())
                .map(this::persist);
    }

    private Mono<ApiLog> performRequest(ApiService service, Instant start) {
        return webClient.get()
                .uri(service.getUrl())
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.backoff(properties.healthCheck().maxRetries(),
                                Duration.ofMillis(properties.healthCheck().backoffInitialMs()))
                        .filter(HealthCheckService::isRetryable)
                        .doBeforeRetry(signal -> log.debug(
                                "Retrying health check for '{}' (attempt {})",
                                service.getName(), signal.totalRetries() + 1)))
                .map(response -> buildLog(service, response.getStatusCode().value(), start))
                .onErrorResume(ex -> Mono.just(buildFailedLog(service, start, ex)));
    }

    private static boolean isRetryable(Throwable ex) {
        // Retry on network-level failures and timeouts; a well-formed 4xx/5xx response
        // is a real answer from the server, not a transient failure, so it is recorded
        // immediately rather than retried.
        return !(ex instanceof WebClientResponseException);
    }

    private ApiLog buildLog(ApiService service, int statusCode, Instant start) {
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        return ApiLog.builder()
                .apiService(service)
                .statusCode(statusCode)
                .responseTimeMs(elapsedMs)
                .success(statusCode >= 200 && statusCode < 300)
                .checkedAt(Instant.now())
                .build();
    }

    private ApiLog buildFailedLog(ApiService service, Instant start, Throwable ex) {
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        int statusCode = 0;
        if (ex instanceof WebClientResponseException wcre) {
            statusCode = wcre.getStatusCode().value();
        }
        log.debug("Health check failed for '{}': {}", service.getName(), ex.toString());
        return ApiLog.builder()
                .apiService(service)
                .statusCode(statusCode)
                .responseTimeMs(elapsedMs)
                .success(false)
                .checkedAt(Instant.now())
                .build();
    }

    private ApiLog buildBlockedLog(ApiService service, Instant start) {
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        return ApiLog.builder()
                .apiService(service)
                .statusCode(0)
                .responseTimeMs(elapsedMs)
                .success(false)
                .checkedAt(Instant.now())
                .build();
    }

    private ApiLog persist(ApiLog apiLog) {
        ApiLog saved = logRepository.save(apiLog);
        // Two separate saves rather than one wrapped in @Transactional: this method is
        // invoked via `this::persist` from inside the class (a Reactor .map() callback),
        // and Spring's @Transactional is proxy-based - a self-invocation like that bypasses
        // the proxy entirely and the annotation would silently do nothing. Splitting them is
        // also fine correctness-wise: if the lastCheckedAt update below were to fail after
        // the log save succeeds, the service is just re-checked slightly early next tick -
        // not a state any part of the app treats as invalid.
        ApiService service = apiLog.getApiService();
        service.setLastCheckedAt(apiLog.getCheckedAt());
        serviceRepository.save(service);
        return saved;
    }
}
