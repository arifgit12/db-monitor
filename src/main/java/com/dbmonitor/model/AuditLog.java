package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String action; // LOGIN, LOGOUT, CREATE_USER, UPDATE_USER, DELETE_USER, etc.
    
    @Column(length = 1000)
    private String description;
    
    private String ipAddress;
    
    private String userAgent;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    private String status; // SUCCESS, FAILURE
    
    @Column(length = 2000)
    private String details; // JSON or additional info
}
