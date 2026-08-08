package com.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for every tunable behavior of the monitoring engine.
 *
 * <p>All values live under the {@code monitor.*} prefix in application.yml and can be
 * overridden per-environment via environment variables (Spring relaxed binding turns
 * {@code MONITOR_HEALTHCHECK_REQUESTTIMEOUTMS} into {@code monitor.health-check.request-timeout-ms}).
 * Nothing here is per-service yet - that is intentionally deferred to a later phase
 * (configurable-per-service intervals/timeouts/retries) - but centralizing the *global*
 * defaults now means that future feature is a schema + service change, not a rewrite of
 * how configuration flows through the app.</p>
 *
 * @param scheduler   how often the global health-check sweep runs
 * @param healthCheck outbound HTTP behavior for each individual check
 * @param status      thresholds used to classify a service as HEALTHY / SLOW / DOWN
 * @param logs        limits applied when returning raw log history to a client
 */
@ConfigurationProperties(prefix = "monitor")
public record MonitorProperties(
        Scheduler scheduler,
        HealthCheck healthCheck,
        Status status,
        Logs logs
) {

    public record Scheduler(
            long fixedRateMs
    ) {
    }

    public record HealthCheck(
            long requestTimeoutMs,
            long connectTimeoutMs,
            int maxRetries,
            long backoffInitialMs,
            int concurrency,
            long totalSweepTimeoutMs
    ) {
    }

    public record Status(
            long slowThresholdMs,
            long downThresholdMs
    ) {
    }

    public record Logs(
            int defaultLimit,
            int maxLimit
    ) {
    }
}
