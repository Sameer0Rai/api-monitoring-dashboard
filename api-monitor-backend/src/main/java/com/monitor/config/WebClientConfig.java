package com.monitor.config;

import io.netty.channel.ChannelOption;
import com.monitor.security.UrlSecurityValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Produces the single {@link WebClient} used by {@link com.monitor.service.HealthCheckService}
 * to ping monitored URLs.
 *
 * <p>Why WebClient instead of RestTemplate: RestTemplate has been in maintenance mode since
 * Spring 5 and, more importantly for this app, its blocking model means monitoring N services
 * means N sequential HTTP round-trips per sweep. WebClient lets the scheduler fire all health
 * checks concurrently (bounded by {@code monitor.health-check.concurrency}) using Reactor's
 * non-blocking I/O, so one slow/hanging endpoint no longer delays the check of every other
 * endpoint. The rest of the application (controllers, repositories) stays on the standard
 * Spring MVC / JDBC stack - only the outbound health-check calls are reactive.</p>
 *
 * <p>Both the connect timeout and the response timeout are applied here, at the client
 * level, so every request through this WebClient is bounded by default. Retry/backoff
 * behavior is applied per-call in HealthCheckService, since that is where the service
 * being checked (and therefore the log entry to fail) is known.</p>
 *
 * <p><b>Redirects are explicitly not followed.</b> {@link UrlSecurityValidator} validates
 * the registered URL (and re-validates it before every check, to reduce DNS-rebinding
 * risk) - but that validation only ever inspects the URL the user gave us. If the client
 * silently followed a 3xx response to a different host, that destination would never go
 * through the same SSRF check, defeating it entirely (e.g. a public URL that redirects to
 * {@code http://169.254.169.254/}). A 3xx response is therefore returned as-is to
 * {@code HealthCheckService}, which records it as a failed check (its status code is
 * outside the 2xx range) rather than transparently chasing it.</p>
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient healthCheckWebClient(MonitorProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.healthCheck().connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.healthCheck().requestTimeoutMs()))
                // Explicit, not relying on the library default - see class doc above.
                .followRedirect(false);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
