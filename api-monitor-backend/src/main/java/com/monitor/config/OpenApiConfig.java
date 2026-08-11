package com.monitor.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the "bearerAuth" scheme referenced by every controller's
 * {@code @SecurityRequirement(name = "bearerAuth")}, so Swagger UI renders an "Authorize"
 * button that attaches the token to every subsequent request in the UI - paste in the
 * token returned from POST /api/auth/login and every protected endpoint becomes callable
 * directly from the docs page.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "API Monitoring Dashboard",
                version = "0.4.0",
                description = "Self-hosted API uptime/latency monitoring platform. "
                        + "Register at /api/auth/register, log in at /api/auth/login, then "
                        + "click Authorize below and paste the returned token to try the rest."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Configuration
public class OpenApiConfig {
}
