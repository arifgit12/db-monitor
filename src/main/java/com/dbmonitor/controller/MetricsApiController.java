package com.dbmonitor.controller;

import com.dbmonitor.model.DatabaseMetrics;
import com.dbmonitor.service.DatabaseMonitoringService;
import com.dbmonitor.service.QueryMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsApiController {

    @Autowired
    private DatabaseMonitoringService databaseMonitoringService;

    @Autowired
    private QueryMonitoringService queryMonitoringService;

    @GetMapping("/current")
    public ResponseEntity<DatabaseMetrics> getCurrentMetrics() {
        return ResponseEntity.ok(databaseMonitoringService.getCurrentMetrics());
    }

    @GetMapping("/history")
    public ResponseEntity<List<DatabaseMetrics>> getMetricsHistory(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(databaseMonitoringService.getMetricsHistory(limit));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        DatabaseMetrics current = databaseMonitoringService.getCurrentMetrics();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("current", current);
        summary.put("totalQueries", queryMonitoringService.getTotalQueriesInLastHour());
        summary.put("averageQueryTime", queryMonitoringService.getAverageQueryTime(1));
        summary.put("slowQueriesCount", queryMonitoringService.getSlowQueries().size());
        
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/chart-data")
    public ResponseEntity<Map<String, Object>> getChartData(@RequestParam(defaultValue = "30") int limit) {
        List<DatabaseMetrics> history = databaseMonitoringService.getMetricsHistory(limit);
        
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> timestamps = history.stream()
                .map(m -> m.getTimestamp().toString())
                .toList();
        
        List<Integer> activeConnections = history.stream()
                .map(DatabaseMetrics::getActiveConnections)
                .toList();
        
        List<Double> cpuUsage = history.stream()
                .map(DatabaseMetrics::getCpuUsage)
                .toList();
        
        List<Double> memoryUsage = history.stream()
                .map(DatabaseMetrics::getMemoryUsage)
                .toList();
        
        chartData.put("timestamps", timestamps);
        chartData.put("activeConnections", activeConnections);
        chartData.put("cpuUsage", cpuUsage);
        chartData.put("memoryUsage", memoryUsage);
        
        return ResponseEntity.ok(chartData);
    }
}
