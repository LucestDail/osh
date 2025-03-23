package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.project.osh.controller.DashboardController;

public class InterfaceCore {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	
	@Value("${osh.logging}")
    private boolean loggingFlag;

	public String getWeatherInfo() {
        return new WeatherInterface().getOpenweathermap();
	}

	public String getWeatherInfo(String lat, String lon) {
        return new WeatherInterface().getOpenweathermap(lat, lon);
	}
	
	public String getEmergencyInfo() {
        return new EmergencyInterface().getEmergencyInfo();
	}
	
	public String getTrafficInfo() {
        return new TrafficInterface().getTrafficInfo();
	}

	public String getYeonhapInfo(){
        return new NewsInterface().getYeonhapNews();
	}

}
