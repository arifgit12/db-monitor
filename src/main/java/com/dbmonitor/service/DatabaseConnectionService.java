package com.dbmonitor.service;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.repository.DatabaseConnectionRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class DatabaseConnectionService {


    @Value( "${dbmonitor.dev-mode:false}")
    private boolean devMode;

    @Autowired
    private DatabaseConnectionRepository connectionRepository;

    // Cache for data sources
    private final Map<Long, HikariDataSource> dataSourceCache = new ConcurrentHashMap<>();

    public List<DatabaseConnection> getAllConnections() {
        return connectionRepository.findAll();
    }

    public List<DatabaseConnection> getActiveConnections() {
        return connectionRepository.findByIsActiveTrue();
    }

    public Optional<DatabaseConnection> getConnectionById(Long id) {
        return connectionRepository.findById(id);
    }

    public Optional<DatabaseConnection> getConnectionByName(String name) {
        return connectionRepository.findByConnectionName(name);
    }

    public Optional<DatabaseConnection> getDefaultConnection() {
        return connectionRepository.findByIsDefaultTrue();
    }

    public DatabaseConnection saveConnection(DatabaseConnection connection) {
        // Ensure only one default connection
        if (connection.getIsDefault()) {
            connectionRepository.findByIsDefaultTrue().ifPresent(existingDefault -> {
                if (!existingDefault.getId().equals(connection.getId())) {
                    existingDefault.setIsDefault(false);
                    connectionRepository.save(existingDefault);
                }
            });
        }

        DatabaseConnection saved = connectionRepository.save(connection);
        log.info("Database connection saved: {}", saved.getConnectionName());
        return saved;
    }

    public void deleteConnection(Long id) {
        // Close and remove data source from cache
        if (dataSourceCache.containsKey(id)) {
            HikariDataSource ds = dataSourceCache.remove(id);
            ds.close();
            log.info("Closed data source for connection ID: {}", id);
        }
        connectionRepository.deleteById(id);
        log.info("Deleted connection ID: {}", id);
    }

    public boolean testConnection(DatabaseConnection connection) {
        try {
            HikariDataSource testDataSource = createDataSource(connection);
            try (Connection conn = testDataSource.getConnection()) {
                boolean isValid = conn.isValid(5);
                connection.setLastTestedAt(LocalDateTime.now());
                connection.setLastTestStatus(isValid ? "SUCCESS" : "FAILED");
                
                if (connection.getId() != null) {
                    connectionRepository.save(connection);
                }
                
                testDataSource.close();
                log.info("Connection test for '{}': {}", connection.getConnectionName(), 
                        isValid ? "SUCCESS" : "FAILED");
                return isValid;
            }
        } catch (SQLException e) {
            log.error("Connection test failed for '{}'", connection.getConnectionName(), e);
            connection.setLastTestedAt(LocalDateTime.now());
            connection.setLastTestStatus("FAILED: " + e.getMessage());
            
            if (connection.getId() != null) {
                connectionRepository.save(connection);
            }
            return false;
        }
    }

    public DataSource getDataSource(Long connectionId) {
        // Return cached data source or create new one
        return dataSourceCache.computeIfAbsent(connectionId, id -> {
            DatabaseConnection connection = connectionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
            return createDataSource(connection);
        });
    }

    public void closeDataSource(Long connectionId) {
        HikariDataSource ds = dataSourceCache.remove(connectionId);
        if (ds != null) {
            ds.close();
            log.info("Closed data source for connection ID: {}", connectionId);
        }
    }

    public void closeAllDataSources() {
        dataSourceCache.forEach((id, ds) -> {
            ds.close();
            log.info("Closed data source for connection ID: {}", id);
        });
        dataSourceCache.clear();
    }

    private HikariDataSource createDataSource(DatabaseConnection connection) {
        HikariConfig config = new HikariConfig();

        String jdbcUrl = buildJdbcUrlWithSSL(connection);
        config.setJdbcUrl(jdbcUrl);

//        config.setJdbcUrl(connection.getJdbcUrl());
        config.setUsername(connection.getUsername());
        config.setPassword(connection.getPassword());
        config.setDriverClassName(connection.getDriverClassName());
        
        config.setMinimumIdle(connection.getMinPoolSize());
        config.setMaximumPoolSize(connection.getMaxPoolSize());
        config.setConnectionTimeout(connection.getConnectionTimeout());
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setLeakDetectionThreshold(60000);
        
        config.setPoolName(connection.getConnectionName() + "-pool");
        
        // Register metrics
        config.setMetricRegistry(null);
        
        log.info("Creating data source for connection: {}", connection.getConnectionName());
        return new HikariDataSource(config);
    }

    private String buildJdbcUrlWithSSL(DatabaseConnection c) {
        String url = c.getJdbcUrl();
        if (!isSqlServerConnection(c)) return url;

        boolean devSkipValidation = Boolean.TRUE.equals(c.getSkipTlsValidation()) || devMode;

        if (devSkipValidation) {
            url = isMssqlUrl(url)
                    ? mssqlAddOrReplace(url, "encrypt", "true")
                    : addOrReplace(url, "encrypt", "true");
            url = isMssqlUrl(url)
                    ? mssqlAddOrReplace(url, "trustServerCertificate", "true")
                    : addOrReplace(url, "trustServerCertificate", "true");
            // IMPORTANT: do not add trustStore* in dev mode
            return url;
        }

        // Production validation
        url = isMssqlUrl(url)
                ? mssqlAddOrReplace(url, "encrypt", "true")
                : addOrReplace(url, "encrypt", "true");
        url = isMssqlUrl(url)
                ? mssqlAddOrReplace(url, "trustServerCertificate", "false")
                : addOrReplace(url, "trustServerCertificate", "false");

        if (notBlank(c.getTrustStorePath()) && notBlank(c.getTrustStorePassword())) {
            url = isMssqlUrl(url)
                    ? mssqlAddOrReplace(url, "trustStore", c.getTrustStorePath())
                    : addOrReplace(url, "trustStore", c.getTrustStorePath());
            url = isMssqlUrl(url)
                    ? mssqlAddOrReplace(url, "trustStorePassword", c.getTrustStorePassword())
                    : addOrReplace(url, "trustStorePassword", c.getTrustStorePassword());
            if (notBlank(c.getHostNameInCertificate())) {
                url = isMssqlUrl(url)
                        ? mssqlAddOrReplace(url, "hostNameInCertificate", c.getHostNameInCertificate())
                        : addOrReplace(url, "hostNameInCertificate", c.getHostNameInCertificate());
            }
        } else {
            log.warn("MSSQL TLS validation enabled but no truststore for '{}'. Provide trustStore/Password or enable skipTlsValidation/devMode.",
                    c.getConnectionName());
        }

        // optional default
        url = isMssqlUrl(url)
                ? mssqlAddOrReplace(url, "loginTimeout", "30")
                : addIfMissing(url, "loginTimeout", "30");

        return url;
    }

    private static String mssqlAddOrReplace(String url, String key, String value) {
        // Split "base;propA=...;propB=..." into base + props
        String base = url;
        String propsPart = "";
        int firstSemi = url.indexOf(';');
        if (firstSemi >= 0) {
            base = url.substring(0, firstSemi);
            propsPart = url.substring(firstSemi + 1);
        }

        // Parse existing properties into a map (case-insensitive keys)
        java.util.LinkedHashMap<String, String> props = new java.util.LinkedHashMap<>();
        if (!propsPart.isEmpty()) {
            for (String p : propsPart.split(";")) {
                if (p.isBlank()) continue;
                int eq = p.indexOf('=');
                if (eq > 0) {
                    String k = p.substring(0, eq).trim();
                    String v = p.substring(eq + 1).trim();
                    props.put(k.toLowerCase(), k + "=" + v); // store original k=v for stable casing
                } else {
                    // handle flags like ";IntegratedSecurity" if ever present
                    props.put(p.toLowerCase(), p);
                }
            }
        }

        // Put/replace the target key (case-insensitive), preserving exact "k=v" format on rebuild
        String normalizedKey = key.toLowerCase();
        props.put(normalizedKey, key + "=" + value);

        // Rebuild string
        StringBuilder sb = new StringBuilder(base);
        sb.append(';');
        boolean first = true;
        for (String kv : props.values()) {
            if (!first) sb.append(';');
            sb.append(kv);
            first = false;
        }
        return sb.toString();
    }

    private static boolean isMssqlUrl(String url) {
        return url != null && url.toLowerCase().startsWith("jdbc:sqlserver:");
    }

    private static boolean notBlank(String s){ return s != null && !s.isBlank(); }

    private static String addIfMissing(String url, String k, String v) {
        return url.matches("(?i).*([?&])" + k + "=.*") ? url : addOrReplace(url, k, v);
    }
    private static String addOrReplace(String url, String k, String v) {
        String base = url;
        String query = "";
        int q = url.indexOf('?');
        if (q >= 0) { base = url.substring(0, q); query = url.substring(q + 1); }
        // remove existing key (case-insensitive)
        StringBuilder newQ = new StringBuilder();
        if (!query.isEmpty()) {
            for (String p : query.split("&")) {
                if (!p.toLowerCase().startsWith((k + "=").toLowerCase())) {
                    if (newQ.length() > 0) newQ.append('&');
                    newQ.append(p);
                }
            }
        }
        if (newQ.length() > 0) newQ.append('&');
        newQ.append(k).append('=').append(v);
        return base + "?" + newQ;
    }

    private boolean isSqlServerConnection(DatabaseConnection connection) {
        String driverClassName = connection.getDriverClassName();
        String jdbcUrl = connection.getJdbcUrl();

        return (driverClassName != null && driverClassName.contains("sqlserver")) ||
                (jdbcUrl != null && jdbcUrl.toLowerCase().contains("sqlserver"));
    }

    public void refreshDataSource(Long connectionId) {
        closeDataSource(connectionId);
        log.info("Data source refreshed for connection ID: {}", connectionId);
    }

    public boolean connectionExists(String connectionName) {
        return connectionRepository.existsByConnectionName(connectionName);
    }

    public DatabaseConnection toggleActive(Long id) {
        DatabaseConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
        
        connection.setIsActive(!connection.getIsActive());
        
        if (!connection.getIsActive()) {
            // Close data source if deactivating
            closeDataSource(id);
        }
        
        return connectionRepository.save(connection);
    }

    public DatabaseConnection setAsDefault(Long id) {
        // Remove default from all connections
        connectionRepository.findByIsDefaultTrue().ifPresent(existingDefault -> {
            existingDefault.setIsDefault(false);
            connectionRepository.save(existingDefault);
        });
        
        // Set new default
        DatabaseConnection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
        connection.setIsDefault(true);
        connection.setIsActive(true);
        
        return connectionRepository.save(connection);
    }
}
