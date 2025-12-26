package com.dbmonitor.controller;

import com.dbmonitor.model.IndexRecommendation;
import com.dbmonitor.service.IndexRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/index-recommendations")
@Slf4j
public class IndexRecommendationApiController {

    @Autowired
    private IndexRecommendationService indexRecommendationService;

    @PostMapping("/generate/{connectionId}")
    public ResponseEntity<List<IndexRecommendation>> generateRecommendations(
            @PathVariable Long connectionId) {
        List<IndexRecommendation> recommendations = 
            indexRecommendationService.generateRecommendations(connectionId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/{connectionId}")
    public ResponseEntity<List<IndexRecommendation>> getRecommendations(
            @PathVariable Long connectionId) {
        return ResponseEntity.ok(indexRecommendationService.getRecommendations(connectionId));
    }

    @GetMapping("/{connectionId}/pending")
    public ResponseEntity<List<IndexRecommendation>> getPendingRecommendations(
            @PathVariable Long connectionId) {
        return ResponseEntity.ok(indexRecommendationService.getPendingRecommendations(connectionId));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<String> applyRecommendation(@PathVariable Long id) {
        try {
            indexRecommendationService.applyRecommendation(id);
            return ResponseEntity.ok("Index recommendation applied successfully");
        } catch (Exception e) {
            log.error("Error applying index recommendation", e);
            return ResponseEntity.badRequest().body("Failed to apply recommendation: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectRecommendation(@PathVariable Long id) {
        indexRecommendationService.rejectRecommendation(id);
        return ResponseEntity.ok("Index recommendation rejected");
    }

    @GetMapping("/applied")
    public ResponseEntity<List<IndexRecommendation>> getAppliedRecommendations() {
        return ResponseEntity.ok(indexRecommendationService.getAppliedRecommendations());
    }

    @GetMapping("/table/{tableName}")
    public ResponseEntity<List<IndexRecommendation>> getRecommendationsForTable(
            @PathVariable String tableName) {
        return ResponseEntity.ok(indexRecommendationService.getRecommendationsForTable(tableName));
    }
}
