package com.dbmonitor.controller;

import com.dbmonitor.model.Privilege;
import com.dbmonitor.service.PrivilegeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/privileges")
@Slf4j
public class PrivilegeApiController {

    @Autowired
    private PrivilegeService privilegeService;

    @GetMapping
    public ResponseEntity<List<Privilege>> getAllPrivileges() {
        return ResponseEntity.ok(privilegeService.getAllPrivileges());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Privilege> getPrivilege(@PathVariable Long id) {
        return privilegeService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Privilege> getPrivilegeByName(@PathVariable String name) {
        return privilegeService.findByName(name)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createPrivilege(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String category = request.get("category");
            
            Privilege privilege = privilegeService.createPrivilege(name, description, category);
            return ResponseEntity.ok(privilege);
        } catch (Exception e) {
            log.error("Error creating privilege", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePrivilege(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String description = request.get("description");
            String category = request.get("category");
            
            Privilege privilege = privilegeService.updatePrivilege(id, description, category);
            return ResponseEntity.ok(privilege);
        } catch (Exception e) {
            log.error("Error updating privilege", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrivilege(@PathVariable Long id) {
        try {
            privilegeService.deletePrivilege(id);
            return ResponseEntity.ok(Map.of("message", "Privilege deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting privilege", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
