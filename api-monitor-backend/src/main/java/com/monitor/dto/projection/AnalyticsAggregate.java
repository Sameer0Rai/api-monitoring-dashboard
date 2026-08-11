package com.monitor.dto.projection;

/**
 * Aggregate response-time/uptime statistics for a service over some window, computed in
 * the database via a JPQL constructor expression (see {@code ApiLogRepository}) - same
 * reasoning as {@link MetricsAggregate}: no array casting, Hibernate builds this directly.
 */
public record AnalyticsAggregate(
        long totalChecks,
        Double averageResponseTimeMs,
        Long minResponseTimeMs,
        Long maxResponseTimeMs,
        Long successCount
) {
}
