package com.monitor.repository;

import com.monitor.model.ApiLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    /**
     * Most recent logs for a service, newest first, bounded by {@code pageable}.
     * Replaces the original "return every log this service has ever produced" behavior,
     * which grows unbounded and was the single most obvious scalability bug in the
     * original schema.
     */
    List<ApiLog> findByApiServiceIdOrderByCheckedAtDesc(Long apiServiceId, Pageable pageable);

    /**
     * Aggregate metrics computed in the database rather than pulling every row into the
     * JVM to average/count them - same "all-time" semantics as the original hand-rolled
     * total()/success()/avgLatency() queries, just combined into a single round-trip.
     */
    @Query("""
            SELECT
                COUNT(l),
                AVG(l.responseTimeMs),
                SUM(CASE WHEN l.success = true THEN 1 ELSE 0 END)
            FROM ApiLog l
            WHERE l.apiService.id = :apiServiceId
            """)
    Object[] aggregateMetrics(@Param("apiServiceId") Long apiServiceId);
}
