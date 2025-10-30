package com.dbmonitor.repository;

import com.dbmonitor.model.QueryMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryMetricsRepository extends JpaRepository<QueryMetrics, Long> {
    
    List<QueryMetrics> findByIsSlowTrue();
    
    List<QueryMetrics> findByExecutionTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT q FROM QueryMetrics q WHERE q.executionTime >= :since ORDER BY q.executionDurationMs DESC")
    List<QueryMetrics> findTopSlowQueriesSince(LocalDateTime since);
    
    @Query("SELECT COUNT(q) FROM QueryMetrics q WHERE q.executionTime >= :since")
    Long countQueriesSince(LocalDateTime since);
    
    @Query("SELECT AVG(q.executionDurationMs) FROM QueryMetrics q WHERE q.executionTime >= :since")
    Double averageExecutionTimeSince(LocalDateTime since);
}
