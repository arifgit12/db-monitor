package com.dbmonitor.config;

import com.dbmonitor.service.QueryMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private QueryMonitoringService queryMonitoringService;

    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void run(String... args) {
        log.info("Starting sample data generation...");
        
        // Schedule periodic query simulation
        scheduler.scheduleAtFixedRate(this::simulateQueries, 0, 10, TimeUnit.SECONDS);
    }

    private void simulateQueries() {
        try {
            // Simulate various types of queries
            String[] queryTypes = {"SELECT", "INSERT", "UPDATE", "DELETE"};
            String[] sampleQueries = {
                "SELECT * FROM users WHERE id = ?",
                "SELECT COUNT(*) FROM orders WHERE status = 'PENDING'",
                "INSERT INTO logs (message, timestamp) VALUES (?, ?)",
                "UPDATE products SET stock = stock - 1 WHERE id = ?",
                "DELETE FROM sessions WHERE expired_at < NOW()",
                "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id",
                "SELECT * FROM products WHERE category = ? ORDER BY price DESC LIMIT 10",
                "UPDATE users SET last_login = NOW() WHERE id = ?"
            };

            int numQueries = random.nextInt(5) + 1;
            
            for (int i = 0; i < numQueries; i++) {
                String query = sampleQueries[random.nextInt(sampleQueries.length)];
                String type = queryTypes[random.nextInt(queryTypes.length)];
                
                // Generate execution time (mostly fast, occasionally slow)
                long executionTime;
                if (random.nextDouble() < 0.1) { // 10% chance of slow query
                    executionTime = 1000 + random.nextInt(3000); // 1-4 seconds
                } else {
                    executionTime = 10 + random.nextInt(200); // 10-210ms
                }
                
                int rowsAffected = random.nextInt(100) + 1;
                
                queryMonitoringService.recordQuery(query, executionTime, type, rowsAffected);
            }
            
            log.debug("Simulated {} queries", numQueries);
            
        } catch (Exception e) {
            log.error("Error simulating queries", e);
        }
    }
}
