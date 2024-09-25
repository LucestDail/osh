package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.osh.controller.DashboardController;

public class InterfaceCore {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	
	public String getWeatherInfo() {
		log.info("{} >> InterfaceCore.getWeatherInfo", dateFormat.format(new Date()));
        return new WeatherInterface().getOpenweathermap();
	}

	public String getWeatherInfo(String lat, String lon) {
		log.info("{} >> InterfaceCore.getWeatherInfo", dateFormat.format(new Date()));
        return new WeatherInterface().getOpenweathermap(lat, lon);
	}
	
	public String getEmergencyInfo() {
		log.info("{} >> InterfaceCore.getEmergencyInfo", dateFormat.format(new Date()));
        return new EmergencyInterface().getEmergencyInfo();
	}
	
	public String getTrafficInfo() {
		log.info("{} >> InterfaceCore.getTrafficInfo", dateFormat.format(new Date()));
        return new TrafficInterface().getTrafficInfo();
	}

}
