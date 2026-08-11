package com.monitor.service;

import com.monitor.config.MonitorProperties;
import com.monitor.dto.projection.MetricsAggregate;
import com.monitor.exception.ValidationException;
import com.monitor.model.ApiLog;
import com.monitor.model.HealthStatus;
import com.monitor.repository.ApiLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Read-side logic for a monitored service's history and computed health metrics.
 * Split out from {@link ApiServiceService} because it has a different concern (reading
 * monitoring results) and a different set of collaborators (ApiLogRepository, thresholds)
 * than service registration does.
 *
 * <p>Every method takes {@code ownerId} and re-validates ownership via
 * {@code ApiServiceService.findByIdOrThrow(id, ownerId)} before touching any log data - so
 * even though {@code ApiLog} doesn't carry an owner column directly, ownership is enforced
 * through its parent {@code ApiService} on every read, not just at the controller layer.</p>
 */
@Service
@Transactional(readOnly = true)
public class MonitoringService {

    private final ApiServiceService apiServiceService;
    private final ApiLogRepository logRepository;
    private final MonitorProperties properties;

    public MonitoringService(ApiServiceService apiServiceService,
                              ApiLogRepository logRepository,
                              MonitorProperties properties) {
        this.apiServiceService = apiServiceService;
        this.logRepository = logRepository;
        this.properties = properties;
    }

    /**
     * Most recent logs for a service, oldest first (matching the original chart-friendly
     * ordering), bounded to at most {@code monitor.logs.max-limit} entries regardless of
     * what the caller asks for.
     */
    public List<ApiLog> recentLogs(Long serviceId, Long ownerId, Integer requestedLimit) {
        apiServiceService.findByIdOrThrow(serviceId, ownerId); // 404s instead of silently returning []

        if (requestedLimit != null && requestedLimit < 1) {
            throw new ValidationException("limit", "must be a positive integer");
        }

        int limit = requestedLimit == null
                ? properties.logs().defaultLimit()
                : Math.min(requestedLimit, properties.logs().maxLimit());

        Pageable page = PageRequest.of(0, limit);
        List<ApiLog> newestFirst = logRepository.findByApiServiceIdOrderByCheckedAtDesc(serviceId, page);

        Collections.reverse(newestFirst); // charts expect oldest -> newest, matching original behavior
        return newestFirst;
    }

    public Metrics metricsFor(Long serviceId, Long ownerId) {
        apiServiceService.findByIdOrThrow(serviceId, ownerId);

        MetricsAggregate aggregate = logRepository.aggregateMetrics(serviceId);

        long total = aggregate.totalChecks();
        Double avgLatency = aggregate.averageResponseTimeMs();
        long successCount = aggregate.successCount() == null ? 0L : aggregate.successCount();

        double uptime = total == 0 ? 0.0 : (successCount * 100.0 / total);

        ApiLog latest = logRepository.findFirstByApiServiceIdOrderByCheckedAtDesc(serviceId).orElse(null);
        HealthStatus status = classify(latest);

        return new Metrics(status, uptime, avgLatency, total);
    }

    /**
     * Classifies *current* live status primarily from the single most recent check, not
     * from an average - averaging latency across all-time history meant a service back
     * up for hours after one bad night could still show DOWN, and a service currently
     * down but with a fast historical average could show HEALTHY. Neither reflects
     * reality "right now", which is what this endpoint is meant to answer.
     *
     * <ul>
     *   <li>No checks recorded yet -&gt; UNKNOWN.</li>
     *   <li>Latest check failed -&gt; DOWN, regardless of latency.</li>
     *   <li>Latest check succeeded -&gt; SLOW or HEALTHY based on that check's own
     *       response time against {@code monitor.status.slow-threshold-ms}.</li>
     * </ul>
     *
     * <p>There is deliberately no latency-based "down" threshold - a successful response
     * is a successful response; how slow a passing check has to be before it should count
     * as effectively DOWN is a different, debounced-over-time question (e.g. N consecutive
     * slow/failed checks) that this classification isn't trying to answer. A prior version
     * of this config (`monitor.status.down-threshold-ms`) existed for that purpose and was
     * removed since it was never actually consulted here - re-add it only alongside real
     * logic that uses it.</p>
     */
    HealthStatus classify(ApiLog latest) {
        if (latest == null) {
            return HealthStatus.UNKNOWN;
        }
        if (!latest.isSuccess()) {
            return HealthStatus.DOWN;
        }
        if (latest.getResponseTimeMs() > properties.status().slowThresholdMs()) {
            return HealthStatus.SLOW;
        }
        return HealthStatus.HEALTHY;
    }

    public record Metrics(HealthStatus status, double uptimePercentage, Double averageResponseTimeMs, long totalChecks) {
    }
}
