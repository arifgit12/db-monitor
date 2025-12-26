package com.dbmonitor.service;

import com.dbmonitor.model.DashboardWidget;
import com.dbmonitor.repository.DashboardWidgetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DashboardWidgetService {

    @Autowired
    private DashboardWidgetRepository widgetRepository;

    public List<DashboardWidget> getUserWidgets(Long userId) {
        return widgetRepository.findByUserIdOrderBySortOrder(userId);
    }

    public List<DashboardWidget> getVisibleUserWidgets(Long userId) {
        return widgetRepository.findByUserIdAndVisibleTrueOrderBySortOrder(userId);
    }

    public DashboardWidget createWidget(DashboardWidget widget) {
        DashboardWidget saved = widgetRepository.save(widget);
        log.info("Created widget: {} for user: {}", widget.getTitle(), widget.getUser().getId());
        return saved;
    }

    public DashboardWidget updateWidget(Long widgetId, DashboardWidget updatedWidget) {
        DashboardWidget widget = widgetRepository.findById(widgetId)
            .orElseThrow(() -> new IllegalArgumentException("Widget not found"));
        
        if (updatedWidget.getTitle() != null) {
            widget.setTitle(updatedWidget.getTitle());
        }
        if (updatedWidget.getPositionX() != null) {
            widget.setPositionX(updatedWidget.getPositionX());
        }
        if (updatedWidget.getPositionY() != null) {
            widget.setPositionY(updatedWidget.getPositionY());
        }
        if (updatedWidget.getWidth() != null) {
            widget.setWidth(updatedWidget.getWidth());
        }
        if (updatedWidget.getHeight() != null) {
            widget.setHeight(updatedWidget.getHeight());
        }
        if (updatedWidget.getConfiguration() != null) {
            widget.setConfiguration(updatedWidget.getConfiguration());
        }
        if (updatedWidget.getVisible() != null) {
            widget.setVisible(updatedWidget.getVisible());
        }
        if (updatedWidget.getSortOrder() != null) {
            widget.setSortOrder(updatedWidget.getSortOrder());
        }
        
        DashboardWidget saved = widgetRepository.save(widget);
        log.info("Updated widget: {}", widgetId);
        return saved;
    }

    public void deleteWidget(Long widgetId) {
        widgetRepository.deleteById(widgetId);
        log.info("Deleted widget: {}", widgetId);
    }

    public void reorderWidgets(Long userId, List<Long> widgetIds) {
        for (int i = 0; i < widgetIds.size(); i++) {
            Long widgetId = widgetIds.get(i);
            final int sortOrder = i;
            widgetRepository.findById(widgetId).ifPresent(widget -> {
                if (widget.getUser().getId().equals(userId)) {
                    widget.setSortOrder(sortOrder);
                    widgetRepository.save(widget);
                }
            });
        }
        log.info("Reordered widgets for user: {}", userId);
    }
}
