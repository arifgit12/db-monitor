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
}
