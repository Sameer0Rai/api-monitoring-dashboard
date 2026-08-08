package com.monitor.dto.response;

import com.monitor.model.HealthStatus;

public record ApiMetricsResponse(
        Long serviceId,
        HealthStatus status,
        double uptimePercentage,
        Double averageResponseTimeMs,
        long totalChecks
) {
}
