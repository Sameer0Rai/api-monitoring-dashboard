package com.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional email over SMTP if credentials are configured, and otherwise degrades
 * to logging the content - this is what lets the password-reset flow work in dev before
 * anyone has set up a real mail provider. See MAIL_USERNAME/MAIL_PASSWORD in .env.example.
 *
 * <p>Builds its own {@link JavaMailSenderImpl} from plain {@code @Value} properties rather
 * than relying on Spring Boot's {@code spring.mail.*} autoconfiguration, so an unset
 * MAIL_USERNAME simply means "mail sender not configured" instead of a startup failure.</p>
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSenderImpl mailSender;
    private final String fromAddress;

    public MailService(@Value("${mail.host:smtp.gmail.com}") String host,
                        @Value("${mail.port:587}") int port,
                        @Value("${mail.username:}") String username,
                        @Value("${mail.password:}") String password) {
        this.fromAddress = username;

        if (username.isBlank() || password.isBlank()) {
            this.mailSender = null;
            log.warn("MAIL_USERNAME/MAIL_PASSWORD not set - transactional emails will be logged, not sent");
            return;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.getJavaMailProperties().put("mail.transport.protocol", "smtp");
        sender.getJavaMailProperties().put("mail.smtp.auth", "true");
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        // Without explicit timeouts JavaMail waits indefinitely on an unresponsive server,
        // which would pin an executor thread for as long as the connection hangs.
        sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "10000");
        sender.getJavaMailProperties().put("mail.smtp.timeout", "10000");
        sender.getJavaMailProperties().put("mail.smtp.writetimeout", "10000");
        this.mailSender = sender;
    }

    /**
     * Sent on a background thread: a Gmail SMTP handshake regularly takes longer than the
     * frontend's 10s HTTP timeout, and the caller has nothing to learn from the result -
     * /forgot-password deliberately responds identically whether or not mail went out.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink, long expirationMinutes) {
        if (mailSender == null) {
            log.info("Password reset link for {} (not emailed - no mail sender configured): {}", toEmail, resetLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your API Monitor password");
        message.setText("""
                We received a request to reset your password.

                Reset it here (this link expires in %d minutes):
                %s

                If you didn't request this, you can safely ignore this email.
                """.formatted(expirationMinutes, resetLink));

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception ex) {
            // An SMTP hiccup shouldn't break the request or reveal account existence to the
            // caller - the link is still available in the logs as a fallback.
            log.error("Failed to send password reset email to {} - reset link: {}", toEmail, resetLink, ex);
        }
    }
}
