package com.dbmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DatabaseMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseMonitorApplication.class, args);
    }
}
