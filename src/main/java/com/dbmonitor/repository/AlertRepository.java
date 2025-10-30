package com.dbmonitor.repository;

import com.dbmonitor.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    List<Alert> findByAcknowledgedFalseOrderByCreatedAtDesc();
    
    List<Alert> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);
    
    List<Alert> findBySeverityOrderByCreatedAtDesc(String severity);
    
    Long countByAcknowledgedFalse();
}
