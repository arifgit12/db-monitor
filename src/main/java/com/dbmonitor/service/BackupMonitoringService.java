package com.dbmonitor.service;

import com.dbmonitor.model.BackupStatus;
import com.dbmonitor.repository.BackupStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BackupMonitoringService {

    @Autowired
    private BackupStatusRepository backupStatusRepository;

    @Autowired
    private DatabaseConnectionService connectionService;

    public BackupStatus checkBackupStatus(Long connectionId) {
        try {
            DataSource dataSource = connectionService.getDataSource(connectionId);
            String connectionName = connectionService.getConnectionById(connectionId)
                .map(conn -> conn.getConnectionName())
                .orElse("Unknown");

            BackupStatus status = BackupStatus.builder()
                .connectionId(connectionId)
                .connectionName(connectionName)
                .checkedAt(LocalDateTime.now())
                .build();

            try (Connection conn = dataSource.getConnection()) {
                String databaseType = conn.getMetaData().getDatabaseProductName().toLowerCase();
                
                if (databaseType.contains("mysql") || databaseType.contains("mariadb")) {
                    checkMySQLBackup(conn, status);
                } else if (databaseType.contains("postgresql")) {
                    checkPostgreSQLBackup(conn, status);
                } else if (databaseType.contains("sql server")) {
                    checkSQLServerBackup(conn, status);
                } else {
                    status.setBackupStatus("NOT_SUPPORTED");
                    log.warn("Backup monitoring not supported for database type: {}", databaseType);
                }
            }

            backupStatusRepository.save(status);
            return status;
        } catch (Exception e) {
            log.error("Error checking backup status for connection ID: {}", connectionId, e);
            throw new RuntimeException("Failed to check backup status", e);
        }
    }

    private void checkMySQLBackup(Connection conn, BackupStatus status) {
        // MySQL doesn't have built-in backup tracking, this is a placeholder
        // In real scenarios, you'd query your backup tool's metadata or files
        status.setBackupStatus("UNKNOWN");
        status.setBackupType("UNKNOWN");
        log.info("MySQL backup check - would require integration with backup tool");
    }

    private void checkPostgreSQLBackup(Connection conn, BackupStatus status) {
        try (Statement stmt = conn.createStatement()) {
            // Check for pg_basebackup or WAL archiving status
            ResultSet rs = stmt.executeQuery("SELECT pg_is_in_recovery(), pg_last_wal_replay_lsn()");
            if (rs.next()) {
                boolean isRecovery = rs.getBoolean(1);
                status.setBackupType(isRecovery ? "REPLICA" : "PRIMARY");
                status.setBackupStatus("ACTIVE");
            }
        } catch (Exception e) {
            log.error("Error checking PostgreSQL backup", e);
            status.setBackupStatus("ERROR");
            status.setErrorMessage(e.getMessage());
        }
    }

    private void checkSQLServerBackup(Connection conn, BackupStatus status) {
        try (Statement stmt = conn.createStatement()) {
            // Query msdb backup history
            String query = "SELECT TOP 1 backup_start_date, backup_finish_date, " +
                          "type, backup_size, database_name " +
                          "FROM msdb.dbo.backupset " +
                          "ORDER BY backup_start_date DESC";
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                status.setLastBackupTime(rs.getTimestamp("backup_finish_date").toLocalDateTime());
                status.setBackupType(getBackupType(rs.getString("type")));
                status.setBackupSizeBytes(rs.getLong("backup_size"));
                status.setBackupStatus("SUCCESS");
            } else {
                status.setBackupStatus("NO_BACKUPS");
            }
        } catch (Exception e) {
            log.error("Error checking SQL Server backup", e);
            status.setBackupStatus("ERROR");
            status.setErrorMessage(e.getMessage());
        }
    }

    private String getBackupType(String type) {
        switch (type) {
            case "D": return "FULL";
            case "I": return "DIFFERENTIAL";
            case "L": return "LOG";
            default: return "UNKNOWN";
        }
    }

    public Optional<BackupStatus> getLatestBackupStatus(Long connectionId) {
        return backupStatusRepository.findTopByConnectionIdOrderByLastBackupTimeDesc(connectionId);
    }

    public List<BackupStatus> getBackupHistory(Long connectionId) {
        return backupStatusRepository.findByConnectionIdOrderByLastBackupTimeDesc(connectionId);
    }

    public List<BackupStatus> getFailedBackups() {
        return backupStatusRepository.findByBackupStatus("FAILED");
    }

    public List<BackupStatus> getOldBackups(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        return backupStatusRepository.findByLastBackupTimeBefore(cutoff);
    }
}
