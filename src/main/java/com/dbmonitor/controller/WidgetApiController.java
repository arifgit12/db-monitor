package com.dbmonitor.controller;

import com.dbmonitor.model.DashboardWidget;
import com.dbmonitor.service.DashboardWidgetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/widgets")
@Slf4j
public class WidgetApiController {

    @Autowired
    private DashboardWidgetService widgetService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DashboardWidget>> getUserWidgets(@PathVariable Long userId) {
        return ResponseEntity.ok(widgetService.getUserWidgets(userId));
    }

    @GetMapping("/user/{userId}/visible")
    public ResponseEntity<List<DashboardWidget>> getVisibleUserWidgets(@PathVariable Long userId) {
        return ResponseEntity.ok(widgetService.getVisibleUserWidgets(userId));
    }

    @PostMapping
    public ResponseEntity<DashboardWidget> createWidget(@RequestBody DashboardWidget widget) {
        DashboardWidget created = widgetService.createWidget(widget);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DashboardWidget> updateWidget(
            @PathVariable Long id,
            @RequestBody DashboardWidget widget) {
        DashboardWidget updated = widgetService.updateWidget(id, widget);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWidget(@PathVariable Long id) {
        widgetService.deleteWidget(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/{userId}/reorder")
    public ResponseEntity<Void> reorderWidgets(
            @PathVariable Long userId,
            @RequestBody List<Long> widgetIds) {
        widgetService.reorderWidgets(userId, widgetIds);
        return ResponseEntity.ok().build();
    }
}
