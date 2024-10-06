package com.project.osh.service.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.project.osh.controller.DashboardController;
import com.project.osh.interfaces.InterfaceCore;
import com.project.osh.service.DashboardService;
import com.project.osh.util.JsonUtil;

@Service
public class DashboardServiceImpl implements DashboardService{

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private static JsonObject weatherJsonObject;
    private static JsonObject weatherJsonObject1;
    private static JsonObject weatherJsonObject2;
    private static JsonObject trafficJsonObject;
    private static JsonObject emergencyJsonObject;
    private static JsonObject yeonhapJsonObject;

    @Value("${osh.logging}")
    private boolean loggingFlag;

    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    @Override
    public JsonObject getDashboardJsonObject() {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getDashboardJsonObject", dateFormat.format(new Date()));
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("weatherJson1",getWeatherJsonObject1("35.221316","128.682037").toString());
        jsonObject.addProperty("weatherJson2",getWeatherJsonObject2("37.245807","127.057375").toString());
        jsonObject.addProperty("applicationJson",getApplicationJsonObject().toString());
        jsonObject.addProperty("trafficJson",getTrafficJsonObject().toString());
        jsonObject.addProperty("emergencyJson",getEmergencyJsonObject().toString());
        jsonObject.addProperty("yeonhapJson",getNewsYeonhapJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getWeatherJsonObject() {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getWeatherJsonObject", dateFormat.format(new Date()));
        }
        if(weatherJsonObject == null) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo());
        return weatherJsonObject;
    }

    @Override
    public JsonObject getWeatherJsonObject(String lat, String lon) {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getWeatherJsonObject(lat,lon)", dateFormat.format(new Date()));
        }
        if(weatherJsonObject == null) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject;
    }

    @Override
    public JsonObject getWeatherJsonObject1(String lat, String lon) {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getWeatherJsonObject1", dateFormat.format(new Date()));
        }
        if(weatherJsonObject1 == null) weatherJsonObject1 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject1;
    }

    @Override
    public JsonObject getWeatherJsonObject2(String lat, String lon) {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getWeatherJsonObject2", dateFormat.format(new Date()));
        }
        if(weatherJsonObject2 == null) weatherJsonObject2 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject2;
    }

    @Override
    public void renewWeatherJsonObject(){
        weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo());
    }

    @Override
    public void renewWeatherJsonObject(String lat, String lon){
        weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat,lon));
    }

    @Override
    public void renewWeatherJsonObject1(String lat, String lon){
        weatherJsonObject1 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat,lon));
    }

    @Override
    public void renewWeatherJsonObject2(String lat, String lon){
        weatherJsonObject2 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat,lon));
    }

    @Override
    public JsonObject getApplicationJsonObject() {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getApplicationJsonObject", dateFormat.format(new Date()));
        }
        JsonObject jsonObject = new JsonObject();
        SimpleDateFormat seoulSdf = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" ) ;
        java.util.TimeZone seoul = java.util.TimeZone.getTimeZone ( "Asia/Seoul" );
        seoulSdf.setTimeZone ( seoul ) ;
        jsonObject.addProperty("currentTime", seoulSdf.format(new Timestamp(System.currentTimeMillis())));
        jsonObject.addProperty("systemArchitecture", osBean.getArch().toString());
        jsonObject.addProperty("systemName",osBean.getName().toString());
        jsonObject.addProperty("systemVersion",osBean.getVersion().toString());
        jsonObject.addProperty("systemLoadAverage",osBean.getSystemLoadAverage());
        jsonObject.addProperty("memory", (Runtime.getRuntime().totalMemory()/(1024*1024)) + "MB");
        jsonObject.addProperty("useMemory", ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(1024*1024)) + "MB");
        jsonObject.addProperty("freeMemory", (Runtime.getRuntime().freeMemory()/(1024*1024)) + "MB");
        jsonObject.addProperty("availableProcessors", (Runtime.getRuntime().availableProcessors()));
        return jsonObject;
    }

    @Override
    public JsonObject getTrafficWrapperJson(){
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getTrafficWrapperJson", dateFormat.format(new Date()));
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("trafficJson",getTrafficJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getTrafficJsonObject() {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getTrafficJsonObject", dateFormat.format(new Date()));
        }
        if(trafficJsonObject == null) trafficJsonObject = new JsonUtil().getJson(new InterfaceCore().getTrafficInfo());
        return trafficJsonObject;
    }

    @Override
    public void renewTrafficJsonObject(){
        trafficJsonObject = new JsonUtil().getJson(new InterfaceCore().getTrafficInfo());
    }

    @Override
    public JsonObject getEmergencyWrapperJson(){
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getEmergencyWrapperJson", dateFormat.format(new Date()));
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("emergencyJson",getEmergencyJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getEmergencyJsonObject() {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getEmergencyJsonObject", dateFormat.format(new Date()));
        }
        if(emergencyJsonObject == null) emergencyJsonObject = new JsonUtil().getJson(new InterfaceCore().getEmergencyInfo());
        return emergencyJsonObject;
    }

    @Override
    public void renewEmergencyJsonObject(){
        emergencyJsonObject = new JsonUtil().getJson(new InterfaceCore().getEmergencyInfo());
    }

    @Override
    public JsonObject getYeonhapWrapperJson(){
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getYeonhapWrapperJson", dateFormat.format(new Date()));
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("yeonhapJson",getNewsYeonhapJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getNewsYeonhapJsonObject() {
        if(loggingFlag){
            log.info("{} >> DashboardServiceImpl.getNewsYeonhapJsonObject", dateFormat.format(new Date()));
        }
        if(yeonhapJsonObject == null) yeonhapJsonObject = new JsonUtil().getJson(new InterfaceCore().getYeonhapInfo());
        return yeonhapJsonObject;
    }

    @Override
    public void renewNewsYeonhapJsonObject(){
        yeonhapJsonObject = new JsonUtil().getJson(new InterfaceCore().getYeonhapInfo());
    }
    
}
