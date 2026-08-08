package com.monitor.service;

import com.monitor.config.MonitorProperties;
import com.monitor.model.ApiLog;
import com.monitor.model.ApiService;
import com.monitor.repository.ApiLogRepository;
import com.monitor.repository.ApiServiceRepository;
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
 * <p>Design notes (this is the piece of the original code that most needed rethinking):</p>
 * <ul>
 *   <li><b>Concurrency:</b> the original scheduler looped over services sequentially with a
 *       blocking RestTemplate call each - one slow endpoint delayed every check behind it.
 *       Here, {@code Flux.fromIterable(...).flatMap(check, concurrency)} runs up to
 *       {@code monitor.health-check.concurrency} checks in parallel, non-blocking.</li>
 *   <li><b>Retry:</b> a single dropped packet no longer means DOWN. Each check retries with
 *       exponential backoff ({@code monitor.health-check.max-retries} attempts) before the
 *       result is recorded as a failure.</li>
 *   <li><b>Blocking DB writes on non-blocking threads:</b> WebClient callbacks run on Netty's
 *       event-loop threads. Calling a blocking JPA repository directly from one of those
 *       threads would stall the event loop for other in-flight requests. Each save is moved
 *       onto {@code Schedulers.boundedElastic()} first, which is the standard Reactor pattern
 *       for mixing blocking I/O into an otherwise non-blocking pipeline.</li>
 * </ul>
 */
@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final WebClient webClient;
    private final ApiServiceRepository serviceRepository;
    private final ApiLogRepository logRepository;
    private final MonitorProperties properties;

    public HealthCheckService(WebClient healthCheckWebClient,
                               ApiServiceRepository serviceRepository,
                               ApiLogRepository logRepository,
                               MonitorProperties properties) {
        this.webClient = healthCheckWebClient;
        this.serviceRepository = serviceRepository;
        this.logRepository = logRepository;
        this.properties = properties;
    }

    /**
     * Checks every registered service concurrently and persists one {@link ApiLog} per
     * service. Blocks the calling (scheduler) thread until the whole sweep finishes or the
     * total sweep timeout elapses, whichever comes first - the scheduler itself stays a
     * simple, synchronous method as far as Spring's TaskScheduler is concerned.
     */
    public void checkAllServices() {
        List<ApiService> services = serviceRepository.findAll();
        if (services.isEmpty()) {
            log.debug("No services registered - skipping health-check sweep");
            return;
        }

        log.info("Starting health-check sweep for {} service(s)", services.size());

        Flux.fromIterable(services)
                .flatMap(this::checkOneWithRetry, properties.healthCheck().concurrency())
                .timeout(Duration.ofMillis(properties.healthCheck().totalSweepTimeoutMs()))
                .doOnError(ex -> log.warn("Health-check sweep did not finish cleanly: {}", ex.toString()))
                .onErrorResume(ex -> Flux.empty())
                .blockLast();

        log.info("Completed health-check sweep for {} service(s)", services.size());
    }

    private Mono<ApiLog> checkOneWithRetry(ApiService service) {
        Instant start = Instant.now();

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
                .onErrorResume(ex -> Mono.just(buildFailedLog(service, start, ex)))
                .publishOn(Schedulers.boundedElastic())
                .map(this::persist);
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

    private ApiLog persist(ApiLog apiLog) {
        return logRepository.save(apiLog);
    }
}
