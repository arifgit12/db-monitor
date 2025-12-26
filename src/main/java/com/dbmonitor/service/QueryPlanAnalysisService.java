package com.dbmonitor.service;

import com.dbmonitor.model.QueryMetrics;
import com.dbmonitor.model.QueryPlan;
import com.dbmonitor.repository.QueryPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class QueryPlanAnalysisService {

    @Autowired
    private QueryPlanRepository queryPlanRepository;

    @Autowired
    private DatabaseConnectionService connectionService;

    public QueryPlan analyzeQueryPlan(Long connectionId, String queryText) {
        try {
            DataSource dataSource = connectionService.getDataSource(connectionId);
            
            QueryPlan queryPlan = QueryPlan.builder()
                .queryText(queryText)
                .analyzedAt(LocalDateTime.now())
                .build();

            try (Connection conn = dataSource.getConnection()) {
                String databaseType = conn.getMetaData().getDatabaseProductName().toLowerCase();
                
                if (databaseType.contains("mysql") || databaseType.contains("mariadb")) {
                    analyzeMySQLQuery(conn, queryText, queryPlan);
                } else if (databaseType.contains("postgresql")) {
                    analyzePostgreSQLQuery(conn, queryText, queryPlan);
                } else if (databaseType.contains("sql server")) {
                    analyzeSQLServerQuery(conn, queryText, queryPlan);
                } else {
                    log.warn("Query plan analysis not supported for database type: {}", databaseType);
                    queryPlan.setPlanType("NOT_SUPPORTED");
                }
            }

            queryPlanRepository.save(queryPlan);
            return queryPlan;
        } catch (Exception e) {
            log.error("Error analyzing query plan", e);
            throw new RuntimeException("Failed to analyze query plan", e);
        }
    }

    private void analyzeMySQLQuery(Connection conn, String queryText, QueryPlan queryPlan) {
        try (Statement stmt = conn.createStatement()) {
            queryPlan.setPlanType("EXPLAIN");
            
            // Run EXPLAIN
            ResultSet rs = stmt.executeQuery("EXPLAIN " + queryText);
            StringBuilder plan = new StringBuilder();
            
            boolean usesIndex = false;
            boolean hasFullTableScan = false;
            long estimatedRows = 0;
            
            while (rs.next()) {
                String type = rs.getString("type");
                String key = rs.getString("key");
                long rows = rs.getLong("rows");
                
                plan.append("Type: ").append(type)
                    .append(", Key: ").append(key)
                    .append(", Rows: ").append(rows)
                    .append(", Extra: ").append(rs.getString("Extra"))
                    .append("\n");
                
                if (key != null && !key.equals("NULL")) {
                    usesIndex = true;
                }
                if ("ALL".equals(type)) {
                    hasFullTableScan = true;
                }
                estimatedRows += rows;
            }
            
            queryPlan.setExecutionPlan(plan.toString());
            queryPlan.setUsesIndex(usesIndex);
            queryPlan.setHasFullTableScan(hasFullTableScan);
            queryPlan.setEstimatedRows(estimatedRows);
            
        } catch (Exception e) {
            log.error("Error analyzing MySQL query", e);
            queryPlan.setExecutionPlan("ERROR: " + e.getMessage());
        }
    }

    private void analyzePostgreSQLQuery(Connection conn, String queryText, QueryPlan queryPlan) {
        try (Statement stmt = conn.createStatement()) {
            queryPlan.setPlanType("EXPLAIN ANALYZE");
            
            // Run EXPLAIN ANALYZE
            ResultSet rs = stmt.executeQuery("EXPLAIN ANALYZE " + queryText);
            StringBuilder plan = new StringBuilder();
            
            boolean usesIndex = false;
            boolean hasFullTableScan = false;
            
            while (rs.next()) {
                String planLine = rs.getString(1);
                plan.append(planLine).append("\n");
                
                if (planLine.contains("Index Scan") || planLine.contains("Index Only Scan")) {
                    usesIndex = true;
                }
                if (planLine.contains("Seq Scan")) {
                    hasFullTableScan = true;
                }
                
                // Extract cost and rows from plan line
                if (planLine.contains("cost=")) {
                    try {
                        int costStart = planLine.indexOf("cost=") + 5;
                        int costEnd = planLine.indexOf("..", costStart);
                        if (costEnd > costStart) {
                            String costStr = planLine.substring(costStart, costEnd);
                            queryPlan.setEstimatedCost(Double.parseDouble(costStr));
                        }
                    } catch (Exception e) {
                        log.debug("Could not parse cost from plan line", e);
                    }
                }
                
                if (planLine.contains("rows=")) {
                    try {
                        int rowsStart = planLine.indexOf("rows=") + 5;
                        int rowsEnd = planLine.indexOf(" ", rowsStart);
                        if (rowsEnd == -1) rowsEnd = planLine.length();
                        String rowsStr = planLine.substring(rowsStart, rowsEnd);
                        queryPlan.setEstimatedRows(Long.parseLong(rowsStr));
                    } catch (Exception e) {
                        log.debug("Could not parse rows from plan line", e);
                    }
                }
            }
            
            queryPlan.setExecutionPlan(plan.toString());
            queryPlan.setUsesIndex(usesIndex);
            queryPlan.setHasFullTableScan(hasFullTableScan);
            
        } catch (Exception e) {
            log.error("Error analyzing PostgreSQL query", e);
            queryPlan.setExecutionPlan("ERROR: " + e.getMessage());
        }
    }

    private void analyzeSQLServerQuery(Connection conn, String queryText, QueryPlan queryPlan) {
        try (Statement stmt = conn.createStatement()) {
            queryPlan.setPlanType("EXECUTION PLAN");
            
            // Enable showplan
            stmt.execute("SET SHOWPLAN_TEXT ON");
            
            ResultSet rs = stmt.executeQuery(queryText);
            StringBuilder plan = new StringBuilder();
            
            while (rs.next()) {
                plan.append(rs.getString(1)).append("\n");
            }
            
            stmt.execute("SET SHOWPLAN_TEXT OFF");
            
            String planText = plan.toString();
            queryPlan.setExecutionPlan(planText);
            queryPlan.setUsesIndex(planText.contains("Index Seek") || planText.contains("Index Scan"));
            queryPlan.setHasFullTableScan(planText.contains("Table Scan"));
            
        } catch (Exception e) {
            log.error("Error analyzing SQL Server query", e);
            queryPlan.setExecutionPlan("ERROR: " + e.getMessage());
        }
    }

    public List<QueryPlan> getQueryPlans(Long queryId) {
        return queryPlanRepository.findByQueryMetricsId(queryId);
    }

    public List<QueryPlan> getQueriesWithFullTableScans() {
        return queryPlanRepository.findByHasFullTableScanTrue();
    }

    public List<QueryPlan> getQueriesWithoutIndexes() {
        return queryPlanRepository.findByUsesIndexFalse();
    }

    public List<QueryPlan> getRecentQueryPlans(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return queryPlanRepository.findByAnalyzedAtAfter(since);
    }
}
