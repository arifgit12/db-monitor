package com.dbmonitor.service;

import com.dbmonitor.model.QueryMetrics;
import com.dbmonitor.repository.QueryMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class QueryMonitoringService {

    @Autowired
    private QueryMetricsRepository queryMetricsRepository;

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
}
