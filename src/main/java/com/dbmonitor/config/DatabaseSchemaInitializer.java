package com.dbmonitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes database schema on application startup.
 * Creates the persistent_logins table if it doesn't exist.
 */
@Component
public class DatabaseSchemaInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchema() {
        try {
            logger.info("Checking if persistent_logins table exists...");
            
            // Check if table exists
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'persistent_logins'",
                Integer.class
            );
            
            if (count == null || count == 0) {
                logger.info("Creating persistent_logins table...");
                
                // Create table
                jdbcTemplate.execute(
                    "CREATE TABLE persistent_logins (" +
                    "    username VARCHAR(64) NOT NULL," +
                    "    series VARCHAR(64) NOT NULL PRIMARY KEY," +
                    "    token VARCHAR(64) NOT NULL," +
                    "    last_used DATETIME2 NOT NULL" +
                    ")"
                );
                
                // Create index
                jdbcTemplate.execute(
                    "CREATE INDEX IX_persistent_logins_username ON persistent_logins(username)"
                );
                
                logger.info("Successfully created persistent_logins table");
            } else {
                logger.info("persistent_logins table already exists");
            }
        } catch (Exception e) {
            logger.error("Error initializing database schema", e);
            // Don't throw exception - let app continue even if table creation fails
        }
    }
}
