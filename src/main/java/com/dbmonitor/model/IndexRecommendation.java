package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "index_recommendations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexRecommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long connectionId;
    
    private String tableName;
    
    private String columnNames; // Comma-separated column names
    
    private String indexType; // BTREE, HASH, FULLTEXT, SPATIAL
    
    private String recommendationReason;
    
    private Integer impactScore; // 1-100, higher means more impact
    
    private Long affectedQueries;
    
    private Double estimatedPerformanceGain;
    
    @Column(columnDefinition = "TEXT")
    private String createIndexStatement;
    
    private String status; // PENDING, APPLIED, REJECTED, OBSOLETE
    
    private LocalDateTime recommendedAt;
    
    private LocalDateTime appliedAt;
}
