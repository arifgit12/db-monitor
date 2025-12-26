package com.dbmonitor.controller;

import com.dbmonitor.model.LoginAttempt;
import com.dbmonitor.service.SecurityMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
@Slf4j
public class SecurityApiController {

    @Autowired
    private SecurityMonitoringService securityMonitoringService;

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getFraudDetectionAnalytics(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(securityMonitoringService.getFraudDetectionAnalytics(hours));
    }

    @GetMapping("/login-attempts/recent")
    public ResponseEntity<List<LoginAttempt>> getRecentAttempts(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(securityMonitoringService.getRecentAttempts(hours));
    }

    @GetMapping("/login-attempts/user/{username}")
    public ResponseEntity<List<LoginAttempt>> getLoginHistory(@PathVariable String username) {
        return ResponseEntity.ok(securityMonitoringService.getLoginHistory(username));
    }

    @GetMapping("/login-attempts/user/{username}/failed")
    public ResponseEntity<List<LoginAttempt>> getRecentFailedAttempts(
            @PathVariable String username,
            @RequestParam(defaultValue = "60") int minutes) {
        return ResponseEntity.ok(securityMonitoringService.getRecentFailedAttempts(username, minutes));
    }

    @GetMapping("/login-attempts/ip/{ipAddress}")
    public ResponseEntity<List<LoginAttempt>> getLoginHistoryByIp(@PathVariable String ipAddress) {
        return ResponseEntity.ok(securityMonitoringService.getLoginHistoryByIp(ipAddress));
    }

    @GetMapping("/login-attempts/ip/{ipAddress}/failed")
    public ResponseEntity<List<LoginAttempt>> getRecentFailedAttemptsByIp(
            @PathVariable String ipAddress,
            @RequestParam(defaultValue = "60") int minutes) {
        return ResponseEntity.ok(securityMonitoringService.getRecentFailedAttemptsByIp(ipAddress, minutes));
    }

    @GetMapping("/account-status/{username}")
    public ResponseEntity<Map<String, Object>> getAccountStatus(@PathVariable String username) {
        boolean shouldLock = securityMonitoringService.shouldLockAccount(username);
        List<LoginAttempt> recentFailed = securityMonitoringService.getRecentFailedAttempts(username, 60);
        
        return ResponseEntity.ok(Map.of(
            "username", username,
            "shouldLock", shouldLock,
            "recentFailedAttempts", recentFailed.size(),
            "recentAttempts", recentFailed
        ));
    }
}
