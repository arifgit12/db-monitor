package com.dbmonitor.controller;

import com.dbmonitor.model.QueryPlan;
import com.dbmonitor.service.QueryPlanAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query-plans")
@Slf4j
public class QueryPlanApiController {

    @Autowired
    private QueryPlanAnalysisService queryPlanAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<QueryPlan> analyzeQueryPlan(
            @RequestParam Long connectionId,
            @RequestBody Map<String, String> request) {
        String queryText = request.get("queryText");
        if (queryText == null || queryText.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        QueryPlan queryPlan = queryPlanAnalysisService.analyzeQueryPlan(connectionId, queryText);
        return ResponseEntity.ok(queryPlan);
    }

    @GetMapping("/query/{queryId}")
    public ResponseEntity<List<QueryPlan>> getQueryPlans(@PathVariable Long queryId) {
        return ResponseEntity.ok(queryPlanAnalysisService.getQueryPlans(queryId));
    }

    @GetMapping("/full-table-scans")
    public ResponseEntity<List<QueryPlan>> getQueriesWithFullTableScans() {
        return ResponseEntity.ok(queryPlanAnalysisService.getQueriesWithFullTableScans());
    }

    @GetMapping("/without-indexes")
    public ResponseEntity<List<QueryPlan>> getQueriesWithoutIndexes() {
        return ResponseEntity.ok(queryPlanAnalysisService.getQueriesWithoutIndexes());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<QueryPlan>> getRecentQueryPlans(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(queryPlanAnalysisService.getRecentQueryPlans(hours));
    }
}
