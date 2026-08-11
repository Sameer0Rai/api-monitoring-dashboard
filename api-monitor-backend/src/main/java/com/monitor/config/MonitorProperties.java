package com.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for every tunable behavior of the monitoring engine.
 *
 * <p>All values live under the {@code monitor.*} prefix in application.yml and can be
 * overridden per-environment via environment variables (Spring relaxed binding turns
 * {@code MONITOR_HEALTHCHECK_REQUESTTIMEOUTMS} into {@code monitor.health-check.request-timeout-ms}).</p>
 *
 * <p>{@code healthCheck.defaultIntervalSeconds}/{@code minIntervalSeconds}/{@code maxIntervalSeconds}
 * back the per-service configurable interval feature: each {@code ApiService} stores its own
 * {@code intervalSeconds} (validated against min/max here at request time), and the scheduler
 * tick decides which services are actually due rather than re-checking everything on every tick.</p>
 *
 * @param scheduler   how often the scheduler "tick" runs (services are only actually checked
 *                    when their own interval has elapsed - see HealthCheckService)
 * @param healthCheck outbound HTTP behavior for each individual check, plus interval bounds
 * @param status      the latency threshold used to classify a successful check as
 *                    SLOW vs HEALTHY (see {@code MonitoringService.classify}); DOWN is
 *                    determined solely by whether the latest check succeeded, not by
 *                    any latency threshold - there is deliberately no "down threshold"
 *                    here
 * @param logs        limits applied when returning raw log history to a client
 * @param analytics   defaults and bounds for the historical analytics endpoint
 */
@ConfigurationProperties(prefix = "monitor")
public record MonitorProperties(
        Scheduler scheduler,
        HealthCheck healthCheck,
        Status status,
        Logs logs,
        Analytics analytics
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
            long totalSweepTimeoutMs,
            int defaultIntervalSeconds,
            int minIntervalSeconds,
            int maxIntervalSeconds
    ) {
    }

    public record Status(
            long slowThresholdMs
    ) {
    }

    public record Logs(
            int defaultLimit,
            int maxLimit
    ) {
    }

    public record Analytics(
            int defaultWindowDays,
            int minWindowDays,
            int maxWindowDays
    ) {
    }
}
