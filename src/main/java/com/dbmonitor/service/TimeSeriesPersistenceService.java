package com.dbmonitor.service;

import com.dbmonitor.model.DatabaseMetrics;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TimeSeriesPersistenceService {

    @Value("${influxdb.url:http://localhost:8086}")
    private String influxUrl;

    @Value("${influxdb.token:}")
    private String influxToken;

    @Value("${influxdb.org:dbmonitor}")
    private String influxOrg;

    @Value("${influxdb.bucket:metrics}")
    private String influxBucket;

    @Value("${influxdb.enabled:false}")
    private boolean influxEnabled;

    private InfluxDBClient influxDBClient;

    public void persistMetrics(DatabaseMetrics metrics) {
        if (!influxEnabled || influxToken.isEmpty()) {
            log.debug("InfluxDB persistence is disabled or not configured");
            return;
        }

        try {
            if (influxDBClient == null) {
                influxDBClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrg, influxBucket);
            }

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            Point point = Point.measurement("database_metrics")
                .addTag("connection_id", String.valueOf(metrics.getConnectionId()))
                .addTag("connection_name", metrics.getConnectionName())
                .addTag("database_type", metrics.getDatabaseType())
                .addField("cpu_usage", metrics.getCpuUsage())
                .addField("memory_usage", metrics.getMemoryUsage())
                .addField("active_connections", metrics.getActiveConnections())
                .addField("idle_connections", metrics.getIdleConnections())
                .addField("total_connections", metrics.getTotalConnections())
                .addField("max_connections", metrics.getMaxConnections())
                .addField("connection_usage_percent", metrics.getConnectionUsagePercent())
                .addField("waiting_threads", metrics.getWaitingThreads())
                .time(Instant.now(), WritePrecision.NS);

            writeApi.writePoint(point);
            log.debug("Persisted metrics to InfluxDB for connection: {}", metrics.getConnectionName());

        } catch (Exception e) {
            log.error("Error persisting metrics to InfluxDB", e);
        }
    }

    public List<DatabaseMetrics> queryMetrics(Long connectionId, String timeRange) {
        if (!influxEnabled || influxToken.isEmpty()) {
            log.debug("InfluxDB is disabled or not configured");
            return new ArrayList<>();
        }

        try {
            if (influxDBClient == null) {
                influxDBClient = InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrg, influxBucket);
            }

            String flux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: %s) " +
                "|> filter(fn: (r) => r._measurement == \"database_metrics\") " +
                "|> filter(fn: (r) => r.connection_id == \"%s\")",
                influxBucket, timeRange, connectionId
            );

            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, influxOrg);
            List<DatabaseMetrics> metricsList = new ArrayList<>();

            // Parse the flux results into DatabaseMetrics objects
            // This is a simplified version - you'd need more complex parsing in production
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    DatabaseMetrics metrics = DatabaseMetrics.builder()
                        .connectionId(connectionId)
                        .connectionName((String) record.getValueByKey("connection_name"))
                        .databaseType((String) record.getValueByKey("database_type"))
                        .build();
                    metricsList.add(metrics);
                }
            }

            return metricsList;

        } catch (Exception e) {
            log.error("Error querying metrics from InfluxDB", e);
            return new ArrayList<>();
        }
    }

    public void close() {
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }
}
