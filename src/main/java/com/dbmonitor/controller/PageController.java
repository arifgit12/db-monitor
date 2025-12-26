package com.dbmonitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/users")
    public String users() {
        return "users";
    }

    @GetMapping("/roles")
    public String roles() {
        return "roles";
    }

    @GetMapping("/audit-logs")
    public String auditLogs() {
        return "audit-logs";
    }

    @GetMapping("/security")
    public String security() {
        return "security";
    }
}
