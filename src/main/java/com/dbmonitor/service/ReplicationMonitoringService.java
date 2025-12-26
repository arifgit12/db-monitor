package com.dbmonitor.service;

import com.dbmonitor.model.ReplicationStatus;
import com.dbmonitor.repository.ReplicationStatusRepository;
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
public class ReplicationMonitoringService {

    @Autowired
    private ReplicationStatusRepository replicationStatusRepository;

    @Autowired
    private DatabaseConnectionService connectionService;

    public ReplicationStatus checkReplicationStatus(Long connectionId) {
        try {
            DataSource dataSource = connectionService.getDataSource(connectionId);
            String connectionName = connectionService.getConnectionById(connectionId)
                .map(conn -> conn.getConnectionName())
                .orElse("Unknown");

            ReplicationStatus status = ReplicationStatus.builder()
                .connectionId(connectionId)
                .connectionName(connectionName)
                .lastChecked(LocalDateTime.now())
                .build();

            try (Connection conn = dataSource.getConnection()) {
                String databaseType = conn.getMetaData().getDatabaseProductName().toLowerCase();
                
                if (databaseType.contains("mysql") || databaseType.contains("mariadb")) {
                    checkMySQLReplication(conn, status);
                } else if (databaseType.contains("postgresql")) {
                    checkPostgreSQLReplication(conn, status);
                } else if (databaseType.contains("sql server")) {
                    checkSQLServerReplication(conn, status);
                } else {
                    status.setReplicationState("NOT_SUPPORTED");
                    log.warn("Replication monitoring not supported for database type: {}", databaseType);
                }
            }

            replicationStatusRepository.save(status);
            return status;
        } catch (Exception e) {
            log.error("Error checking replication status for connection ID: {}", connectionId, e);
            throw new RuntimeException("Failed to check replication status", e);
        }
    }

    private void checkMySQLReplication(Connection conn, ReplicationStatus status) {
        try (Statement stmt = conn.createStatement()) {
            // Check if this is a master or slave
            try {
                ResultSet rs = stmt.executeQuery("SHOW SLAVE STATUS");
                if (rs.next()) {
                    // This is a slave
                    status.setReplicationType("SLAVE");
                    status.setSlaveIORunning(rs.getString("Slave_IO_Running"));
                    status.setSlaveSQLRunning(rs.getString("Slave_SQL_Running"));
                    status.setMasterHost(rs.getString("Master_Host"));
                    status.setMasterPort(rs.getInt("Master_Port"));
                    
                    long secondsBehindMaster = rs.getLong("Seconds_Behind_Master");
                    status.setLagSeconds(secondsBehindMaster);
                    
                    String lastError = rs.getString("Last_Error");
                    if (lastError != null && !lastError.isEmpty()) {
                        status.setLastError(lastError);
                        status.setReplicationState("ERROR");
                    } else if ("Yes".equals(status.getSlaveIORunning()) && 
                              "Yes".equals(status.getSlaveSQLRunning())) {
                        status.setReplicationState("RUNNING");
                    } else {
                        status.setReplicationState("STOPPED");
                    }
                } else {
                    // Check if this is a master
                    rs = stmt.executeQuery("SHOW MASTER STATUS");
                    if (rs.next()) {
                        status.setReplicationType("MASTER");
                        status.setReplicationState("RUNNING");
                        status.setLagSeconds(0L);
                    } else {
                        status.setReplicationType("STANDALONE");
                        status.setReplicationState("NOT_CONFIGURED");
                    }
                }
            } catch (Exception e) {
                status.setReplicationType("UNKNOWN");
                status.setReplicationState("ERROR");
                status.setLastError(e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error checking MySQL replication", e);
            status.setReplicationState("ERROR");
            status.setLastError(e.getMessage());
        }
    }

    private void checkPostgreSQLReplication(Connection conn, ReplicationStatus status) {
        try (Statement stmt = conn.createStatement()) {
            // Check if in recovery (replica)
            ResultSet rs = stmt.executeQuery("SELECT pg_is_in_recovery()");
            if (rs.next()) {
                boolean isRecovery = rs.getBoolean(1);
                
                if (isRecovery) {
                    status.setReplicationType("REPLICA");
                    status.setReplicationState("RUNNING");
                    
                    // Get replication lag
                    rs = stmt.executeQuery(
                        "SELECT EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp()))::INTEGER"
                    );
                    if (rs.next()) {
                        long lag = rs.getLong(1);
                        status.setLagSeconds(lag);
                    }
                } else {
                    // This is a primary
                    status.setReplicationType("MASTER");
                    status.setReplicationState("RUNNING");
                    status.setLagSeconds(0L);
                    
                    // Count active replicas
                    rs = stmt.executeQuery(
                        "SELECT count(*) FROM pg_stat_replication"
                    );
                    if (rs.next()) {
                        int replicaCount = rs.getInt(1);
                        log.info("PostgreSQL master has {} active replicas", replicaCount);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking PostgreSQL replication", e);
            status.setReplicationState("ERROR");
            status.setLastError(e.getMessage());
        }
    }

    private void checkSQLServerReplication(Connection conn, ReplicationStatus status) {
        try (Statement stmt = conn.createStatement()) {
            // Check Always On Availability Groups
            String query = "SELECT role_desc, synchronization_health_desc " +
                          "FROM sys.dm_hadr_availability_replica_states ars " +
                          "JOIN sys.availability_replicas ar ON ars.replica_id = ar.replica_id " +
                          "WHERE is_local = 1";
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                String role = rs.getString("role_desc");
                String health = rs.getString("synchronization_health_desc");
                
                status.setReplicationType(role);
                status.setReplicationState(health);
                status.setLagSeconds(0L); // Would need more complex query for actual lag
            } else {
                status.setReplicationType("STANDALONE");
                status.setReplicationState("NOT_CONFIGURED");
            }
        } catch (Exception e) {
            log.error("Error checking SQL Server replication", e);
            status.setReplicationState("ERROR");
            status.setLastError(e.getMessage());
        }
    }

    public Optional<ReplicationStatus> getLatestReplicationStatus(Long connectionId) {
        return replicationStatusRepository.findTopByConnectionIdOrderByLastCheckedDesc(connectionId);
    }

    public List<ReplicationStatus> getReplicationHistory(Long connectionId) {
        return replicationStatusRepository.findByConnectionId(connectionId);
    }

    public List<ReplicationStatus> getReplicationErrors() {
        return replicationStatusRepository.findByReplicationState("ERROR");
    }

    public List<ReplicationStatus> getHighLagReplicas(long lagThresholdSeconds) {
        return replicationStatusRepository.findByLagSecondsGreaterThan(lagThresholdSeconds);
    }
}
