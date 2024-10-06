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
		if(loggingFlag) {
			log.info("{} >> InterfaceCore.getWeatherInfo", dateFormat.format(new Date()));
		}
        return new WeatherInterface().getOpenweathermap();
	}

	public String getWeatherInfo(String lat, String lon) {
		if(loggingFlag) {
			log.info("{} >> InterfaceCore.getWeatherInfo", dateFormat.format(new Date()));
		}
        return new WeatherInterface().getOpenweathermap(lat, lon);
	}
	
	public String getEmergencyInfo() {
		if(loggingFlag) {
			log.info("{} >> InterfaceCore.getEmergencyInfo", dateFormat.format(new Date()));
		}
        return new EmergencyInterface().getEmergencyInfo();
	}
	
	public String getTrafficInfo() {
		if(loggingFlag) {
			log.info("{} >> InterfaceCore.getTrafficInfo", dateFormat.format(new Date()));
		}
        return new TrafficInterface().getTrafficInfo();
	}

	public String getYeonhapInfo(){
		if(loggingFlag) {
			log.info("{} >> InterfaceCore.getYeonhapInfo", dateFormat.format(new Date()));
		}
		return new NewsInterface().getYeonhapNews();
	}

}
