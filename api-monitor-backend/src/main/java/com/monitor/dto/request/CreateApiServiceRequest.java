package com.monitor.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for registering a new monitored service, scoped to the authenticated
 * caller (the owner is taken from the JWT, never from the request body - a client can't
 * register a service on someone else's behalf just by putting a different id in the JSON).
 *
 * <p>{@code intervalSeconds} is optional; {@code null} means "use the configured default"
 * (see {@code ApiServiceService}). When provided it's bounds-checked in the service layer
 * against {@code monitor.health-check.min-interval-seconds}/{@code max-interval-seconds}
 * rather than a fixed {@code @Min}/{@code @Max} here, since those bounds are environment
 * configuration, not a compile-time constant.</p>
 */
public record CreateApiServiceRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be 100 characters or fewer")
        String name,

        @NotBlank(message = "url is required")
        @Pattern(
                regexp = "^https?://.+",
                message = "url must start with http:// or https://"
        )
        @Size(max = 2048, message = "url must be 2048 characters or fewer")
        String url,

        @Min(value = 1, message = "intervalSeconds must be positive")
        @Max(value = 86400, message = "intervalSeconds must be 86400 (24h) or less")
        Integer intervalSeconds
) {
}
