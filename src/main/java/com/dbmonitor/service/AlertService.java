package com.dbmonitor.service;

import com.dbmonitor.model.Alert;
import com.dbmonitor.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getUnacknowledgedAlerts() {
        return alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc();
    }

    public List<Alert> getRecentAlerts(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return alertRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
    }

    public List<Alert> getAlertsBySeverity(String severity) {
        return alertRepository.findBySeverityOrderByCreatedAtDesc(severity);
    }

    public Long getUnacknowledgedCount() {
        return alertRepository.countByAcknowledgedFalse();
    }

    public Alert createAlert(Alert alert) {
        Alert saved = alertRepository.save(alert);
        log.info("Alert created: {}", alert.getMessage());
        
        // Send notifications
        if (notificationService != null) {
            try {
                notificationService.sendAlertNotifications(saved);
            } catch (Exception e) {
                log.error("Failed to send alert notifications", e);
            }
        }
        
        return saved;
    }

    public void acknowledgeAlert(Long id) {
        Optional<Alert> alertOpt = alertRepository.findById(id);
        if (alertOpt.isPresent()) {
            Alert alert = alertOpt.get();
            alert.setAcknowledged(true);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alertRepository.save(alert);
            log.info("Alert {} acknowledged", id);
        }
    }

    public void acknowledgeAllAlerts() {
        List<Alert> unacknowledged = getUnacknowledgedAlerts();
        LocalDateTime now = LocalDateTime.now();
        unacknowledged.forEach(alert -> {
            alert.setAcknowledged(true);
            alert.setAcknowledgedAt(now);
        });
        alertRepository.saveAll(unacknowledged);
        log.info("All alerts acknowledged");
    }

    public void deleteAlert(Long id) {
        alertRepository.deleteById(id);
        log.info("Alert {} deleted", id);
    }

    public void clearOldAlerts(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Alert> oldAlerts = alertRepository.findByCreatedAtAfterOrderByCreatedAtDesc(cutoff);
        alertRepository.deleteAll(oldAlerts);
        log.info("Cleared alerts older than {} days", days);
    }
}
