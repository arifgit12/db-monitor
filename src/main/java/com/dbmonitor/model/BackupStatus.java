package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long connectionId;
    
    private String connectionName;
    
    private LocalDateTime lastBackupTime;
    
    private String backupType; // FULL, INCREMENTAL, DIFFERENTIAL
    
    private String backupStatus; // SUCCESS, FAILED, IN_PROGRESS
    
    private Long backupSizeBytes;
    
    private Integer backupDurationSeconds;
    
    private String backupLocation;
    
    private String errorMessage;
    
    private LocalDateTime checkedAt;
}
