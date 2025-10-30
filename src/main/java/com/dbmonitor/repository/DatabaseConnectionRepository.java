package com.dbmonitor.repository;

import com.dbmonitor.model.DatabaseConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Long> {
    
    Optional<DatabaseConnection> findByConnectionName(String connectionName);
    
    List<DatabaseConnection> findByIsActiveTrue();
    
    Optional<DatabaseConnection> findByIsDefaultTrue();
    
    boolean existsByConnectionName(String connectionName);
}
