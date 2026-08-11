package com.monitor.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Stateless JWT security. "Stateless" (SessionCreationPolicy.STATELESS) means Spring
 * Security never creates or reads an HttpSession - every request must carry a valid
 * bearer token, which matches a horizontally-scalable REST API (no server-side session
 * affinity needed) far better than the traditional session-cookie model would.
 *
 * <p>{@link #publicEndpoints()} is the single source of truth for which paths skip
 * authentication - it's used both here (permitAll) and by {@link JwtAuthenticationFilter}
 * (shouldNotFilter), so the list only exists in one place.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public RequestMatcher publicEndpoints() {
        return new OrRequestMatcher(
                new AntPathRequestMatcher("/api/auth/**"),
                new AntPathRequestMatcher("/actuator/**"),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/swagger-ui.html"),
                new AntPathRequestMatcher("/v3/api-docs/**")
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     JwtAuthenticationFilter jwtAuthenticationFilter,
                                                     RequestMatcher publicEndpoints,
                                                     org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource,
                                                     RestAuthEntryPoints restAuthEntryPoints) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Stateless token-based API: no browser form submissions to protect against CSRF for.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restAuthEntryPoints)
                        .accessDeniedHandler(restAuthEntryPoints))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpoints).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
