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

    // Optional: Custom JDBC URL (if provided, overrides auto-generated URL)
    @Column(name = "custom_jdbc_url", length = 500)
    private String customJdbcUrl;

    // Optional: Custom driver class name (if provided, overrides auto-detected driver)
    @Column(name = "custom_driver_class")
    private String customDriverClassName;

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
        // If custom JDBC URL is provided, use it
        if (customJdbcUrl != null && !customJdbcUrl.trim().isEmpty()) {
            return customJdbcUrl;
        }

        // Otherwise, construct URL based on database type
        String dbType = databaseType.toUpperCase().replace(" ", "");
        switch (dbType) {
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
                // If database type is unknown but we have host/port/database, try generic format
                if (host != null && port != null && databaseName != null) {
                    return String.format("jdbc:%s://%s:%d/%s", databaseType.toLowerCase(), host, port, databaseName);
                }
                throw new IllegalArgumentException("Unsupported database type: " + databaseType +
                    ". Please provide a custom JDBC URL.");
        }
    }

    public String getDriverClassName() {
        // If custom driver class name is provided, use it
        if (customDriverClassName != null && !customDriverClassName.trim().isEmpty()) {
            return customDriverClassName;
        }

        // Otherwise, detect driver based on database type
        String dbType = databaseType.toUpperCase().replace(" ", "");
        switch (dbType) {
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
                throw new IllegalArgumentException("Unsupported database type: " + databaseType +
                    ". Please provide a custom driver class name.");
        }
    }
}
