package com.dbmonitor.controller;

import com.dbmonitor.model.ReplicationStatus;
import com.dbmonitor.service.ReplicationMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replication")
@Slf4j
public class ReplicationApiController {

    @Autowired
    private ReplicationMonitoringService replicationMonitoringService;

    @GetMapping("/check/{connectionId}")
    public ResponseEntity<ReplicationStatus> checkReplicationStatus(@PathVariable Long connectionId) {
        ReplicationStatus status = replicationMonitoringService.checkReplicationStatus(connectionId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/latest/{connectionId}")
    public ResponseEntity<ReplicationStatus> getLatestReplicationStatus(@PathVariable Long connectionId) {
        return replicationMonitoringService.getLatestReplicationStatus(connectionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history/{connectionId}")
    public ResponseEntity<List<ReplicationStatus>> getReplicationHistory(@PathVariable Long connectionId) {
        return ResponseEntity.ok(replicationMonitoringService.getReplicationHistory(connectionId));
    }

    @GetMapping("/errors")
    public ResponseEntity<List<ReplicationStatus>> getReplicationErrors() {
        return ResponseEntity.ok(replicationMonitoringService.getReplicationErrors());
    }

    @GetMapping("/high-lag")
    public ResponseEntity<List<ReplicationStatus>> getHighLagReplicas(
            @RequestParam(defaultValue = "60") long lagThresholdSeconds) {
        return ResponseEntity.ok(replicationMonitoringService.getHighLagReplicas(lagThresholdSeconds));
    }
}
