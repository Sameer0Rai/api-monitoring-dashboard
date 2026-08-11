package com.monitor.dto.response;

import java.time.Instant;

public record ApiServiceResponse(
        Long id,
        String name,
        String url,
        int intervalSeconds,
        Instant lastCheckedAt,
        Instant createdAt
) {
}
