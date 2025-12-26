package com.dbmonitor.service;

import com.dbmonitor.model.Privilege;
import com.dbmonitor.repository.PrivilegeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PrivilegeService {

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Transactional
    public Privilege createPrivilege(String name, String description, String category) {
        if (privilegeRepository.existsByName(name)) {
            throw new IllegalArgumentException("Privilege already exists: " + name);
        }

        Privilege privilege = Privilege.builder()
            .name(name)
            .description(description)
            .category(category)
            .build();

        Privilege saved = privilegeRepository.save(privilege);
        log.info("Created privilege: {}", name);
        return saved;
    }

    public Optional<Privilege> findByName(String name) {
        return privilegeRepository.findByName(name);
    }

    public Optional<Privilege> findById(Long id) {
        return privilegeRepository.findById(id);
    }

    public List<Privilege> getAllPrivileges() {
        return privilegeRepository.findAll();
    }

    @Transactional
    public Privilege updatePrivilege(Long id, String description, String category) {
        Privilege privilege = privilegeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Privilege not found"));
        
        if (description != null) {
            privilege.setDescription(description);
        }
        if (category != null) {
            privilege.setCategory(category);
        }
        
        Privilege updated = privilegeRepository.save(privilege);
        log.info("Updated privilege: {}", privilege.getName());
        return updated;
    }

    @Transactional
    public void deletePrivilege(Long id) {
        privilegeRepository.deleteById(id);
        log.info("Deleted privilege with ID: {}", id);
    }
}
