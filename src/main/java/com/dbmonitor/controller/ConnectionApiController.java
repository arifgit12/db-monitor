package com.dbmonitor.controller;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.model.DatabaseMetrics;
import com.dbmonitor.service.DatabaseConnectionService;
import com.dbmonitor.service.MultiDatabaseMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections")
public class ConnectionApiController {

    @Autowired
    private DatabaseConnectionService connectionService;

    @Autowired
    private MultiDatabaseMonitoringService monitoringService;

    @GetMapping
    public ResponseEntity<List<DatabaseConnection>> getAllConnections() {
        return ResponseEntity.ok(connectionService.getAllConnections());
    }

    @GetMapping("/active")
    public ResponseEntity<List<DatabaseConnection>> getActiveConnections() {
        return ResponseEntity.ok(connectionService.getActiveConnections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatabaseConnection> getConnection(@PathVariable Long id) {
        return connectionService.getConnectionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/metrics/current")
    public ResponseEntity<DatabaseMetrics> getCurrentMetrics(@PathVariable Long id) {
        try {
            DatabaseMetrics metrics = monitoringService.getCurrentMetrics(id);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/metrics/history")
    public ResponseEntity<List<DatabaseMetrics>> getMetricsHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit) {
        List<DatabaseMetrics> history = monitoringService.getMetricsHistory(id, limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/metrics/chart-data")
    public ResponseEntity<Map<String, Object>> getChartData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int limit) {
        List<DatabaseMetrics> history = monitoringService.getMetricsHistory(id, limit);
        
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

    @GetMapping("/all/metrics")
    public ResponseEntity<Map<Long, DatabaseMetrics>> getAllMetrics() {
        Map<Long, DatabaseMetrics> metrics = monitoringService.getCurrentMetricsForAllConnections();
        return ResponseEntity.ok(metrics);
    }

    @PostMapping
    public ResponseEntity<DatabaseConnection> createConnection(@RequestBody DatabaseConnection connection) {
        try {
            DatabaseConnection saved = connectionService.saveConnection(connection);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DatabaseConnection> updateConnection(
            @PathVariable Long id,
            @RequestBody DatabaseConnection connection) {
        try {
            connection.setId(id);
            DatabaseConnection updated = connectionService.saveConnection(connection);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        try {
            connectionService.deleteConnection(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatabaseConnection connection = connectionService.getConnectionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
            
            boolean success = connectionService.testConnection(connection);
            result.put("success", success);
            result.put("message", success ? "Connection successful!" : "Connection failed!");
            result.put("status", connection.getLastTestStatus());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error testing connection");
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/{id}/toggle-active")
    public ResponseEntity<DatabaseConnection> toggleActive(@PathVariable Long id) {
        try {
            DatabaseConnection connection = connectionService.toggleActive(id);
            return ResponseEntity.ok(connection);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<DatabaseConnection> setAsDefault(@PathVariable Long id) {
        try {
            DatabaseConnection connection = connectionService.setAsDefault(id);
            return ResponseEntity.ok(connection);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
