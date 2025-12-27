package com.dbmonitor.controller;

import com.dbmonitor.service.PasswordResetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Display forgot password form
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "forgot-password";
    }

    /**
     * Process forgot password request
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Create reset token
            String token = passwordResetService.createPasswordResetToken(email);

            // Send reset email
            passwordResetService.sendPasswordResetEmail(email, token);

            redirectAttributes.addFlashAttribute("message",
                "Password reset instructions have been sent to your email address.");
            redirectAttributes.addFlashAttribute("messageType", "success");

            log.info("Password reset email sent to: {}", email);
        } catch (IllegalArgumentException e) {
            // Don't reveal if email exists or not for security reasons
            log.warn("Password reset requested for non-existent email: {}", email);
            redirectAttributes.addFlashAttribute("message",
                "If the email address exists, password reset instructions have been sent.");
            redirectAttributes.addFlashAttribute("messageType", "info");
        } catch (Exception e) {
            log.error("Error processing password reset request for email: {}", email, e);
            redirectAttributes.addFlashAttribute("message",
                "An error occurred while processing your request. Please try again later.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/forgot-password";
    }

    /**
     * Display reset password form
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        try {
            // Validate token
            passwordResetService.validateToken(token);

            model.addAttribute("token", token);
            model.addAttribute("validToken", true);

            return "reset-password";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid or expired password reset token: {}", token);
            model.addAttribute("validToken", false);
            model.addAttribute("errorMessage", e.getMessage());

            return "reset-password";
        }
    }

    /**
     * Process password reset
     */
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Validate passwords match
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/reset-password?token=" + token;
            }

            // Validate password strength (minimum 8 characters)
            if (password.length() < 8) {
                redirectAttributes.addFlashAttribute("error",
                    "Password must be at least 8 characters long");
                return "redirect:/reset-password?token=" + token;
            }

            // Reset password
            passwordResetService.resetPassword(token, password);

            redirectAttributes.addFlashAttribute("message",
                "Your password has been successfully reset. You can now log in with your new password.");
            redirectAttributes.addFlashAttribute("messageType", "success");

            log.info("Password reset completed successfully for token: {}", token);

            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            log.error("Error resetting password with token: {}", token, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        } catch (Exception e) {
            log.error("Unexpected error resetting password", e);
            redirectAttributes.addFlashAttribute("error",
                "An error occurred while resetting your password. Please try again.");
            return "redirect:/reset-password?token=" + token;
        }
    }
}
