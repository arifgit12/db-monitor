package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "replication_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplicationStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long connectionId;
    
    private String connectionName;
    
    private String replicationType; // MASTER, SLAVE, REPLICA
    
    private String replicationState; // RUNNING, STOPPED, ERROR
    
    private Long lagSeconds;
    
    private Long lagBytes;
    
    private String masterHost;
    
    private Integer masterPort;
    
    private String slaveIORunning;
    
    private String slaveSQLRunning;
    
    private String lastError;
    
    private LocalDateTime lastChecked;
}
