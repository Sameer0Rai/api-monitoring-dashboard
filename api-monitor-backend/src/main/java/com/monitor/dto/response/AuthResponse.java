package com.monitor.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String email
) {
    public static AuthResponse bearer(String token, long expiresInMs, String email) {
        return new AuthResponse(token, "Bearer", expiresInMs, email);
    }
}
