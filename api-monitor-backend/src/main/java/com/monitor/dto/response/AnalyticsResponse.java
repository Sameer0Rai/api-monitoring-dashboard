package com.monitor.dto.response;

public record AnalyticsResponse(
        Long serviceId,
        int windowDays,
        long totalChecks,
        Double averageResponseTimeMs,
        Long fastestResponseTimeMs,
        Long slowestResponseTimeMs,
        double uptimePercentage,
        double downtimePercentage,
        int totalOutages,
        Double averageRecoverySeconds
) {
}
