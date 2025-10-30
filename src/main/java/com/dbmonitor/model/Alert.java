package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String alertType;
    
    private String severity;
    
    private String message;
    
    private LocalDateTime createdAt;
    
    private Boolean acknowledged;
    
    private LocalDateTime acknowledgedAt;
    
    private Double metricValue;
    
    private Double threshold;
}
