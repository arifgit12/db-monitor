package com.dbmonitor.service;

import com.dbmonitor.model.Alert;
import com.dbmonitor.model.DatabaseMetrics;
import com.dbmonitor.repository.AlertRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DatabaseMonitoringService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AlertRepository alertRepository;

    @Value("${monitor.alert.cpu-threshold:80}")
    private double cpuThreshold;

    @Value("${monitor.alert.memory-threshold:85}")
    private double memoryThreshold;

    private final List<DatabaseMetrics> metricsHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100;

    private long startTime = System.currentTimeMillis();

    @Scheduled(fixedDelayString = "${monitor.refresh.interval-ms:5000}")
    public void collectMetrics() {
        try {
            DatabaseMetrics metrics = getCurrentMetrics();
            
            // Add to history
            synchronized (metricsHistory) {
                metricsHistory.add(metrics);
                if (metricsHistory.size() > MAX_HISTORY_SIZE) {
                    metricsHistory.remove(0);
                }
            }

            // Check for alerts
            checkAndCreateAlerts(metrics);
            
            log.debug("Collected metrics: Active Connections={}, CPU={}%, Memory={}%",
                    metrics.getActiveConnections(), 
                    String.format("%.2f", metrics.getCpuUsage()),
                    String.format("%.2f", metrics.getMemoryUsage()));
            
        } catch (Exception e) {
            log.error("Error collecting metrics", e);
        }
    }

    public DatabaseMetrics getCurrentMetrics() {
        DatabaseMetrics.DatabaseMetricsBuilder builder = DatabaseMetrics.builder()
                .timestamp(LocalDateTime.now());

        try {
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
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
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
            long uptime = System.currentTimeMillis() - startTime;
            builder.uptime(uptime);

        } catch (SQLException e) {
            log.error("Error getting database metrics", e);
            builder.databaseStatus("ERROR: " + e.getMessage());
        }

        return builder.build();
    }

    private void checkAndCreateAlerts(DatabaseMetrics metrics) {
        // Check CPU usage
        if (metrics.getCpuUsage() > cpuThreshold) {
            createAlert("CPU_HIGH", "WARNING", 
                    String.format("CPU usage is high: %.2f%%", metrics.getCpuUsage()),
                    metrics.getCpuUsage(), cpuThreshold);
        }

        // Check memory usage
        if (metrics.getMemoryUsage() > memoryThreshold) {
            createAlert("MEMORY_HIGH", "WARNING",
                    String.format("Memory usage is high: %.2f%%", metrics.getMemoryUsage()),
                    metrics.getMemoryUsage(), memoryThreshold);
        }

        // Check connection pool
        if (metrics.getConnectionUsagePercent() > 90) {
            createAlert("CONNECTION_POOL_HIGH", "CRITICAL",
                    String.format("Connection pool usage is critical: %.2f%%", metrics.getConnectionUsagePercent()),
                    metrics.getConnectionUsagePercent(), 90.0);
        }

        // Check waiting threads
        if (metrics.getWaitingThreads() > 10) {
            createAlert("THREADS_WAITING", "WARNING",
                    String.format("High number of threads waiting for connections: %d", metrics.getWaitingThreads()),
                    (double) metrics.getWaitingThreads(), 10.0);
        }
    }

    private void createAlert(String type, String severity, String message, double value, double threshold) {
        // Check if similar alert already exists in last 5 minutes
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Alert> recentAlerts = alertRepository.findByCreatedAtAfterOrderByCreatedAtDesc(fiveMinutesAgo);
        
        boolean alertExists = recentAlerts.stream()
                .anyMatch(a -> a.getAlertType().equals(type) && !a.getAcknowledged());
        
        if (!alertExists) {
            Alert alert = Alert.builder()
                    .alertType(type)
                    .severity(severity)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .acknowledged(false)
                    .metricValue(value)
                    .threshold(threshold)
                    .build();
            alertRepository.save(alert);
            log.warn("Alert created: {} - {}", type, message);
        }
    }

    public List<DatabaseMetrics> getMetricsHistory() {
        synchronized (metricsHistory) {
            return new ArrayList<>(metricsHistory);
        }
    }

    public List<DatabaseMetrics> getMetricsHistory(int limit) {
        synchronized (metricsHistory) {
            int size = metricsHistory.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(metricsHistory.subList(start, size));
        }
    }
}
