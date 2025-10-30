package com.dbmonitor.controller;

import com.dbmonitor.model.DatabaseConnection;
import com.dbmonitor.service.DatabaseConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/connections-config")
public class ConnectionController {

    @Autowired
    private DatabaseConnectionService connectionService;

    @GetMapping
    public String connectionsPage(Model model) {
        model.addAttribute("connections", connectionService.getAllConnections());
        model.addAttribute("newConnection", new DatabaseConnection());
        return "connections-config";
    }

    @GetMapping("/new")
    public String newConnectionForm(Model model) {
        model.addAttribute("connection", new DatabaseConnection());
        model.addAttribute("isEdit", false);
        return "connection-form";
    }

    @GetMapping("/edit/{id}")
    public String editConnectionForm(@PathVariable Long id, Model model) {
        DatabaseConnection connection = connectionService.getConnectionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
        model.addAttribute("connection", connection);
        model.addAttribute("isEdit", true);
        return "connection-form";
    }

    @PostMapping("/save")
    public String saveConnection(@ModelAttribute DatabaseConnection connection, 
                                RedirectAttributes redirectAttributes) {
        try {
            // Validate connection name uniqueness
            if (connection.getId() == null && 
                connectionService.connectionExists(connection.getConnectionName())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Connection name already exists: " + connection.getConnectionName());
                return "redirect:/connections-config/new";
            }

            connectionService.saveConnection(connection);
            redirectAttributes.addFlashAttribute("success", 
                "Connection saved successfully: " + connection.getConnectionName());
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving connection: " + e.getMessage());
        }
        
        return "redirect:/connections-config";
    }

    @PostMapping("/delete/{id}")
    public String deleteConnection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            DatabaseConnection connection = connectionService.getConnectionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
            
            connectionService.deleteConnection(id);
            redirectAttributes.addFlashAttribute("success", 
                "Connection deleted: " + connection.getConnectionName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting connection: " + e.getMessage());
        }
        
        return "redirect:/connections-config";
    }

    @PostMapping("/test/{id}")
    @ResponseBody
    public Map<String, Object> testConnection(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatabaseConnection connection = connectionService.getConnectionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
            
            boolean success = connectionService.testConnection(connection);
            result.put("success", success);
            result.put("message", success ? "Connection successful!" : "Connection failed!");
            result.put("status", connection.getLastTestStatus());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error testing connection");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @PostMapping("/toggle-active/{id}")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            DatabaseConnection connection = connectionService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", 
                "Connection " + (connection.getIsActive() ? "activated" : "deactivated") + 
                ": " + connection.getConnectionName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error toggling connection: " + e.getMessage());
        }
        
        return "redirect:/connections-config";
    }

    @PostMapping("/set-default/{id}")
    public String setAsDefault(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            DatabaseConnection connection = connectionService.setAsDefault(id);
            redirectAttributes.addFlashAttribute("success", 
                "Set as default: " + connection.getConnectionName());
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error setting default: " + e.getMessage());
        }
        
        return "redirect:/connections-config";
    }
}
