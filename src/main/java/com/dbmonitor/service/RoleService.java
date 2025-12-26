package com.dbmonitor.service;

import com.dbmonitor.model.Privilege;
import com.dbmonitor.model.Role;
import com.dbmonitor.repository.PrivilegeRepository;
import com.dbmonitor.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Transactional
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
        return saved;
    }

    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public Role updateRole(Long id, String description) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        if (description != null) {
            role.setDescription(description);
        }
        
        Role updated = roleRepository.save(role);
        log.info("Updated role: {}", role.getName());
        return updated;
    }

    @Transactional
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
        log.info("Deleted role with ID: {}", id);
    }

    @Transactional
    public Role addPrivilegeToRole(Long roleId, Long privilegeId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        Privilege privilege = privilegeRepository.findById(privilegeId)
            .orElseThrow(() -> new IllegalArgumentException("Privilege not found"));
        
        role.getPrivileges().add(privilege);
        Role updated = roleRepository.save(role);
        log.info("Added privilege {} to role {}", privilege.getName(), role.getName());
        return updated;
    }

    @Transactional
    public Role removePrivilegeFromRole(Long roleId, Long privilegeId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        role.getPrivileges().removeIf(p -> p.getId().equals(privilegeId));
        Role updated = roleRepository.save(role);
        log.info("Removed privilege {} from role {}", privilegeId, role.getName());
        return updated;
    }

    @Transactional
    public Role setPrivileges(Long roleId, Set<Long> privilegeIds) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Set<Privilege> privileges = new HashSet<>();
        for (Long privilegeId : privilegeIds) {
            Privilege privilege = privilegeRepository.findById(privilegeId)
                .orElseThrow(() -> new IllegalArgumentException("Privilege not found: " + privilegeId));
            privileges.add(privilege);
        }
        
        role.setPrivileges(privileges);
        Role updated = roleRepository.save(role);
        log.info("Set {} privileges for role {}", privileges.size(), role.getName());
        return updated;
    }
}
