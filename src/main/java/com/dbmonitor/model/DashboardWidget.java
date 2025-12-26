package com.dbmonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dashboard_widgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String widgetType; // CPU, MEMORY, CONNECTIONS, QUERIES, ALERTS, CUSTOM
    
    private String title;
    
    private Integer positionX;
    
    private Integer positionY;
    
    private Integer width;
    
    private Integer height;
    
    private String configuration; // JSON configuration for widget settings
    
    private Boolean visible;
    
    private Integer sortOrder;
}
