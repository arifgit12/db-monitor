package com.dbmonitor.repository;

import com.dbmonitor.model.QueryPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueryPlanRepository extends JpaRepository<QueryPlan, Long> {
    List<QueryPlan> findByQueryMetricsId(Long queryId);
    List<QueryPlan> findByHasFullTableScanTrue();
    List<QueryPlan> findByUsesIndexFalse();
    List<QueryPlan> findByAnalyzedAtAfter(LocalDateTime after);
}
