package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "database_connections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConnection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String connectionName;
    
    @Column(nullable = false)
    private String databaseType; // MYSQL, POSTGRESQL, H2, ORACLE, SQLSERVER
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(nullable = false)
    private String databaseName;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;

    /* --- New TLS fields (all optional) --- */

    // Dev helper (true = use encrypt=true;trustServerCertificate=true)
    @Column(name = "skip_tls_validation")
    private Boolean skipTlsValidation;

    // For proper validation (when skipTlsValidation = false)
    @Column(name = "trust_store_path")
    private String trustStorePath;

    @Column(name = "trust_store_password")
    private String trustStorePassword;

    // Optional override when connecting by IP or CN/SAN mismatch
    @Column(name = "host_name_in_certificate")
    private String hostNameInCertificate;
    
    private String description;
    
    private Boolean isActive;
    
    private Boolean isDefault;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime lastTestedAt;
    
    private String lastTestStatus;
    
    // Connection pool settings
    private Integer minPoolSize;
    private Integer maxPoolSize;
    private Integer connectionTimeout;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isDefault == null) isDefault = false;
        if (minPoolSize == null) minPoolSize = 5;
        if (maxPoolSize == null) maxPoolSize = 20;
        if (connectionTimeout == null) connectionTimeout = 30000;
    }
    
    public String getJdbcUrl() {
        switch (databaseType.toUpperCase()) {
            case "MYSQL":
                return String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
            case "POSTGRESQL":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "H2":
                return String.format("jdbc:h2:mem:%s", databaseName);
            case "ORACLE":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            case "SQLSERVER":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
    }
    
    public String getDriverClassName() {
        switch (databaseType.toUpperCase()) {
            case "MYSQL":
                return "com.mysql.cj.jdbc.Driver";
            case "POSTGRESQL":
                return "org.postgresql.Driver";
            case "H2":
                return "org.h2.Driver";
            case "ORACLE":
                return "oracle.jdbc.OracleDriver";
            case "SQLSERVER":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
    }
}
