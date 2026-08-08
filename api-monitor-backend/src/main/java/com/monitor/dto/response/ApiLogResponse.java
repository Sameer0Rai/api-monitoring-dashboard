package com.monitor.dto.response;

import java.time.Instant;

public record ApiLogResponse(
        Long id,
        int statusCode,
        long responseTimeMs,
        boolean success,
        Instant checkedAt
) {
}
