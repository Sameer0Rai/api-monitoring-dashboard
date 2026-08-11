package com.monitor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request, before Spring Security's own authentication filter. If a valid
 * bearer token is present, it populates the SecurityContext so the rest of the chain (and
 * the controller, via Authentication/@AuthenticationPrincipal) sees an authenticated user.
 * No token, or an invalid one, and the request simply proceeds unauthenticated - it's
 * SecurityConfig's authorization rules that then reject it if the endpoint requires auth.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RequestMatcher permitAllMatcher;

    public JwtAuthenticationFilter(JwtService jwtService,
                                    CustomUserDetailsService userDetailsService,
                                    RequestMatcher permitAllMatcher) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.permitAllMatcher = permitAllMatcher;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Skip token parsing entirely for public routes (auth endpoints, actuator, swagger) -
        // there is nothing to authenticate there and it avoids noisy failures on missing tokens.
        return permitAllMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isValid(token, userDetails.getUsername())) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Malformed/expired/tampered token: leave the context unauthenticated and let
            // Spring Security's normal 401/403 handling take over downstream.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
