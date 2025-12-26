package com.dbmonitor.config;

import com.dbmonitor.model.Privilege;
import com.dbmonitor.model.Role;
import com.dbmonitor.model.User;
import com.dbmonitor.repository.PrivilegeRepository;
import com.dbmonitor.repository.RoleRepository;
import com.dbmonitor.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(1)
@Slf4j
public class SecurityDataInitializer implements CommandLineRunner {

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Initializing security data...");
        
        // Create privileges if they don't exist
        createPrivilegeIfNotExists("VIEW_DASHBOARD", "View dashboard", "VIEW");
        createPrivilegeIfNotExists("VIEW_METRICS", "View metrics", "VIEW");
        createPrivilegeIfNotExists("VIEW_QUERIES", "View queries", "VIEW");
        createPrivilegeIfNotExists("VIEW_CONNECTIONS", "View connections", "VIEW");
        createPrivilegeIfNotExists("VIEW_ALERTS", "View alerts", "VIEW");
        createPrivilegeIfNotExists("VIEW_PERFORMANCE", "View performance", "VIEW");
        createPrivilegeIfNotExists("VIEW_REPORTS", "View reports", "VIEW");
        createPrivilegeIfNotExists("VIEW_AUDIT_LOGS", "View audit logs", "VIEW");
        
        createPrivilegeIfNotExists("MANAGE_CONNECTIONS", "Manage database connections", "MANAGE");
        createPrivilegeIfNotExists("MANAGE_ALERTS", "Manage alerts", "MANAGE");
        createPrivilegeIfNotExists("MANAGE_QUERIES", "Manage queries", "MANAGE");
        createPrivilegeIfNotExists("MANAGE_REPORTS", "Manage reports", "MANAGE");
        createPrivilegeIfNotExists("MANAGE_NOTIFICATIONS", "Manage notifications", "MANAGE");
        
        createPrivilegeIfNotExists("ADMIN_USERS", "Manage users", "ADMIN");
        createPrivilegeIfNotExists("ADMIN_ROLES", "Manage roles", "ADMIN");
        createPrivilegeIfNotExists("ADMIN_PRIVILEGES", "Manage privileges", "ADMIN");
        createPrivilegeIfNotExists("ADMIN_SYSTEM", "System administration", "ADMIN");
        createPrivilegeIfNotExists("ADMIN_SECURITY", "Security administration", "ADMIN");
        
        // Create super-admin role with all privileges
        Role superAdminRole = createRoleIfNotExists("ROLE_SUPER_ADMIN", "Super Administrator with all privileges");
        Set<Privilege> allPrivileges = new HashSet<>(privilegeRepository.findAll());
        superAdminRole.setPrivileges(allPrivileges);
        roleRepository.save(superAdminRole);
        
        // Create admin role with most privileges
        Role adminRole = createRoleIfNotExists("ROLE_ADMIN", "Administrator");
        Set<Privilege> adminPrivileges = new HashSet<>();
        adminPrivileges.addAll(getPrivilegesByCategory("VIEW"));
        adminPrivileges.addAll(getPrivilegesByCategory("MANAGE"));
        adminRole.setPrivileges(adminPrivileges);
        roleRepository.save(adminRole);
        
        // Create user role with view privileges only
        Role userRole = createRoleIfNotExists("ROLE_USER", "Regular User");
        Set<Privilege> userPrivileges = new HashSet<>(getPrivilegesByCategory("VIEW"));
        userRole.setPrivileges(userPrivileges);
        roleRepository.save(userRole);
        
        // Create default admin user
        if (!userRepository.existsByUsername("admin")) {
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(superAdminRole);
            
            User adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@dbmonitor.com")
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .roles(adminRoles)
                .build();
            
            userRepository.save(adminUser);
            log.info("Created default admin user (username: admin, password: admin123)");
            log.warn("SECURITY WARNING: Please change the default admin password immediately!");
        }
        
        log.info("Security data initialization complete");
    }

    private Privilege createPrivilegeIfNotExists(String name, String description, String category) {
        return privilegeRepository.findByName(name).orElseGet(() -> {
            Privilege privilege = Privilege.builder()
                .name(name)
                .description(description)
                .category(category)
                .build();
            Privilege saved = privilegeRepository.save(privilege);
            log.debug("Created privilege: {}", name);
            return saved;
        });
    }

    private Role createRoleIfNotExists(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder()
                .name(name)
                .description(description)
                .privileges(new HashSet<>())
                .build();
            Role saved = roleRepository.save(role);
            log.debug("Created role: {}", name);
            return saved;
        });
    }

    private Set<Privilege> getPrivilegesByCategory(String category) {
        return new HashSet<>(privilegeRepository.findAll().stream()
            .filter(p -> category.equals(p.getCategory()))
            .toList());
    }
}
