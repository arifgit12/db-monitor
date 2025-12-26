package com.dbmonitor.service;

import com.dbmonitor.model.IndexRecommendation;
import com.dbmonitor.model.QueryMetrics;
import com.dbmonitor.repository.IndexRecommendationRepository;
import com.dbmonitor.repository.QueryMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IndexRecommendationService {

    @Autowired
    private IndexRecommendationRepository indexRecommendationRepository;

    @Autowired
    private QueryMetricsRepository queryMetricsRepository;

    @Autowired
    private DatabaseConnectionService connectionService;

    public List<IndexRecommendation> generateRecommendations(Long connectionId) {
        try {
            List<IndexRecommendation> recommendations = new ArrayList<>();
            DataSource dataSource = connectionService.getDataSource(connectionId);

            // Analyze slow queries
            List<QueryMetrics> slowQueries = queryMetricsRepository.findByIsSlowTrue();
            
            // Analyze queries for potential index candidates
            Map<String, Set<String>> tableColumns = analyzeQueriesForIndexCandidates(slowQueries);
            
            try (Connection conn = dataSource.getConnection()) {
                // Get existing indexes
                Map<String, Set<String>> existingIndexes = getExistingIndexes(conn);
                
                // Generate recommendations for missing indexes
                for (Map.Entry<String, Set<String>> entry : tableColumns.entrySet()) {
                    String tableName = entry.getKey();
                    Set<String> columns = entry.getValue();
                    
                    Set<String> existingCols = existingIndexes.getOrDefault(tableName, new HashSet<>());
                    
                    for (String column : columns) {
                        if (!existingCols.contains(column)) {
                            IndexRecommendation recommendation = createRecommendation(
                                connectionId, tableName, column, slowQueries.size()
                            );
                            recommendations.add(recommendation);
                            indexRecommendationRepository.save(recommendation);
                        }
                    }
                }
            }

            log.info("Generated {} index recommendations for connection ID: {}", 
                recommendations.size(), connectionId);
            return recommendations;
            
        } catch (Exception e) {
            log.error("Error generating index recommendations", e);
            throw new RuntimeException("Failed to generate index recommendations", e);
        }
    }

    private Map<String, Set<String>> analyzeQueriesForIndexCandidates(List<QueryMetrics> queries) {
        Map<String, Set<String>> tableColumns = new HashMap<>();
        
        // Patterns to extract table and column names from queries
        Pattern wherePattern = Pattern.compile("WHERE\\s+(\\w+)\\.(\\w+)", Pattern.CASE_INSENSITIVE);
        Pattern joinPattern = Pattern.compile("JOIN\\s+(\\w+)\\s+ON\\s+(\\w+)\\.(\\w+)", Pattern.CASE_INSENSITIVE);
        Pattern orderPattern = Pattern.compile("ORDER BY\\s+(\\w+)\\.(\\w+)", Pattern.CASE_INSENSITIVE);
        
        for (QueryMetrics query : queries) {
            String sql = query.getQueryText();
            if (sql == null) continue;
            
            // Extract columns from WHERE clause
            Matcher whereMatcher = wherePattern.matcher(sql);
            while (whereMatcher.find()) {
                String table = whereMatcher.group(1);
                String column = whereMatcher.group(2);
                tableColumns.computeIfAbsent(table, k -> new HashSet<>()).add(column);
            }
            
            // Extract columns from JOIN clause
            Matcher joinMatcher = joinPattern.matcher(sql);
            while (joinMatcher.find()) {
                String table = joinMatcher.group(1);
                String column = joinMatcher.group(3);
                tableColumns.computeIfAbsent(table, k -> new HashSet<>()).add(column);
            }
            
            // Extract columns from ORDER BY clause
            Matcher orderMatcher = orderPattern.matcher(sql);
            while (orderMatcher.find()) {
                String table = orderMatcher.group(1);
                String column = orderMatcher.group(2);
                tableColumns.computeIfAbsent(table, k -> new HashSet<>()).add(column);
            }
        }
        
        return tableColumns;
    }

    private Map<String, Set<String>> getExistingIndexes(Connection conn) throws Exception {
        Map<String, Set<String>> indexes = new HashMap<>();
        DatabaseMetaData metaData = conn.getMetaData();
        
        // Get all tables
        ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            
            // Get indexes for this table
            ResultSet indexInfo = metaData.getIndexInfo(null, null, tableName, false, false);
            while (indexInfo.next()) {
                String columnName = indexInfo.getString("COLUMN_NAME");
                if (columnName != null) {
                    indexes.computeIfAbsent(tableName, k -> new HashSet<>()).add(columnName);
                }
            }
        }
        
        return indexes;
    }

    private IndexRecommendation createRecommendation(Long connectionId, String tableName, 
                                                     String columnName, long affectedQueries) {
        // Calculate impact score (simplified)
        int impactScore = Math.min(100, (int)(affectedQueries * 10));
        
        String createStatement = String.format(
            "CREATE INDEX idx_%s_%s ON %s(%s);",
            tableName, columnName, tableName, columnName
        );
        
        return IndexRecommendation.builder()
            .connectionId(connectionId)
            .tableName(tableName)
            .columnNames(columnName)
            .indexType("BTREE")
            .recommendationReason("Column frequently used in WHERE/JOIN/ORDER BY clauses")
            .impactScore(impactScore)
            .affectedQueries(affectedQueries)
            .estimatedPerformanceGain(calculatePerformanceGain(affectedQueries))
            .createIndexStatement(createStatement)
            .status("PENDING")
            .recommendedAt(LocalDateTime.now())
            .build();
    }

    private Double calculatePerformanceGain(long affectedQueries) {
        // Simplified calculation: more affected queries = higher gain
        return Math.min(90.0, affectedQueries * 5.0);
    }

    public List<IndexRecommendation> getRecommendations(Long connectionId) {
        return indexRecommendationRepository.findByConnectionId(connectionId);
    }

    public List<IndexRecommendation> getPendingRecommendations(Long connectionId) {
        return indexRecommendationRepository.findByConnectionIdAndStatusOrderByImpactScoreDesc(
            connectionId, "PENDING");
    }

    public void applyRecommendation(Long recommendationId) {
        IndexRecommendation recommendation = indexRecommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
        
        try {
            DataSource dataSource = connectionService.getDataSource(recommendation.getConnectionId());
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                stmt.execute(recommendation.getCreateIndexStatement());
                
                recommendation.setStatus("APPLIED");
                recommendation.setAppliedAt(LocalDateTime.now());
                indexRecommendationRepository.save(recommendation);
                
                log.info("Applied index recommendation: {}", recommendationId);
            }
        } catch (Exception e) {
            log.error("Error applying index recommendation", e);
            throw new RuntimeException("Failed to apply index recommendation", e);
        }
    }

    public void rejectRecommendation(Long recommendationId) {
        IndexRecommendation recommendation = indexRecommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
        
        recommendation.setStatus("REJECTED");
        indexRecommendationRepository.save(recommendation);
        log.info("Rejected index recommendation: {}", recommendationId);
    }

    public List<IndexRecommendation> getAppliedRecommendations() {
        return indexRecommendationRepository.findByStatus("APPLIED");
    }

    public List<IndexRecommendation> getRecommendationsForTable(String tableName) {
        return indexRecommendationRepository.findByTableName(tableName);
    }
}
