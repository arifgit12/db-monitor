package com.dbmonitor.repository;

import com.dbmonitor.model.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {
    List<DashboardWidget> findByUserIdOrderBySortOrder(Long userId);
    List<DashboardWidget> findByUserIdAndVisibleTrueOrderBySortOrder(Long userId);
}
