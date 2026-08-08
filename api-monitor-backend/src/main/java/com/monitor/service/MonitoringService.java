package com.monitor.service;

import com.monitor.config.MonitorProperties;
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
    public List<ApiLog> recentLogs(Long serviceId, Integer requestedLimit) {
        apiServiceService.findByIdOrThrow(serviceId); // 404s instead of silently returning []

        int limit = requestedLimit == null
                ? properties.logs().defaultLimit()
                : Math.min(requestedLimit, properties.logs().maxLimit());

        Pageable page = PageRequest.of(0, limit);
        List<ApiLog> newestFirst = logRepository.findByApiServiceIdOrderByCheckedAtDesc(serviceId, page);

        Collections.reverse(newestFirst); // charts expect oldest -> newest, matching original behavior
        return newestFirst;
    }

    public Metrics metricsFor(Long serviceId) {
        apiServiceService.findByIdOrThrow(serviceId);

        Object[] row = logRepository.aggregateMetrics(serviceId);

        long total = row[0] == null ? 0L : ((Number) row[0]).longValue();
        Double avgLatency = row[1] == null ? null : ((Number) row[1]).doubleValue();
        long successCount = row[2] == null ? 0L : ((Number) row[2]).longValue();

        double uptime = total == 0 ? 0.0 : (successCount * 100.0 / total);
        HealthStatus status = classify(total, avgLatency);

        return new Metrics(status, uptime, avgLatency, total);
    }

    private HealthStatus classify(long total, Double avgLatencyMs) {
        if (total == 0 || avgLatencyMs == null) {
            return HealthStatus.UNKNOWN;
        }
        if (avgLatencyMs > properties.status().downThresholdMs()) {
            return HealthStatus.DOWN;
        }
        if (avgLatencyMs > properties.status().slowThresholdMs()) {
            return HealthStatus.SLOW;
        }
        return HealthStatus.HEALTHY;
    }

    public record Metrics(HealthStatus status, double uptimePercentage, Double averageResponseTimeMs, long totalChecks) {
    }
}
