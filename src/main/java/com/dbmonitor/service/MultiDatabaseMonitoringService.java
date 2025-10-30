package com.dbmonitor.service;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.model.DatabaseMetrics;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MultiDatabaseMonitoringService {

    @Autowired
    private DatabaseConnectionService connectionService;

    @Autowired
    private AlertService alertService;

    // Store metrics history for each database connection
    private final Map<Long, List<DatabaseMetrics>> metricsHistoryMap = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 100;

    private final Map<Long, Long> startTimeMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${monitor.refresh.interval-ms:5000}")
    public void collectMetricsForAllConnections() {
        log.info("Starting scheduled metrics collection...");
        List<DatabaseConnection> activeConnections = connectionService.getActiveConnections();
        log.info("Found {} active connections to monitor", activeConnections.size());

        if (activeConnections.isEmpty()) {
            log.warn("No active connections found for metrics collection!");
            return;
        }

        for (DatabaseConnection conn : activeConnections) {
            try {
                log.info("Collecting metrics for connection ID: {}, Name: '{}'",
                    conn.getId(), conn.getConnectionName());

                DatabaseMetrics metrics = getCurrentMetrics(conn.getId());

                // Add to history
                metricsHistoryMap.computeIfAbsent(conn.getId(), k -> {
                    log.info("Creating new metrics history list for connection ID: {}", conn.getId());
                    return new ArrayList<>();
                });
                List<DatabaseMetrics> history = metricsHistoryMap.get(conn.getId());

                synchronized (history) {
                    history.add(metrics);
                    if (history.size() > MAX_HISTORY_SIZE) {
                        history.remove(0);
                    }
                    log.info("Metrics added to history for connection ID: {}. History size: {}",
                        conn.getId(), history.size());
                }

                log.info("Collected metrics for '{}': Active Connections={}, CPU={}%, Memory={}%",
                        conn.getConnectionName(),
                        metrics.getActiveConnections(),
                        String.format("%.2f", metrics.getCpuUsage()),
                        String.format("%.2f", metrics.getMemoryUsage()));

            } catch (Exception e) {
                log.error("Error collecting metrics for connection '{}' (ID: {})",
                    conn.getConnectionName(), conn.getId(), e);
                e.printStackTrace();
            }
        }

        log.info("Metrics collection completed. Total connections in map: {}", metricsHistoryMap.size());
    }

    public DatabaseMetrics getCurrentMetrics(Long connectionId) {
        DatabaseConnection connection = connectionService.getConnectionById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        DatabaseMetrics.DatabaseMetricsBuilder builder = DatabaseMetrics.builder()
                .connectionId(connectionId)
                .connectionName(connection.getConnectionName())
                .timestamp(LocalDateTime.now());

        try {
            DataSource dataSource = connectionService.getDataSource(connectionId);
            
            // Get HikariCP pool metrics
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

                int activeConnections = poolMXBean.getActiveConnections();
                int idleConnections = poolMXBean.getIdleConnections();
                int totalConnections = poolMXBean.getTotalConnections();
                int maxConnections = hikariDataSource.getMaximumPoolSize();
                int waitingThreads = poolMXBean.getThreadsAwaitingConnection();

                builder.activeConnections(activeConnections)
                        .idleConnections(idleConnections)
                        .totalConnections(totalConnections)
                        .maxConnections(maxConnections)
                        .waitingThreads(waitingThreads)
                        .connectionUsagePercent((double) totalConnections / maxConnections * 100);
            }

            // Get database info
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                builder.databaseVersion(metaData.getDatabaseProductVersion())
                        .databaseType(metaData.getDatabaseProductName())
                        .databaseStatus("CONNECTED");
            }

            // Get JVM metrics
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory * 100;

            builder.usedMemory(usedMemory)
                    .maxMemory(maxMemory)
                    .memoryUsage(memoryUsage);

            // Get CPU usage
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors() * 100;
            if (cpuUsage < 0) cpuUsage = 0;
            builder.cpuUsage(cpuUsage);

            // Calculate uptime
            long startTime = startTimeMap.computeIfAbsent(connectionId, k -> System.currentTimeMillis());
            long uptime = System.currentTimeMillis() - startTime;
            builder.uptime(uptime);

        } catch (SQLException e) {
            log.error("Error getting database metrics for connection ID {}", connectionId, e);
            builder.databaseStatus("ERROR: " + e.getMessage());
        }

        return builder.build();
    }

    public List<DatabaseMetrics> getMetricsHistory(Long connectionId) {
        log.info("getMetricsHistory called for connection ID: {}", connectionId);
        log.info("Current map contains {} connections: {}", metricsHistoryMap.size(), metricsHistoryMap.keySet());

        List<DatabaseMetrics> history = metricsHistoryMap.get(connectionId);
        if (history == null) {
            log.warn("No history found for connection ID: {}", connectionId);
            return new ArrayList<>();
        }
        synchronized (history) {
            log.info("Returning {} metrics records for connection ID: {}", history.size(), connectionId);
            return new ArrayList<>(history);
        }
    }

    public List<DatabaseMetrics> getMetricsHistory(Long connectionId, int limit) {
        log.info("getMetricsHistory(limit={}) called for connection ID: {}", limit, connectionId);
        log.info("Current map contains {} connections: {}", metricsHistoryMap.size(), metricsHistoryMap.keySet());

        List<DatabaseMetrics> history = metricsHistoryMap.get(connectionId);
        if (history == null) {
            log.warn("No history found for connection ID: {}", connectionId);
            return new ArrayList<>();
        }
        synchronized (history) {
            int size = history.size();
            int start = Math.max(0, size - limit);
            log.info("Returning {} metrics records (from {} total) for connection ID: {}",
                size - start, size, connectionId);
            return new ArrayList<>(history.subList(start, size));
        }
    }

    public Map<Long, DatabaseMetrics> getCurrentMetricsForAllConnections() {
        Map<Long, DatabaseMetrics> metricsMap = new HashMap<>();
        List<DatabaseConnection> activeConnections = connectionService.getActiveConnections();
        
        for (DatabaseConnection conn : activeConnections) {
            try {
                DatabaseMetrics metrics = getCurrentMetrics(conn.getId());
                metricsMap.put(conn.getId(), metrics);
            } catch (Exception e) {
                log.error("Error getting metrics for connection '{}'", conn.getConnectionName(), e);
            }
        }
        
        return metricsMap;
    }

    public void clearHistory(Long connectionId) {
        metricsHistoryMap.remove(connectionId);
        startTimeMap.remove(connectionId);
        log.info("Cleared metrics history for connection ID: {}", connectionId);
    }
}
