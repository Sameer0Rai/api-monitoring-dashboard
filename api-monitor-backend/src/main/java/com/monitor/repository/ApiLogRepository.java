package com.monitor.repository;

import com.monitor.dto.projection.AnalyticsAggregate;
import com.monitor.dto.projection.MetricsAggregate;
import com.monitor.model.ApiLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    /**
     * Most recent logs for a service, newest first, bounded by {@code pageable}.
     */
    List<ApiLog> findByApiServiceIdOrderByCheckedAtDesc(Long apiServiceId, Pageable pageable);

    /**
     * Logs since a point in time, oldest first - used by {@code AnalyticsService} to scan
     * for status transitions (outages/recoveries). Bounded by time rather than an unbounded
     * "all history" scan.
     */
    List<ApiLog> findByApiServiceIdAndCheckedAtAfterOrderByCheckedAtAsc(Long apiServiceId, Instant since);

    /**
     * The single most recent check for a service, if any - used to classify current
     * live status primarily from the latest result rather than an average (see
     * {@code MonitoringService.classify}).
     */
    Optional<ApiLog> findFirstByApiServiceIdOrderByCheckedAtDesc(Long apiServiceId);

    /**
     * The last check strictly before a given instant, if any - used by
     * {@code AnalyticsService} to seed the "previous state" when scanning an analytics
     * window, so an outage that was already in progress when the window started isn't
     * misread as a fresh outage beginning exactly at the window boundary.
     */
    Optional<ApiLog> findFirstByApiServiceIdAndCheckedAtBeforeOrderByCheckedAtDesc(Long apiServiceId, Instant before);

    /**
     * All-time uptime/latency snapshot used by the fast dashboard metrics endpoint.
     *
     * <p>Uses a JPQL constructor expression instead of returning {@code Object[]} - see
     * {@link MetricsAggregate} for why: an {@code Object[]} return type for a multi-column,
     * no-GROUP-BY query gets double-wrapped by Spring Data's result processing and throws
     * {@code ClassCastException} on cast. A constructor expression has Hibernate build the
     * DTO directly, with no casting anywhere.</p>
     */
    @Query("""
            SELECT new com.monitor.dto.projection.MetricsAggregate(
                COUNT(l),
                AVG(l.responseTimeMs),
                SUM(CASE WHEN l.success = true THEN 1L ELSE 0L END)
            )
            FROM ApiLog l
            WHERE l.apiService.id = :apiServiceId
            """)
    MetricsAggregate aggregateMetrics(@Param("apiServiceId") Long apiServiceId);

    /**
     * Same pattern as {@link #aggregateMetrics}, extended with min/max latency, for the
     * historical analytics endpoint. {@code since} bounds the window (e.g. last 30 days)
     * so this stays a single indexed range scan even as log volume grows.
     */
    @Query("""
            SELECT new com.monitor.dto.projection.AnalyticsAggregate(
                COUNT(l),
                AVG(l.responseTimeMs),
                MIN(l.responseTimeMs),
                MAX(l.responseTimeMs),
                SUM(CASE WHEN l.success = true THEN 1L ELSE 0L END)
            )
            FROM ApiLog l
            WHERE l.apiService.id = :apiServiceId
              AND l.checkedAt >= :since
            """)
    AnalyticsAggregate aggregateAnalytics(@Param("apiServiceId") Long apiServiceId, @Param("since") Instant since);
}
