package com.project.osh.service.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private JsonObject weatherJsonObject;
    private JsonObject weatherJsonObject1;
    private JsonObject weatherJsonObject2;
    private JsonObject trafficJsonObject;
    private JsonObject emergencyJsonObject;
    private JsonObject yeonhapJsonObject;

    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    @Override
    public JsonObject getDashboardJsonObject() {
        log.info("{} >> DashboardServiceImpl.getDashboardJsonObject", dateFormat.format(new Date()));
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
        log.info("{} >> DashboardServiceImpl.getWeatherJsonObject", dateFormat.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        int second = calendar.get(Calendar.SECOND);
        if(weatherJsonObject == null) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo());
        if(second == 0) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo());
        return weatherJsonObject;
    }

    @Override
    public JsonObject getWeatherJsonObject1(String lat, String lon) {
        log.info("{} >> DashboardServiceImpl.getWeatherJsonObject1", dateFormat.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        int second = calendar.get(Calendar.SECOND);
        if(weatherJsonObject1 == null) weatherJsonObject1 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        if(second == 0) weatherJsonObject1 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject1;
    }

    @Override
    public JsonObject getWeatherJsonObject2(String lat, String lon) {
        log.info("{} >> DashboardServiceImpl.getWeatherJsonObject2", dateFormat.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        int second = calendar.get(Calendar.SECOND);
        if(weatherJsonObject2 == null) weatherJsonObject2 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        if(second == 0) weatherJsonObject2 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject2;
    }

    @Override
    public JsonObject getApplicationJsonObject() {
        log.info("{} >> DashboardServiceImpl.getApplicationJsonObject", dateFormat.format(new Date()));
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
        log.info("{} >> DashboardServiceImpl.getTrafficWrapperJson", dateFormat.format(new Date()));
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("trafficJson",getTrafficJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getTrafficJsonObject() {
        log.info("{} >> DashboardServiceImpl.getTrafficJsonObject", dateFormat.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        int second = calendar.get(Calendar.SECOND);
        if(trafficJsonObject == null) trafficJsonObject = new JsonUtil().getJson(new InterfaceCore().getTrafficInfo());
        if(second == 0) trafficJsonObject = new JsonUtil().getJson(new InterfaceCore().getTrafficInfo());
        return trafficJsonObject;
    }

    @Override
    public JsonObject getEmergencyWrapperJson(){
        log.info("{} >> DashboardServiceImpl.getEmergencyWrapperJson", dateFormat.format(new Date()));
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("emergencyJson",getEmergencyJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getEmergencyJsonObject() {
        log.info("{} >> DashboardServiceImpl.getEmergencyJsonObject", dateFormat.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        int second = calendar.get(Calendar.SECOND);
        if(emergencyJsonObject == null) emergencyJsonObject = new JsonUtil().getJson(new InterfaceCore().getEmergencyInfo());
        if(second == 0) emergencyJsonObject = new JsonUtil().getJson(new InterfaceCore().getEmergencyInfo());
        return emergencyJsonObject;
    }

    @Override
    public JsonObject getYeonhapWrapperJson(){
        log.info("{} >> DashboardServiceImpl.getYeonhapWrapperJson", dateFormat.format(new Date()));
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("yeonhapJson",getNewsYeonhapJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getNewsYeonhapJsonObject() {
        log.info("{} >> DashboardServiceImpl.getNewsYeonhapJsonObject", dateFormat.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        int second = calendar.get(Calendar.SECOND);
        if(yeonhapJsonObject == null) yeonhapJsonObject = new JsonUtil().getJson(new InterfaceCore().getYeonhapInfo());
        if(second == 0) yeonhapJsonObject = new JsonUtil().getJson(new InterfaceCore().getYeonhapInfo());
        return yeonhapJsonObject;
    }

    @Override
    public JsonObject getWeatherJsonObject(String lat, String lon) {
        log.info("{} >> DashboardServiceImpl.getWeatherJsonObject(lat,lon)", dateFormat.format(new Date()));
        Calendar calendar = new GregorianCalendar();
        int second = calendar.get(Calendar.SECOND);
        if(weatherJsonObject == null) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        if(second == 0) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject;
    }
    
}
