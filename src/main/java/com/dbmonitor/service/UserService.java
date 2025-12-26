package com.dbmonitor.service;

import com.dbmonitor.model.Role;
import com.dbmonitor.model.User;
import com.dbmonitor.repository.RoleRepository;
import com.dbmonitor.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
            .createdAt(LocalDateTime.now())
            .roles(roles)
            .build();

        User saved = userRepository.save(user);
        log.info("Created user: {}", username);
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

    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

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
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
        log.info("Deleted user with ID: {}", userId);
    }

    public Role createRole(String name, String description) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }

        Role role = Role.builder()
            .name(name)
            .description(description)
            .build();

        Role saved = roleRepository.save(role);
        log.info("Created role: {}", name);
        return saved;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
