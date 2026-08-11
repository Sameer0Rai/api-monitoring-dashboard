package com.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * A single, explicit, environment-driven CORS allow-list (replacing the old blanket
 * {@code @CrossOrigin}, which allowed every origin).
 *
 * <p>Exposed as a {@link CorsConfigurationSource} bean rather than via
 * {@code WebMvcConfigurer#addCorsMappings} because Spring Security's filter chain runs
 * before the DispatcherServlet - a WebMvc-level CORS mapping never gets a chance to
 * answer a preflight OPTIONS request if Security rejects it first. Wiring this bean into
 * {@code SecurityConfig} via {@code .cors(...)} makes Security itself CORS-aware, so
 * preflight requests are answered correctly before authentication is even considered.</p>
 */
@Configuration
public class WebConfig {

    private final List<String> allowedOrigins;

    public WebConfig(@Value("${monitor.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.asList(allowedOrigins.split(","));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
