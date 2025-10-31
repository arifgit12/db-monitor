package com.dbmonitor.service;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.model.QueryMetrics;
import com.dbmonitor.repository.QueryMetricsRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class QueryMonitoringService {

    @Autowired
    private QueryMetricsRepository queryMetricsRepository;

    @Autowired
    private DatabaseConnectionService connectionService;

    @Value("${monitor.query.slow-threshold-ms:1000}")
    private long slowQueryThreshold;

    public void recordQuery(String queryText, long executionTimeMs, String queryType, int rowsAffected) {
        try {
            QueryMetrics metrics = QueryMetrics.builder()
                    .queryText(queryText)
                    .executionTime(LocalDateTime.now())
                    .executionDurationMs(executionTimeMs)
                    .queryType(queryType)
                    .isSlow(executionTimeMs > slowQueryThreshold)
                    .rowsAffected(rowsAffected)
                    .status("SUCCESS")
                    .build();

            queryMetricsRepository.save(metrics);

            if (metrics.getIsSlow()) {
                log.warn("Slow query detected: {}ms - {}", executionTimeMs, 
                        queryText.length() > 100 ? queryText.substring(0, 100) + "..." : queryText);
            }
        } catch (Exception e) {
            log.error("Error recording query metrics", e);
        }
    }

    public void recordFailedQuery(String queryText, String queryType, String errorMessage) {
        try {
            QueryMetrics metrics = QueryMetrics.builder()
                    .queryText(queryText)
                    .executionTime(LocalDateTime.now())
                    .executionDurationMs(0L)
                    .queryType(queryType)
                    .isSlow(false)
                    .rowsAffected(0)
                    .status("FAILED")
                    .errorMessage(errorMessage)
                    .build();

            queryMetricsRepository.save(metrics);
            log.error("Failed query recorded: {}", errorMessage);
        } catch (Exception e) {
            log.error("Error recording failed query", e);
        }
    }

    public List<QueryMetrics> getSlowQueries() {
        return queryMetricsRepository.findByIsSlowTrue();
    }

    public List<QueryMetrics> getRecentSlowQueries(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return queryMetricsRepository.findTopSlowQueriesSince(since);
    }

    public List<QueryMetrics> getQueriesBetween(LocalDateTime start, LocalDateTime end) {
        return queryMetricsRepository.findByExecutionTimeBetween(start, end);
    }

    public Long getTotalQueriesInLastHour() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return queryMetricsRepository.countQueriesSince(oneHourAgo);
    }

    public Double getAverageQueryTime(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        Double avg = queryMetricsRepository.averageExecutionTimeSince(since);
        return avg != null ? avg : 0.0;
    }

    public List<QueryMetrics> getAllQueries() {
        return queryMetricsRepository.findAll();
    }

    /**
     * Get queries from the monitored database (not the monitoring app's own queries)
     */
    public List<QueryMetrics> getQueriesFromDatabase(Long connectionId) {
        try {
            DatabaseConnection conn = connectionService.getConnectionById(connectionId)
                    .orElseThrow(() -> new RuntimeException("Connection not found: " + connectionId));

            log.info("Fetching queries from database type: {}", conn.getDatabaseType());

            switch (conn.getDatabaseType().toUpperCase()) {
                case "SQL SERVER":
                    return getQueriesFromSqlServer(conn);
                case "MYSQL":
                    return getQueriesFromMySQL(conn);
                case "POSTGRESQL":
                    return getQueriesFromPostgreSQL(conn);
                case "H2":
                    return getQueriesFromH2(conn);
                default:
                    log.warn("Query monitoring not supported for database type: {}", conn.getDatabaseType());
                    return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Error fetching queries from database", e);
            return new ArrayList<>();
        }
    }

    public List<QueryMetrics> getSlowQueriesFromDatabase(Long connectionId) {
        List<QueryMetrics> allQueries = getQueriesFromDatabase(connectionId);
        return allQueries.stream()
                .filter(QueryMetrics::getIsSlow)
                .toList();
    }

    private List<QueryMetrics> getQueriesFromSqlServer(DatabaseConnection conn) {
        List<QueryMetrics> queries = new ArrayList<>();
        String sql = """
            SELECT TOP 50
                SUBSTRING(st.text, (qs.statement_start_offset/2) + 1,
                    ((CASE qs.statement_end_offset
                        WHEN -1 THEN DATALENGTH(st.text)
                        ELSE qs.statement_end_offset
                    END - qs.statement_start_offset)/2) + 1) AS query_text,
                qs.execution_count,
                qs.total_elapsed_time / 1000 AS total_duration_ms,
                (qs.total_elapsed_time / qs.execution_count) / 1000 AS avg_duration_ms,
                qs.last_execution_time,
                qs.total_rows
            FROM sys.dm_exec_query_stats AS qs
            CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) AS st
            WHERE st.text IS NOT NULL
            ORDER BY qs.last_execution_time DESC
            """;

        try (HikariDataSource dataSource = createDataSource(conn);
             Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            log.info("Executing query against SQL Server: {}", conn.getConnectionName());

            while (rs.next()) {
                String queryText = rs.getString("query_text");
                long avgDurationMs = rs.getLong("avg_duration_ms");

                QueryMetrics metrics = QueryMetrics.builder()
                        .queryText(queryText != null ? queryText.trim() : "")
                        .executionTime(rs.getTimestamp("last_execution_time").toLocalDateTime())
                        .executionDurationMs(avgDurationMs)
                        .queryType(determineQueryType(queryText))
                        .isSlow(avgDurationMs > slowQueryThreshold)
                        .rowsAffected(rs.getInt("total_rows"))
                        .status("SUCCESS")
                        .build();

                queries.add(metrics);
            }

            log.info("Retrieved {} queries from SQL Server", queries.size());
        } catch (Exception e) {
            log.error("Error querying SQL Server system views", e);
        }

        return queries;
    }

    private List<QueryMetrics> getQueriesFromMySQL(DatabaseConnection conn) {
        List<QueryMetrics> queries = new ArrayList<>();
        String sql = """
            SELECT
                DIGEST_TEXT as query_text,
                COUNT_STAR as execution_count,
                SUM_TIMER_WAIT / 1000000000 as total_duration_ms,
                AVG_TIMER_WAIT / 1000000000 as avg_duration_ms,
                LAST_SEEN as last_execution_time,
                SUM_ROWS_AFFECTED as rows_affected
            FROM performance_schema.events_statements_summary_by_digest
            WHERE DIGEST_TEXT IS NOT NULL
            ORDER BY LAST_SEEN DESC
            LIMIT 50
            """;

        try (HikariDataSource dataSource = createDataSource(conn);
             Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String queryText = rs.getString("query_text");
                long avgDurationMs = rs.getLong("avg_duration_ms");

                QueryMetrics metrics = QueryMetrics.builder()
                        .queryText(queryText != null ? queryText.trim() : "")
                        .executionTime(rs.getTimestamp("last_execution_time").toLocalDateTime())
                        .executionDurationMs(avgDurationMs)
                        .queryType(determineQueryType(queryText))
                        .isSlow(avgDurationMs > slowQueryThreshold)
                        .rowsAffected(rs.getInt("rows_affected"))
                        .status("SUCCESS")
                        .build();

                queries.add(metrics);
            }

            log.info("Retrieved {} queries from MySQL", queries.size());
        } catch (Exception e) {
            log.error("Error querying MySQL performance schema", e);
        }

        return queries;
    }

    private List<QueryMetrics> getQueriesFromPostgreSQL(DatabaseConnection conn) {
        List<QueryMetrics> queries = new ArrayList<>();
        String sql = """
            SELECT
                query as query_text,
                calls as execution_count,
                total_time as total_duration_ms,
                mean_time as avg_duration_ms,
                rows as rows_affected
            FROM pg_stat_statements
            WHERE query IS NOT NULL
            ORDER BY calls DESC
            LIMIT 50
            """;

        try (HikariDataSource dataSource = createDataSource(conn);
             Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String queryText = rs.getString("query_text");
                long avgDurationMs = rs.getLong("avg_duration_ms");

                QueryMetrics metrics = QueryMetrics.builder()
                        .queryText(queryText != null ? queryText.trim() : "")
                        .executionTime(LocalDateTime.now())
                        .executionDurationMs(avgDurationMs)
                        .queryType(determineQueryType(queryText))
                        .isSlow(avgDurationMs > slowQueryThreshold)
                        .rowsAffected(rs.getInt("rows_affected"))
                        .status("SUCCESS")
                        .build();

                queries.add(metrics);
            }

            log.info("Retrieved {} queries from PostgreSQL", queries.size());
        } catch (Exception e) {
            log.error("Error querying PostgreSQL pg_stat_statements", e);
        }

        return queries;
    }

    private List<QueryMetrics> getQueriesFromH2(DatabaseConnection conn) {
        // H2 doesn't have built-in query statistics, return empty list
        log.info("H2 database doesn't support query monitoring");
        return new ArrayList<>();
    }

    private HikariDataSource createDataSource(DatabaseConnection conn) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(conn.getJdbcUrl());
        config.setUsername(conn.getUsername());
        config.setPassword(conn.getPassword());
        config.setDriverClassName(conn.getDriverClassName());
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(conn.getConnectionTimeout());

        return new HikariDataSource(config);
    }

    private String determineQueryType(String queryText) {
        if (queryText == null || queryText.isEmpty()) {
            return "UNKNOWN";
        }

        String upper = queryText.trim().toUpperCase();
        if (upper.startsWith("SELECT")) return "SELECT";
        if (upper.startsWith("INSERT")) return "INSERT";
        if (upper.startsWith("UPDATE")) return "UPDATE";
        if (upper.startsWith("DELETE")) return "DELETE";
        if (upper.startsWith("CREATE")) return "DDL";
        if (upper.startsWith("ALTER")) return "DDL";
        if (upper.startsWith("DROP")) return "DDL";

        return "OTHER";
    }
}
