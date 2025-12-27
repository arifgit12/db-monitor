package com.dbmonitor.controller;

import com.dbmonitor.model.Role;
import com.dbmonitor.model.User;
import com.dbmonitor.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserApiController {

    @Autowired
    private UserService userService;
    
    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String email;
        private String phone;
        private List<String> roles;
    }
    
    @Data
    public static class UpdateUserRolesRequest {
        private List<String> roles;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            Set<String> roleNames = request.getRoles() != null 
                ? new HashSet<>(request.getRoles()) 
                : new HashSet<>();
            
            User user = userService.createUser(
                request.getUsername(), 
                request.getPassword(), 
                request.getEmail(), 
                request.getPhone(), 
                roleNames
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String phone = (String) request.get("phone");
            Boolean enabled = (Boolean) request.get("enabled");
            
            userService.updateUser(id, email, phone, enabled);
            return ResponseEntity.ok(Map.of("message", "User updated successfully"));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<?> updateUserRoles(
            @PathVariable Long id,
            @RequestBody UpdateUserRolesRequest request) {
        try {
            Set<String> roleNames = request.getRoles() != null 
                ? new HashSet<>(request.getRoles()) 
                : new HashSet<>();
            userService.updateUserRoles(id, roleNames);
            return ResponseEntity.ok(Map.of("message", "User roles updated successfully"));
        } catch (Exception e) {
            log.error("Error updating user roles", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            userService.changePassword(id, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlockAccount(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            userService.unlockAccount(user.getUsername());
            return ResponseEntity.ok(Map.of("message", "Account unlocked successfully"));
        } catch (Exception e) {
            log.error("Error unlocking account", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/locked")
    public ResponseEntity<Map<String, Boolean>> isAccountLocked(@PathVariable Long id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            boolean locked = userService.isAccountLocked(user.getUsername());
            return ResponseEntity.ok(Map.of("locked", locked));
        } catch (Exception e) {
            log.error("Error checking account lock status", e);
            return ResponseEntity.badRequest().body(Map.of("error", true));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(userService.getAllRoles());
    }

    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            Role role = userService.createRole(name, description);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Error creating role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
