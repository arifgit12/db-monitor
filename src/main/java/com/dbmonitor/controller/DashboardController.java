package com.dbmonitor.controller;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.model.DatabaseMetrics;
import com.dbmonitor.service.AlertService;
import com.dbmonitor.service.DatabaseConnectionService;
import com.dbmonitor.service.MultiDatabaseMonitoringService;
import com.dbmonitor.service.QueryMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
public class DashboardController {

    @Autowired
    private MultiDatabaseMonitoringService multiDatabaseMonitoringService;

    @Autowired
    private QueryMonitoringService queryMonitoringService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private DatabaseConnectionService connectionService;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) Long connectionId, Model model) {
        log.info("=== DASHBOARD REQUEST START ===");

        // Log all connections in database
        List<DatabaseConnection> allConnections = connectionService.getAllConnections();
        log.info("Total connections in database: {}", allConnections.size());
        allConnections.forEach(conn ->
            log.info("  - Connection ID: {}, Name: '{}', Active: {}, Default: {}",
                conn.getId(), conn.getConnectionName(), conn.getIsActive(), conn.getIsDefault())
        );

        List<DatabaseConnection> activeConnections = connectionService.getActiveConnections();
        log.info("Active connections found: {}", activeConnections.size());

        boolean noConnections = activeConnections.isEmpty();
        model.addAttribute("noConnections", noConnections);

        if (activeConnections.isEmpty()) {
            log.warn("No active connections available for dashboard");
            model.addAttribute("connections", new ArrayList<DatabaseConnection>());
            model.addAttribute("unacknowledgedAlerts", 0);
            log.info("=== DASHBOARD REQUEST END (NO CONNECTIONS) ===");
            return "dashboard";
        }

        // Determine which connection to display
        DatabaseConnection selectedConnection;
        if (connectionId != null) {
            log.info("Selecting connection by ID: {}", connectionId);
            selectedConnection = connectionService.getConnectionById(connectionId)
                    .orElse(activeConnections.get(0));
        } else {
            // Try to get default, otherwise first active
            log.info("Selecting default connection or first active");
            Optional<DatabaseConnection> defaultConn = connectionService.getDefaultConnection();
            log.info("Default connection present: {}", defaultConn.isPresent());
            if (defaultConn.isPresent()) {
                log.info("Default connection - ID: {}, Name: '{}'",
                    defaultConn.get().getId(), defaultConn.get().getConnectionName());
            }
            selectedConnection = defaultConn.orElse(activeConnections.get(0));
        }

        log.info("SELECTED CONNECTION - ID: {}, Name: '{}', Active: {}, Default: {}",
            selectedConnection.getId(),
            selectedConnection.getConnectionName(),
            selectedConnection.getIsActive(),
            selectedConnection.getIsDefault());

        model.addAttribute("connections", activeConnections);
        model.addAttribute("selectedConnection", selectedConnection);

        try {
            DatabaseMetrics currentMetrics = multiDatabaseMonitoringService.getCurrentMetrics(selectedConnection.getId());
            model.addAttribute("currentMetrics", currentMetrics);
            log.info("Current metrics loaded successfully for connection ID: {}", selectedConnection.getId());
        } catch (Exception e) {
            log.error("Error loading current metrics for connection ID: {}", selectedConnection.getId(), e);
        }

        model.addAttribute("unacknowledgedAlerts", alertService.getUnacknowledgedCount());

        log.info("=== DASHBOARD REQUEST END (SUCCESS) ===");
        return "dashboard";
    }

    @GetMapping("/queries")
    public String queries(@RequestParam(required = false) Long connectionId, Model model) {
        List<DatabaseConnection> activeConnections = connectionService.getActiveConnections();

        boolean noConnections = activeConnections.isEmpty();
        model.addAttribute("noConnections", noConnections);

        if (activeConnections.isEmpty()) {
            model.addAttribute("connections", new ArrayList<DatabaseConnection>());
            model.addAttribute("queries", new ArrayList<>());
            model.addAttribute("slowQueries", new ArrayList<>());
            return "queries";
        }

        // Determine which connection to display
        DatabaseConnection selectedConnection;
        if (connectionId != null) {
            log.info("Selecting connection by ID: {}", connectionId);
            selectedConnection = connectionService.getConnectionById(connectionId)
                    .orElse(activeConnections.get(0));
        } else {
            log.info("Selecting default connection or first active");
            selectedConnection = connectionService.getDefaultConnection()
                    .orElse(activeConnections.get(0));
        }

        log.info("Fetching queries for connection - ID: {}, Name: '{}'",
            selectedConnection.getId(), selectedConnection.getConnectionName());

        model.addAttribute("connections", activeConnections);
        model.addAttribute("selectedConnection", selectedConnection);
        model.addAttribute("queries", queryMonitoringService.getQueriesFromDatabase(selectedConnection.getId()));
        model.addAttribute("slowQueries", queryMonitoringService.getSlowQueriesFromDatabase(selectedConnection.getId()));
        return "queries";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("alerts", alertService.getAllAlerts());
        model.addAttribute("unacknowledgedCount", alertService.getUnacknowledgedCount());
        model.addAttribute("connections", connectionService.getActiveConnections());
        return "alerts";
    }

    @GetMapping("/connections")
    public String connections(@RequestParam(required = false) Long connectionId, Model model) {

        List<DatabaseConnection> activeConnections = connectionService.getActiveConnections();

        boolean noConnections = activeConnections.isEmpty();
        model.addAttribute("noConnections", noConnections);

        if (activeConnections.isEmpty()) {
            model.addAttribute("connections", new ArrayList<DatabaseConnection>());
            model.addAttribute("unacknowledgedAlerts", 0);
            return "connections";
        }

        DatabaseConnection selectedConnection;
        if (connectionId != null) {
            selectedConnection = connectionService.getConnectionById(connectionId)
                    .orElse(activeConnections.get(0));
        } else {
            selectedConnection = connectionService.getDefaultConnection()
                    .orElse(activeConnections.get(0));
        }

        model.addAttribute("connections", activeConnections);
        model.addAttribute("selectedConnection", selectedConnection);
        model.addAttribute("currentMetrics", 
                multiDatabaseMonitoringService.getCurrentMetrics(selectedConnection.getId()));
        return "connections";
    }

    @GetMapping("/performance")
    public String performance(@RequestParam(required = false) Long connectionId, Model model) {
        List<DatabaseConnection> activeConnections = connectionService.getActiveConnections();

        boolean noConnections = activeConnections.isEmpty();
        model.addAttribute("noConnections", noConnections);

        if (activeConnections.isEmpty()) {
            model.addAttribute("connections", new ArrayList<DatabaseConnection>());
            return "performance";
        }

        DatabaseConnection selectedConnection;
        if (connectionId != null) {
            selectedConnection = connectionService.getConnectionById(connectionId)
                    .orElse(activeConnections.get(0));
        } else {
            selectedConnection = connectionService.getDefaultConnection()
                    .orElse(activeConnections.get(0));
        }

        model.addAttribute("connections", activeConnections);
        model.addAttribute("selectedConnection", selectedConnection);
        model.addAttribute("metricsHistory", 
                multiDatabaseMonitoringService.getMetricsHistory(selectedConnection.getId(), 50));
        return "performance";
    }
}
