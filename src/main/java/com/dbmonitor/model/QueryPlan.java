package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "query_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "query_id")
    private QueryMetrics queryMetrics;
    
    @Column(columnDefinition = "TEXT")
    private String queryText;
    
    @Column(columnDefinition = "TEXT")
    private String executionPlan;
    
    private String planType; // EXPLAIN, EXPLAIN ANALYZE, EXECUTION PLAN
    
    private Double estimatedCost;
    
    private Long estimatedRows;
    
    private Long actualRows;
    
    private Integer executionTimeMs;
    
    private Boolean usesIndex;
    
    private String indexesUsed;
    
    private Boolean hasFullTableScan;
    
    private LocalDateTime analyzedAt;
}
