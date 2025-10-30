package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "query_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String queryText;
    
    private LocalDateTime executionTime;
    
    private Long executionDurationMs;
    
    private String queryType;
    
    private Boolean isSlow;
    
    private Integer rowsAffected;
    
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
