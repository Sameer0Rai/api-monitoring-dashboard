package com.monitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for registering a new monitored service. Kept separate from the
 * {@code ApiService} entity so the persistence model can evolve (e.g. gain an owner_id
 * in a future auth phase) without changing what a client is allowed to submit.
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
        String url
) {
}
