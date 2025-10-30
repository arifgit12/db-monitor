package com.dbmonitor.controller;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.service.AlertService;
import com.dbmonitor.service.DatabaseConnectionService;
import com.dbmonitor.service.MultiDatabaseMonitoringService;
import com.dbmonitor.service.QueryMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
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
        List<DatabaseConnection> activeConnections = connectionService.getActiveConnections();

        boolean noConnections = activeConnections.isEmpty();
        model.addAttribute("noConnections", noConnections);

        if (activeConnections.isEmpty()) {
            model.addAttribute("connections", new ArrayList<DatabaseConnection>());
            model.addAttribute("unacknowledgedAlerts", 0);

            return "dashboard";
        }

        // Determine which connection to display
        DatabaseConnection selectedConnection;
        if (connectionId != null) {
            selectedConnection = connectionService.getConnectionById(connectionId)
                    .orElse(activeConnections.get(0));
        } else {
            // Try to get default, otherwise first active
            selectedConnection = connectionService.getDefaultConnection()
                    .orElse(activeConnections.get(0));
        }


        model.addAttribute("connections", activeConnections);
        model.addAttribute("selectedConnection", selectedConnection);
        model.addAttribute("currentMetrics", 
                multiDatabaseMonitoringService.getCurrentMetrics(selectedConnection.getId()));
        model.addAttribute("unacknowledgedAlerts", alertService.getUnacknowledgedCount());
        
        return "dashboard";
    }

    @GetMapping("/queries")
    public String queries(Model model) {
        model.addAttribute("queries", queryMonitoringService.getAllQueries());
        model.addAttribute("slowQueries", queryMonitoringService.getSlowQueries());
        model.addAttribute("connections", connectionService.getActiveConnections());
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
