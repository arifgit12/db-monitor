package com.dbmonitor.config;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.service.DatabaseConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Slf4j
public class DefaultConnectionInitializer implements ApplicationRunner {

    @Autowired
    private DatabaseConnectionService connectionService;

    @Override
    public void run(ApplicationArguments args) {
        // Check if any connections exist
        log.info("Checking for existing database connections...");
        int existingCount = connectionService.getAllConnections().size();
        log.info("Found {} existing connections", existingCount);

        if (existingCount == 0) {
            log.info("No database connections found. Creating default H2 connection...");

            DatabaseConnection defaultConnection = DatabaseConnection.builder()
                    .connectionName("Default H2 Database")
                    .databaseType("H2")
                    .host("localhost")
                    .port(0) // H2 in-memory doesn't use port
                    .databaseName("testdb")
                    .username("sa")
                    .password("")
                    .description("Default in-memory H2 database for demonstration")
                    .isActive(true)
                    .isDefault(true)
                    .minPoolSize(5)
                    .maxPoolSize(20)
                    .connectionTimeout(30000)
                    .build();

            try {
                DatabaseConnection saved = connectionService.saveConnection(defaultConnection);
                log.info("Default H2 connection created successfully with ID: {}", saved.getId());
                log.info("Connection details - Name: {}, Type: {}, JDBC URL: {}",
                    saved.getConnectionName(), saved.getDatabaseType(), saved.getJdbcUrl());

                // Test the connection
                boolean testResult = connectionService.testConnection(saved);
                log.info("Connection test result: {}", testResult ? "SUCCESS" : "FAILED");
            } catch (Exception e) {
                log.error("Error creating default connection", e);
                e.printStackTrace();
            }
        } else {
            log.info("Existing connections found, skipping default connection creation");
        }
    }
}
