package com.dbmonitor.controller;

import com.dbmonitor.model.Role;
import com.dbmonitor.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@Slf4j
public class RoleApiController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRole(@PathVariable Long id) {
        return roleService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Role> getRoleByName(@PathVariable String name) {
        return roleService.findByName(name)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            
            Role role = roleService.createRole(name, description);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Error creating role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String description = request.get("description");
            
            Role role = roleService.updateRole(id, description);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Error updating role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roleId}/privileges/{privilegeId}")
    public ResponseEntity<?> addPrivilegeToRole(
            @PathVariable Long roleId,
            @PathVariable Long privilegeId) {
        try {
            Role role = roleService.addPrivilegeToRole(roleId, privilegeId);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Error adding privilege to role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{roleId}/privileges/{privilegeId}")
    public ResponseEntity<?> removePrivilegeFromRole(
            @PathVariable Long roleId,
            @PathVariable Long privilegeId) {
        try {
            Role role = roleService.removePrivilegeFromRole(roleId, privilegeId);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Error removing privilege from role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{roleId}/privileges")
    public ResponseEntity<?> setRolePrivileges(
            @PathVariable Long roleId,
            @RequestBody Map<String, Set<Long>> request) {
        try {
            Set<Long> privilegeIds = request.get("privilegeIds");
            Role role = roleService.setPrivileges(roleId, privilegeIds);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Error setting role privileges", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
