package com.dbmonitor.security;

import com.dbmonitor.service.AuditLogService;
import com.dbmonitor.service.SecurityMonitoringService;
import com.dbmonitor.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private SecurityMonitoringService securityMonitoringService;

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String ipAddress = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Record the failed login attempt
        securityMonitoringService.recordLoginAttempt(username, ipAddress, false, 
            exception.getMessage(), userAgent);
        
        // Update user's failed login count
        userService.recordFailedLogin(username);
        
        // Log to audit log
        if (auditLogService != null) {
            auditLogService.logAction(null, username, "LOGIN_FAILED", 
                "Failed login attempt: " + exception.getMessage(), 
                ipAddress, userAgent, "FAILURE", null);
        }
        
        log.warn("Failed login attempt for user: {} from IP: {}", username, ipAddress);
        
        super.setDefaultFailureUrl("/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
