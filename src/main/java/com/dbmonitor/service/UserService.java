package com.dbmonitor.service;

import com.dbmonitor.model.Role;
import com.dbmonitor.model.User;
import com.dbmonitor.repository.RoleRepository;
import com.dbmonitor.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Autowired(required = false)
    private SecurityMonitoringService securityMonitoringService;

    @Value("${security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Transactional
    public User createUser(String username, String password, String email, String phone, Set<String> roleNames) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            roles.add(role);
        }

        User user = User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .email(email)
            .phone(phone)
            .enabled(true)
            .accountLocked(false)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .roles(roles)
            .build();

        User saved = userRepository.save(user);
        log.info("Created user: {}", username);
        
        if (auditLogService != null) {
            auditLogService.logAction(null, "system", "CREATE_USER", 
                "User created: " + username, null, null, "SUCCESS", null);
        }
        
        return saved;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void updateLastLogin(String username, String ipAddress) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            user.setFailedLoginAttempts(0); // Reset failed attempts on successful login
            if (user.getAccountLocked()) {
                user.setAccountLocked(false);
                user.setLockoutTime(null);
            }
            userRepository.save(user);
        });
    }

    @Transactional
    public void recordFailedLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            
            if (securityMonitoringService != null && securityMonitoringService.shouldLockAccount(username)) {
                user.setAccountLocked(true);
                user.setLockoutTime(LocalDateTime.now());
                log.warn("Account locked due to multiple failed login attempts: {}", username);
            }
            
            userRepository.save(user);
        });
    }

    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
            .map(user -> {
                if (user.getAccountLocked() && user.getLockoutTime() != null) {
                    LocalDateTime unlockTime = user.getLockoutTime().plusMinutes(lockoutDurationMinutes);
                    if (LocalDateTime.now().isAfter(unlockTime)) {
                        // Auto-unlock if lockout period has passed
                        unlockAccount(username);
                        return false;
                    }
                    return true;
                }
                return false;
            })
            .orElse(false);
    }

    @Transactional
    public void unlockAccount(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setAccountLocked(false);
            user.setLockoutTime(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            log.info("Account unlocked: {}", username);
            
            if (auditLogService != null) {
                auditLogService.logAction(user.getId(), username, "UNLOCK_ACCOUNT", 
                    "Account unlocked", null, null, "SUCCESS", null);
            }
        });
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
        
        if (auditLogService != null) {
            auditLogService.logAction(userId, user.getUsername(), "CHANGE_PASSWORD", 
                "Password changed", null, null, "SUCCESS", null);
        }
    }

    @Transactional
    public void updateUser(Long userId, String email, String phone, Boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        
        userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());
        
        if (auditLogService != null) {
            auditLogService.logAction(userId, user.getUsername(), "UPDATE_USER", 
                "User updated", null, null, "SUCCESS", null);
        }
    }

    @Transactional
    public void updateUserRoles(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            roles.add(role);
        }
        
        user.setRoles(roles);
        userRepository.save(user);
        log.info("Updated roles for user: {}", user.getUsername());
        
        if (auditLogService != null) {
            auditLogService.logAction(userId, user.getUsername(), "UPDATE_USER_ROLES", 
                "User roles updated", null, null, "SUCCESS", null);
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String username = user.getUsername();
        userRepository.deleteById(userId);
        log.info("Deleted user: {}", username);
        
        if (auditLogService != null) {
            auditLogService.logAction(userId, username, "DELETE_USER", 
                "User deleted: " + username, null, null, "SUCCESS", null);
        }
    }

    public Role createRole(String name, String description) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }

        Role role = Role.builder()
            .name(name)
            .description(description)
            .privileges(new HashSet<>())
            .build();

        Role saved = roleRepository.save(role);
        log.info("Created role: {}", name);
        
        if (auditLogService != null) {
            auditLogService.logAction(null, "system", "CREATE_ROLE", 
                "Role created: " + name, null, null, "SUCCESS", null);
        }
        
        return saved;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
