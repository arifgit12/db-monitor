package com.dbmonitor.service;

import com.dbmonitor.model.Alert;
import com.dbmonitor.model.NotificationPreference;
import com.dbmonitor.repository.NotificationPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.List;

@Service
@Slf4j
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Value("${notification.email.from:noreply@dbmonitor.com}")
    private String fromEmail;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${notification.sms.twilio.from-number:}")
    private String twilioFromNumber;

    public void sendAlertNotifications(Alert alert) {
        log.info("Sending notifications for alert: {}", alert.getMessage());

        // Get all notification preferences
        List<NotificationPreference> emailPrefs = notificationPreferenceRepository.findByEmailEnabledTrue();
        List<NotificationPreference> smsPrefs = notificationPreferenceRepository.findBySmsEnabledTrue();

        // Filter based on alert type and severity
        for (NotificationPreference pref : emailPrefs) {
            if (shouldSendNotification(pref, alert)) {
                sendEmailNotification(pref.getEmailAddress(), alert);
            }
        }

        for (NotificationPreference pref : smsPrefs) {
            if (shouldSendNotification(pref, alert)) {
                sendSmsNotification(pref.getPhoneNumber(), alert);
            }
        }
    }

    private boolean shouldSendNotification(NotificationPreference pref, Alert alert) {
        // Check if alert type is in user's preferences
        if (pref.getAlertTypes() != null && !pref.getAlertTypes().isEmpty()) {
            if (!pref.getAlertTypes().contains(alert.getAlertType())) {
                return false;
            }
        }

        // Check if severity level is in user's preferences
        if (pref.getSeverityLevels() != null && !pref.getSeverityLevels().isEmpty()) {
            if (!pref.getSeverityLevels().contains(alert.getSeverity())) {
                return false;
            }
        }

        return true;
    }

    public void sendEmailNotification(String to, Alert alert) {
        if (!emailEnabled || mailSender == null) {
            log.debug("Email notifications are disabled");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Database Monitor Alert: " + alert.getSeverity());
            message.setText(buildEmailBody(alert));
            
            mailSender.send(message);
            log.info("Email notification sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email notification to: {}", to, e);
        }
    }

    public void sendSmsNotification(String to, Alert alert) {
        if (!smsEnabled || twilioAccountSid.isEmpty() || twilioAuthToken.isEmpty()) {
            log.debug("SMS notifications are disabled or not configured");
            return;
        }

        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            
            Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(twilioFromNumber),
                buildSmsBody(alert)
            ).create();
            
            log.info("SMS notification sent to: {} with SID: {}", to, message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS notification to: {}", to, e);
        }
    }

    private String buildEmailBody(Alert alert) {
        return String.format(
            "Alert Type: %s\n" +
            "Severity: %s\n" +
            "Message: %s\n" +
            "Time: %s\n" +
            "Metric Value: %.2f\n" +
            "Threshold: %.2f\n\n" +
            "Please check the database monitor dashboard for more details.",
            alert.getAlertType(),
            alert.getSeverity(),
            alert.getMessage(),
            alert.getCreatedAt(),
            alert.getMetricValue(),
            alert.getThreshold()
        );
    }

    private String buildSmsBody(Alert alert) {
        return String.format(
            "DB Monitor Alert [%s]: %s (Value: %.2f, Threshold: %.2f)",
            alert.getSeverity(),
            alert.getMessage(),
            alert.getMetricValue(),
            alert.getThreshold()
        );
    }

    public void testEmailNotification(String to) {
        if (!emailEnabled || mailSender == null) {
            throw new IllegalStateException("Email notifications are not enabled");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Database Monitor - Test Notification");
        message.setText("This is a test notification from Database Monitor. If you receive this, email notifications are working correctly.");
        
        mailSender.send(message);
        log.info("Test email sent to: {}", to);
    }

    public void testSmsNotification(String to) {
        if (!smsEnabled) {
            throw new IllegalStateException("SMS notifications are not enabled");
        }

        Twilio.init(twilioAccountSid, twilioAuthToken);
        Message.creator(
            new PhoneNumber(to),
            new PhoneNumber(twilioFromNumber),
            "This is a test SMS from Database Monitor."
        ).create();
        
        log.info("Test SMS sent to: {}", to);
    }
}
