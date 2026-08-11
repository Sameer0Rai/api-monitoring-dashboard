package com.monitor.service;

import com.monitor.dto.request.ForgotPasswordRequest;
import com.monitor.dto.request.LoginRequest;
import com.monitor.dto.request.RegisterRequest;
import com.monitor.dto.request.ResetPasswordRequest;
import com.monitor.dto.response.AuthResponse;
import com.monitor.exception.DuplicateEmailException;
import com.monitor.exception.InvalidResetTokenException;
import com.monitor.model.Role;
import com.monitor.model.User;
import com.monitor.repository.UserRepository;
import com.monitor.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MailService mailService;
    private final long expirationMs;
    private final String frontendUrl;
    private final long resetTokenExpirationMinutes;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService,
                        MailService mailService,
                        @Value("${security.jwt.expiration-ms}") long expirationMs,
                        @Value("${app.frontend-url}") String frontendUrl,
                        @Value("${security.reset-token.expiration-minutes}") long resetTokenExpirationMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.expirationMs = expirationMs;
        this.frontendUrl = frontendUrl;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user account ({})", saved.getEmail());

        String token = jwtService.generateToken(saved.getEmail(), saved.getId());
        return AuthResponse.bearer(token, expirationMs, saved.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // Delegates to Spring Security's AuthenticationManager (which uses
        // CustomUserDetailsService + the BCrypt PasswordEncoder bean) rather than comparing
        // password hashes by hand - this throws BadCredentialsException on a mismatch,
        // handled centrally by GlobalExceptionHandler.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated but user record missing - should be unreachable"));

        String token = jwtService.generateToken(user.getEmail(), user.getId());
        return AuthResponse.bearer(token, expirationMs, user.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // Deliberately silent if the email doesn't exist - responding identically either way
        // stops this endpoint being usable to enumerate registered accounts.
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiresAt(Instant.now().plus(resetTokenExpirationMinutes, ChronoUnit.MINUTES));
            userRepository.save(user);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            mailService.sendPasswordResetEmail(normalizedEmail, resetLink, resetTokenExpirationMinutes);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .filter(u -> u.getResetTokenExpiresAt() != null && u.getResetTokenExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidResetTokenException::new);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Password reset completed for {}", user.getEmail());
    }
}
