package com.dbmonitor.service;

import com.dbmonitor.model.PasswordResetToken;
import com.dbmonitor.model.User;
import com.dbmonitor.repository.PasswordResetTokenRepository;
import com.dbmonitor.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Value("${security.password-reset.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${notification.email.from:noreply@dbmonitor.com}")
    private String fromEmail;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${security.password-reset.enabled:true}")
    private boolean passwordResetEnabled;

    @Value("${server.port:8090}")
    private String serverPort;

    /**
     * Create a password reset token for the given email address
     */
    @Transactional
    public String createPasswordResetToken(String email) {
        if (!passwordResetEnabled) {
            throw new IllegalStateException("Password reset is disabled");
        }

        // Find user by email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiryDate(expiryDate)
            .used(false)
            .createdAt(LocalDateTime.now())
            .build();

        tokenRepository.save(resetToken);

        log.info("Password reset token created for user: {}", user.getUsername());

        if (auditLogService != null) {
            auditLogService.logAction(user.getId(), user.getUsername(), "PASSWORD_RESET_REQUEST",
                "Password reset token created", null, null, "SUCCESS", null);
        }

        return token;
    }

    /**
     * Send password reset email to the user
     */
    public void sendPasswordResetEmail(String email, String token) {
        if (!emailEnabled || mailSender == null) {
            log.warn("Email is disabled. Password reset token: {}", token);
            log.warn("Use this link to reset password: http://localhost:{}/reset-password?token={}",
                serverPort, token);
            return;
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("DB Monitor - Password Reset Request");
            message.setText(buildPasswordResetEmailBody(user.getUsername(), token));

            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Validate a password reset token
     */
    public PasswordResetToken validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (resetToken.getUsed()) {
            throw new IllegalArgumentException("Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Password reset token has expired");
        }

        return resetToken;
    }

    /**
     * Reset password using the token
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validateToken(token);

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        // Reset account lockout if locked
        if (user.getAccountLocked()) {
            user.setAccountLocked(false);
            user.setLockoutTime(null);
            user.setFailedLoginAttempts(0);
        }

        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getUsername());

        if (auditLogService != null) {
            auditLogService.logAction(user.getId(), user.getUsername(), "PASSWORD_RESET",
                "Password reset completed via reset token", null, null, "SUCCESS", null);
        }
    }

    /**
     * Build password reset email body
     */
    private String buildPasswordResetEmailBody(String username, String token) {
        String resetLink = String.format("http://localhost:%s/reset-password?token=%s",
            serverPort, token);

        return String.format(
            "Hello %s,\n\n" +
            "You have requested to reset your password for DB Monitor.\n\n" +
            "Please click the link below to reset your password:\n" +
            "%s\n\n" +
            "This link will expire in %d hours.\n\n" +
            "If you did not request this password reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "DB Monitor Team",
            username,
            resetLink,
            tokenExpiryHours
        );
    }

    /**
     * Clean up expired and used tokens (runs daily at midnight)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired password reset tokens");

        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        tokenRepository.deleteUsedTokens();

        log.info("Completed cleanup of expired password reset tokens");
    }

    /**
     * Check if a user has a valid reset token
     */
    public boolean hasValidToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByUser(userOpt.get());
        return tokenOpt.isPresent() && tokenOpt.get().isValid();
    }

    /**
     * Get token expiry hours for display purposes
     */
    public int getTokenExpiryHours() {
        return tokenExpiryHours;
    }
}
