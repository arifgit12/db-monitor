package com.dbmonitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("activePage", "user-users");
        return "users";
    }

    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("activePage", "user-roles");
        return "roles";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(Model model) {
        model.addAttribute("activePage", "security-audit");
        return "audit-logs";
    }

    @GetMapping("/security")
    public String security(Model model) {
        model.addAttribute("activePage", "security-monitor");
        return "security";
    }
    
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }
    
    @GetMapping("/connections")
    public String connections(Model model) {
        model.addAttribute("activePage", "db-connections");
        return "connections";
    }
    
    @GetMapping("/queries")
    public String queries(Model model) {
        model.addAttribute("activePage", "db-queries");
        return "queries";
    }
    
    @GetMapping("/performance")
    public String performance(Model model) {
        model.addAttribute("activePage", "db-performance");
        return "performance";
    }
    
    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("activePage", "db-alerts");
        return "alerts";
    }
}
