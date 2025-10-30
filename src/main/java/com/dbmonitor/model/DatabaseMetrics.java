package com.dbmonitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseMetrics {
    private Long connectionId;
    private String connectionName;
    private LocalDateTime timestamp;
    private int activeConnections;
    private int idleConnections;
    private int totalConnections;
    private int maxConnections;
    private long waitingThreads;
    private double connectionUsagePercent;
    private long totalQueries;
    private long slowQueries;
    private double averageQueryTime;
    private double cpuUsage;
    private double memoryUsage;
    private long usedMemory;
    private long maxMemory;
    private String databaseStatus;
    private long uptime;
    private String databaseVersion;
    private String databaseType;
}
