package com.project.osh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:dashboard.properties")
public class DashboardConfig {
    
    @Value("${dashboard.emergency.count}")
    private int emergencyCount;
    
    @Value("${dashboard.traffic.count}")
    private int trafficCount;
    
    public int getEmergencyCount() {
        return emergencyCount;
    }
    
    public int getTrafficCount() {
        return trafficCount;
    }
} 