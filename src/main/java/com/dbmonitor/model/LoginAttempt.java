package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_login_username", columnList = "username"),
    @Index(name = "idx_login_ip", columnList = "ipAddress"),
    @Index(name = "idx_login_timestamp", columnList = "attemptTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column(nullable = false)
    private LocalDateTime attemptTime;
    
    @Column(nullable = false)
    private Boolean successful;
    
    private String failureReason;
    
    private String userAgent;
    
    private String location; // Can be populated via IP geolocation
}
