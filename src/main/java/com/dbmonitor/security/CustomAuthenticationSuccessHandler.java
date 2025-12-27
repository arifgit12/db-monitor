package com.dbmonitor.security;

import com.dbmonitor.service.AuditLogService;
import com.dbmonitor.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Lazy
    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String ipAddress = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Update last login time and reset failed attempts
        userService.updateLastLogin(username, ipAddress);
        
        // Log to audit log
        if (auditLogService != null) {
            auditLogService.logAction(null, username, "LOGIN_SUCCESS", 
                "User logged in successfully", ipAddress, userAgent, "SUCCESS", null);
        }
        
        log.info("Successful login for user: {} from IP: {}", username, ipAddress);
        
        super.setDefaultTargetUrl("/");
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
