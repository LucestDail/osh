package com.project.osh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import java.util.Arrays;
import java.util.List;

@Configuration
@PropertySource("classpath:weather.properties")
public class WeatherConfig {
    
    @Value("${weather.locations}")
    private String locations;
    
    @Value("${weather.codes}")
    private String codes;
    
    public List<String> getLocations() {
        return Arrays.asList(locations.split(","));
    }
    
    public List<String> getCodes() {
        return Arrays.asList(codes.split(","));
    }
} 