package com.monitor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "token is required")
        String token,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be at least 8 characters")
        String newPassword
) {
}
