package com.dbmonitor.controller;

import com.dbmonitor.model.QueryMetrics;
import com.dbmonitor.service.QueryMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queries")
public class QueryApiController {

    @Autowired
    private QueryMonitoringService queryMonitoringService;

    @GetMapping
    public ResponseEntity<List<QueryMetrics>> getAllQueries() {
        return ResponseEntity.ok(queryMonitoringService.getAllQueries());
    }

    @GetMapping("/slow")
    public ResponseEntity<List<QueryMetrics>> getSlowQueries() {
        return ResponseEntity.ok(queryMonitoringService.getSlowQueries());
    }

    @GetMapping("/slow/recent")
    public ResponseEntity<List<QueryMetrics>> getRecentSlowQueries(@RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(queryMonitoringService.getRecentSlowQueries(hours));
    }

    @GetMapping("/range")
    public ResponseEntity<List<QueryMetrics>> getQueriesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(queryMonitoringService.getQueriesBetween(start, end));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getQueryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQueriesLastHour", queryMonitoringService.getTotalQueriesInLastHour());
        stats.put("averageQueryTime1h", queryMonitoringService.getAverageQueryTime(1));
        stats.put("averageQueryTime24h", queryMonitoringService.getAverageQueryTime(24));
        stats.put("slowQueriesCount", queryMonitoringService.getSlowQueries().size());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/record")
    public ResponseEntity<Void> recordQuery(
            @RequestParam String queryText,
            @RequestParam long executionTimeMs,
            @RequestParam String queryType,
            @RequestParam int rowsAffected) {
        queryMonitoringService.recordQuery(queryText, executionTimeMs, queryType, rowsAffected);
        return ResponseEntity.ok().build();
    }
}
