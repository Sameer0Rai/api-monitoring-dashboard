package com.monitor.config;

import io.netty.channel.ChannelOption;
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
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient healthCheckWebClient(MonitorProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.healthCheck().connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.healthCheck().requestTimeoutMs()));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
