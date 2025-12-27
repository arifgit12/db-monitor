package com.dbmonitor.service;

import com.dbmonitor.model.LoginAttempt;
import com.dbmonitor.repository.LoginAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SecurityMonitoringService {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Value("${security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${security.failed-attempts-window-minutes:15}")
    private int failedAttemptsWindowMinutes;

    @Async
    @Transactional
    public void recordLoginAttempt(String username, String ipAddress, boolean successful, 
                                   String failureReason, String userAgent) {
        try {
            LoginAttempt attempt = LoginAttempt.builder()
                .username(username)
                .ipAddress(ipAddress)
                .attemptTime(LocalDateTime.now())
                .successful(successful)
                .failureReason(failureReason)
                .userAgent(userAgent)
                .build();

            loginAttemptRepository.save(attempt);
            log.debug("Login attempt recorded for user: {}, success: {}", username, successful);
        } catch (Exception e) {
            log.error("Failed to record login attempt", e);
        }
    }

    public boolean shouldLockAccount(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(failedAttemptsWindowMinutes);
        long failedCount = loginAttemptRepository.countRecentFailedAttempts(username, since);
        return failedCount >= maxFailedAttempts;
    }

    public List<LoginAttempt> getRecentFailedAttempts(String username, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginAttemptRepository.findRecentFailedAttempts(username, since);
    }

    public List<LoginAttempt> getRecentFailedAttemptsByIp(String ipAddress, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return loginAttemptRepository.findRecentFailedAttemptsByIp(ipAddress, since);
    }

    public List<LoginAttempt> getLoginHistory(String username) {
        return loginAttemptRepository.findByUsernameOrderByAttemptTimeDesc(username);
    }

    public List<LoginAttempt> getLoginHistoryByIp(String ipAddress) {
        return loginAttemptRepository.findByIpAddressOrderByAttemptTimeDesc(ipAddress);
    }

    public Map<String, Object> getFraudDetectionAnalytics(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<LoginAttempt> attempts = loginAttemptRepository.findByAttemptTimeBetweenOrderByAttemptTimeDesc(since, LocalDateTime.now());
        
        Map<String, Object> analytics = new HashMap<>();
        
        long totalAttempts = attempts.size();
        long failedAttempts = attempts.stream().filter(a -> !a.getSuccessful()).count();
        long successfulAttempts = totalAttempts - failedAttempts;
        
        // Find suspicious IPs (multiple failed attempts)
        Map<String, Long> ipFailureCount = new HashMap<>();
        for (LoginAttempt attempt : attempts) {
            if (!attempt.getSuccessful()) {
                ipFailureCount.merge(attempt.getIpAddress(), 1L, Long::sum);
            }
        }
        
        List<Map.Entry<String, Long>> suspiciousIps = ipFailureCount.entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .toList();
        
        // Find accounts under attack
        Map<String, Long> usernameFailureCount = new HashMap<>();
        for (LoginAttempt attempt : attempts) {
            if (!attempt.getSuccessful()) {
                usernameFailureCount.merge(attempt.getUsername(), 1L, Long::sum);
            }
        }
        
        List<Map.Entry<String, Long>> accountsUnderAttack = usernameFailureCount.entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .toList();
        
        analytics.put("totalAttempts", totalAttempts);
        analytics.put("failedAttempts", failedAttempts);
        analytics.put("successfulAttempts", successfulAttempts);
        analytics.put("failureRate", totalAttempts > 0 ? (double) failedAttempts / totalAttempts * 100 : 0);
        analytics.put("suspiciousIps", suspiciousIps);
        analytics.put("accountsUnderAttack", accountsUnderAttack);
        
        return analytics;
    }

    public List<LoginAttempt> getRecentAttempts(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return loginAttemptRepository.findByAttemptTimeBetweenOrderByAttemptTimeDesc(since, LocalDateTime.now());
    }
}
