package com.dbmonitor.controller;

import com.dbmonitor.model.NotificationPreference;
import com.dbmonitor.repository.NotificationPreferenceRepository;
import com.dbmonitor.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationApiController {

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreference>> getAllPreferences() {
        return ResponseEntity.ok(notificationPreferenceRepository.findAll());
    }

    @GetMapping("/preferences/{userId}")
    public ResponseEntity<NotificationPreference> getPreferencesByUser(@PathVariable Long userId) {
        return notificationPreferenceRepository.findByUserId(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/preferences")
    public ResponseEntity<NotificationPreference> createPreference(@RequestBody NotificationPreference preference) {
        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/preferences/{id}")
    public ResponseEntity<NotificationPreference> updatePreference(
            @PathVariable Long id,
            @RequestBody NotificationPreference preference) {
        return notificationPreferenceRepository.findById(id)
            .map(existing -> {
                if (preference.getEmailEnabled() != null) {
                    existing.setEmailEnabled(preference.getEmailEnabled());
                }
                if (preference.getSmsEnabled() != null) {
                    existing.setSmsEnabled(preference.getSmsEnabled());
                }
                if (preference.getEmailAddress() != null) {
                    existing.setEmailAddress(preference.getEmailAddress());
                }
                if (preference.getPhoneNumber() != null) {
                    existing.setPhoneNumber(preference.getPhoneNumber());
                }
                if (preference.getAlertTypes() != null) {
                    existing.setAlertTypes(preference.getAlertTypes());
                }
                if (preference.getSeverityLevels() != null) {
                    existing.setSeverityLevels(preference.getSeverityLevels());
                }
                return ResponseEntity.ok(notificationPreferenceRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/preferences/{id}")
    public ResponseEntity<Void> deletePreference(@PathVariable Long id) {
        notificationPreferenceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test/email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        try {
            notificationService.testEmailNotification(to);
            return ResponseEntity.ok("Test email sent successfully to: " + to);
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.badRequest().body("Failed to send test email: " + e.getMessage());
        }
    }

    @PostMapping("/test/sms")
    public ResponseEntity<String> testSms(@RequestParam String to) {
        try {
            notificationService.testSmsNotification(to);
            return ResponseEntity.ok("Test SMS sent successfully to: " + to);
        } catch (Exception e) {
            log.error("Failed to send test SMS", e);
            return ResponseEntity.badRequest().body("Failed to send test SMS: " + e.getMessage());
        }
    }
}
