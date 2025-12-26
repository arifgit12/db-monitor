package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private Boolean emailEnabled;
    
    private Boolean smsEnabled;
    
    private String emailAddress;
    
    private String phoneNumber;
    
    private String alertTypes; // Comma-separated alert types to receive
    
    private String severityLevels; // Comma-separated severity levels (CRITICAL, WARNING, INFO)
}
