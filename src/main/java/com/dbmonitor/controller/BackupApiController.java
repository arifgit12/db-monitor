package com.dbmonitor.controller;

import com.dbmonitor.model.BackupStatus;
import com.dbmonitor.service.BackupMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backup")
@Slf4j
public class BackupApiController {

    @Autowired
    private BackupMonitoringService backupMonitoringService;

    @GetMapping("/check/{connectionId}")
    public ResponseEntity<BackupStatus> checkBackupStatus(@PathVariable Long connectionId) {
        BackupStatus status = backupMonitoringService.checkBackupStatus(connectionId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/latest/{connectionId}")
    public ResponseEntity<BackupStatus> getLatestBackupStatus(@PathVariable Long connectionId) {
        return backupMonitoringService.getLatestBackupStatus(connectionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history/{connectionId}")
    public ResponseEntity<List<BackupStatus>> getBackupHistory(@PathVariable Long connectionId) {
        return ResponseEntity.ok(backupMonitoringService.getBackupHistory(connectionId));
    }

    @GetMapping("/failed")
    public ResponseEntity<List<BackupStatus>> getFailedBackups() {
        return ResponseEntity.ok(backupMonitoringService.getFailedBackups());
    }

    @GetMapping("/old")
    public ResponseEntity<List<BackupStatus>> getOldBackups(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(backupMonitoringService.getOldBackups(days));
    }
}
