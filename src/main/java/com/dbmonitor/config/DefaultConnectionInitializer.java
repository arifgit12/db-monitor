package com.dbmonitor.config;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.service.DatabaseConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1)
@Slf4j
public class DefaultConnectionInitializer implements ApplicationRunner {

    @Autowired
    private DatabaseConnectionService connectionService;

    @Autowired
    private Environment environment;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.driverClassName:}")
    private String driverClassName;

    @Value("${spring.datasource.username:sa}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minPoolSize;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private int connectionTimeout;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== DefaultConnectionInitializer Starting ===");
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("Active Spring Profiles: {}", activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default");
        log.info("Datasource URL: {}", datasourceUrl);
        log.info("Driver Class: {}", driverClassName);

        log.info("Checking for existing database connections...");
        int existingCount = connectionService.getAllConnections().size();
        log.info("Found {} existing connections", existingCount);

        if (existingCount == 0) {
            log.info("No database connections found. Creating default connection from Spring datasource configuration...");

            if (datasourceUrl == null || datasourceUrl.trim().isEmpty()) {
                log.warn("No datasource URL configured. Skipping default connection creation.");
                return;
            }

            try {
                DatabaseConnectionInfo info = parseDatasourceUrl(datasourceUrl, driverClassName);
                log.info("Parsed database info - Type: {}, Host: {}, Port: {}, Database: {}",
                    info.type, info.host, info.port, info.databaseName);

                String profileName = activeProfiles.length > 0 ? activeProfiles[0] : "default";
                String connectionName = String.format("Default %s Database (%s)", info.type, profileName);

                DatabaseConnection defaultConnection = DatabaseConnection.builder()
                        .connectionName(connectionName)
                        .databaseType(info.type)
                        .host(info.host)
                        .port(info.port)
                        .databaseName(info.databaseName)
                        .customJdbcUrl(datasourceUrl)  // Use the actual JDBC URL from properties
                        .customDriverClassName(driverClassName)  // Use the actual driver from properties
                        .username(username)
                        .password(password)
                        .description(String.format("Auto-created from Spring datasource (profile: %s)", profileName))
                        .isActive(true)
                        .isDefault(true)
                        .minPoolSize(minPoolSize)
                        .maxPoolSize(maxPoolSize)
                        .connectionTimeout(connectionTimeout)
                        .build();

                DatabaseConnection saved = connectionService.saveConnection(defaultConnection);
                log.info("Default connection created successfully with ID: {}", saved.getId());
                log.info("Connection details - Name: {}, Type: {}, JDBC URL: {}",
                    saved.getConnectionName(), saved.getDatabaseType(), saved.getJdbcUrl());

                // Test the connection
                boolean testResult = connectionService.testConnection(saved);
                log.info("Connection test result: {}", testResult ? "SUCCESS" : "FAILED");

                if (!testResult) {
                    log.warn("Connection test failed! Please check your database configuration.");
                }
            } catch (Exception e) {
                log.error("Error creating default connection from datasource configuration", e);
                e.printStackTrace();
            }
        } else {
            log.info("Existing connections found, skipping default connection creation");
        }
        log.info("=== DefaultConnectionInitializer Completed ===");
    }

    private DatabaseConnectionInfo parseDatasourceUrl(String jdbcUrl, String driverClass) {
        DatabaseConnectionInfo info = new DatabaseConnectionInfo();

        // Detect database type from driver class or URL
        if (driverClass.contains("sqlserver") || jdbcUrl.contains("sqlserver")) {
            info.type = "SQL Server";
            parseSqlServerUrl(jdbcUrl, info);
        } else if (driverClass.contains("h2") || jdbcUrl.contains(":h2:")) {
            info.type = "H2";
            parseH2Url(jdbcUrl, info);
        } else if (driverClass.contains("mysql") || jdbcUrl.contains("mysql")) {
            info.type = "MySQL";
            parseMySQLUrl(jdbcUrl, info);
        } else if (driverClass.contains("postgresql") || jdbcUrl.contains("postgresql")) {
            info.type = "PostgreSQL";
            parsePostgreSQLUrl(jdbcUrl, info);
        } else if (driverClass.contains("oracle") || jdbcUrl.contains("oracle")) {
            info.type = "Oracle";
            parseOracleUrl(jdbcUrl, info);
        } else {
            info.type = "Unknown";
            info.host = "localhost";
            info.port = 0;
            info.databaseName = "database";
        }

        return info;
    }

    private void parseSqlServerUrl(String url, DatabaseConnectionInfo info) {
        // jdbc:sqlserver://localhost:1433;databaseName=linq2TestDb;...
        Pattern pattern = Pattern.compile("jdbc:sqlserver://([^:;]+)(?::(\\d+))?.*databaseName=([^;]+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            info.host = matcher.group(1);
            info.port = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1433;
            info.databaseName = matcher.group(3);
        } else {
            info.host = "localhost";
            info.port = 1433;
            info.databaseName = "master";
        }
    }

    private void parseH2Url(String url, DatabaseConnectionInfo info) {
        // jdbc:h2:mem:testdb or jdbc:h2:file:./data/test
        if (url.contains("mem:")) {
            String[] parts = url.split("mem:");
            info.databaseName = parts.length > 1 ? parts[1].split(";")[0] : "testdb";
            info.host = "localhost";
            info.port = 0;
        } else if (url.contains("file:")) {
            String[] parts = url.split("file:");
            info.databaseName = parts.length > 1 ? parts[1].split(";")[0] : "testdb";
            info.host = "localhost";
            info.port = 0;
        } else {
            info.host = "localhost";
            info.port = 0;
            info.databaseName = "testdb";
        }
    }

    private void parseMySQLUrl(String url, DatabaseConnectionInfo info) {
        // jdbc:mysql://localhost:3306/database
        Pattern pattern = Pattern.compile("jdbc:mysql://([^:;/]+)(?::(\\d+))?/([^?]+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            info.host = matcher.group(1);
            info.port = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 3306;
            info.databaseName = matcher.group(3);
        } else {
            info.host = "localhost";
            info.port = 3306;
            info.databaseName = "database";
        }
    }

    private void parsePostgreSQLUrl(String url, DatabaseConnectionInfo info) {
        // jdbc:postgresql://localhost:5432/database
        Pattern pattern = Pattern.compile("jdbc:postgresql://([^:;/]+)(?::(\\d+))?/([^?]+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            info.host = matcher.group(1);
            info.port = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 5432;
            info.databaseName = matcher.group(3);
        } else {
            info.host = "localhost";
            info.port = 5432;
            info.databaseName = "database";
        }
    }

    private void parseOracleUrl(String url, DatabaseConnectionInfo info) {
        // jdbc:oracle:thin:@localhost:1521:XE
        Pattern pattern = Pattern.compile("jdbc:oracle:thin:@([^:;]+)(?::(\\d+))?[:/]([^?]+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            info.host = matcher.group(1);
            info.port = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1521;
            info.databaseName = matcher.group(3);
        } else {
            info.host = "localhost";
            info.port = 1521;
            info.databaseName = "XE";
        }
    }

    private static class DatabaseConnectionInfo {
        String type;
        String host;
        int port;
        String databaseName;
    }
}