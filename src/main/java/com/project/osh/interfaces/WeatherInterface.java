package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.osh.controller.DashboardController;
import com.project.osh.util.HttpUtil;

public class WeatherInterface{

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    
    public String getOpenweathermap(){

		log.info("{} >> WeatherInterface.getOpenweathermap", dateFormat.format(new Date()));
		
        String strWeatherInfo = "";
		try {
	        strWeatherInfo = new HttpUtil().executeGet("https://api.openweathermap.org/data/2.5/weather?lat=37.245807&lon=127.057375&appid=e9ba762681ab8a0aa1e50fe52895b0eb");
		}catch(Exception e) {
			e.printStackTrace();
		}
        return strWeatherInfo;
    }

	public String getOpenweathermap(String lat, String lon){

		log.info("{} >> WeatherInterface.getOpenweathermap", dateFormat.format(new Date()));
		
        String strWeatherInfo = "";
		try {
	        strWeatherInfo = new HttpUtil().executeGet("https://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&appid=e9ba762681ab8a0aa1e50fe52895b0eb");
		}catch(Exception e) {
			e.printStackTrace();
		}
        return strWeatherInfo;
    }
    
}