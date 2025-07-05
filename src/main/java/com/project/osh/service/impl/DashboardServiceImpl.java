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
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;
import com.project.osh.controller.DashboardController;
import com.project.osh.interfaces.InterfaceCore;
import com.project.osh.service.DashboardService;
import com.project.osh.service.NewsService;
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
    private static JsonObject applicationJsonObject;
    
    private static long lastWeatherUpdate = 0;
    private static long lastTrafficUpdate = 0;
    private static long lastEmergencyUpdate = 0;
    private static long lastNewsUpdate = 0;
    
    private static final long WEATHER_UPDATE_INTERVAL = 3600000; // 1시간
    private static final long TRAFFIC_UPDATE_INTERVAL = 300000;  // 5분
    private static final long EMERGENCY_UPDATE_INTERVAL = 300000; // 5분
    private static final long NEWS_UPDATE_INTERVAL = 300000;     // 5분

    @Value("${osh.logging}")
    private boolean loggingFlag;

    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    @Autowired
    private NewsService newsService;

    // 생성자에서 초기 데이터 로드
    public DashboardServiceImpl() {
        // 초기 날씨 데이터 로드
        updateWeatherData();
        lastWeatherUpdate = System.currentTimeMillis();
        
        // 초기 교통 데이터 로드
        trafficJsonObject = getTrafficJsonObject();
        lastTrafficUpdate = System.currentTimeMillis();
        
        // 초기 재난 데이터 로드
        emergencyJsonObject = getEmergencyJsonObject();
        lastEmergencyUpdate = System.currentTimeMillis();
        
        // 초기 뉴스 데이터 로드
        yeonhapJsonObject = getNewsYeonhapJsonObject();
        lastNewsUpdate = System.currentTimeMillis();
    }

    @Override
    public JsonObject getDashboardJsonObject() {
        long currentTime = System.currentTimeMillis();
        JsonObject jsonObject = new JsonObject();
        
        try {
            // 날씨 정보 (1시간마다 갱신)
            if (currentTime - lastWeatherUpdate >= WEATHER_UPDATE_INTERVAL) {
                updateWeatherData();
                lastWeatherUpdate = currentTime;
            }
            if (weatherJsonObject != null) {
                jsonObject.addProperty("weatherJson", weatherJsonObject.toString());
            } else {
                jsonObject.addProperty("weatherJson", "{}");
            }
            
            // 교통 정보 (5분마다 갱신)
            if (currentTime - lastTrafficUpdate >= TRAFFIC_UPDATE_INTERVAL) {
                trafficJsonObject = getTrafficJsonObject();
                lastTrafficUpdate = currentTime;
            }
            if (trafficJsonObject != null) {
                jsonObject.addProperty("trafficJson", trafficJsonObject.toString());
            } else {
                jsonObject.addProperty("trafficJson", "{}");
            }
            
            // 재난 정보 (5분마다 갱신)
            if (currentTime - lastEmergencyUpdate >= EMERGENCY_UPDATE_INTERVAL) {
                emergencyJsonObject = getEmergencyJsonObject();
                lastEmergencyUpdate = currentTime;
            }
            if (emergencyJsonObject != null) {
                jsonObject.addProperty("emergencyJson", emergencyJsonObject.toString());
            } else {
                jsonObject.addProperty("emergencyJson", "{}");
            }
            
            // 뉴스 정보 (5분마다 갱신)
            if (currentTime - lastNewsUpdate >= NEWS_UPDATE_INTERVAL) {
                yeonhapJsonObject = getNewsYeonhapJsonObject();
                lastNewsUpdate = currentTime;
            }
            if (yeonhapJsonObject != null) {
                jsonObject.addProperty("yeonhapJson", yeonhapJsonObject.toString());
            } else {
                jsonObject.addProperty("yeonhapJson", "{}");
            }
            
            // 서버 정보 (매번 갱신)
            applicationJsonObject = getApplicationJsonObject();
            if (applicationJsonObject != null) {
                jsonObject.addProperty("applicationJson", applicationJsonObject.toString());
            } else {
                jsonObject.addProperty("applicationJson", "{}");
            }
            
            return jsonObject;
        } catch (Exception e) {
            log.error("Error getting dashboard data: {}", e.getMessage());
            // 에러 발생 시에도 기본 구조의 JSON 반환
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("weatherJson", "{}");
            errorJson.addProperty("trafficJson", "{}");
            errorJson.addProperty("emergencyJson", "{}");
            errorJson.addProperty("yeonhapJson", "{}");
            errorJson.addProperty("applicationJson", "{}");
            return errorJson;
        }
    }

    private void updateWeatherData() {
        weatherJsonObject = new JsonObject();
        try {
            // 20개 도시의 위도/경도 정보
            String[][] cities = {
                {"서울", "37.5665", "126.9780"},  // 서울
                {"부산", "35.1796", "129.0756"},  // 부산
                {"인천", "37.4563", "126.7052"},  // 인천
                {"대구", "35.8687", "128.5990"},  // 대구
                {"대전", "36.3505", "127.3750"},  // 대전
                {"광주", "35.1600", "126.8514"},  // 광주
                {"수원", "37.2636", "127.0286"},  // 수원
                {"울산", "35.5384", "129.3114"},  // 울산
                {"고양", "37.6584", "126.8320"},  // 고양
                {"용인", "37.2411", "127.1776"},  // 용인
                {"포항", "36.0320", "129.3650"},  // 포항
                {"창원", "35.2273", "128.6817"},  // 창원
                {"김해", "35.2284", "128.8893"},  // 김해
                {"김천", "36.1398", "128.1136"},  // 김천
                {"제주", "33.4996", "126.5312"},  // 제주
                {"춘천", "37.8813", "127.7300"},  // 춘천
                {"원주", "37.3442", "127.9200"},  // 원주
                {"강릉", "37.7519", "128.8960"},  // 강릉
                {"속초", "38.2070", "128.5928"}   // 속초
            };

            for (int i = 0; i < cities.length; i++) {
                String cityName = cities[i][0];
                String lat = cities[i][1];
                String lon = cities[i][2];
                String weatherJson = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon)).toString();
                weatherJsonObject.addProperty("weatherJson" + (i + 1), weatherJson);
            }
        } catch (Exception e) {
            log.error("Error updating weather data: {}", e.getMessage());
        }
    }

    @Override
    public JsonObject getWeatherJsonObject() {
        if(weatherJsonObject == null) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo());
        return weatherJsonObject;
    }

    @Override
    public JsonObject getWeatherJsonObject(String lat, String lon) {
        if(weatherJsonObject == null) weatherJsonObject = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject;
    }

    @Override
    public JsonObject getWeatherJsonObject1(String lat, String lon) {
        if(weatherJsonObject1 == null) weatherJsonObject1 = new JsonUtil().getJson(new InterfaceCore().getWeatherInfo(lat, lon));
        return weatherJsonObject1;
    }

    @Override
    public JsonObject getWeatherJsonObject2(String lat, String lon) {
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
        JsonObject jsonObject = new JsonObject();
        SimpleDateFormat seoulSdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        java.util.TimeZone seoul = java.util.TimeZone.getTimeZone("Asia/Seoul");
        seoulSdf.setTimeZone(seoul);
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
        JsonObject jsonObject = new JsonObject();
        JsonObject trafficData = getTrafficJsonObject();
        if (trafficData != null) {
            jsonObject.addProperty("trafficJson", trafficData.toString());
        } else {
            jsonObject.addProperty("trafficJson", "{}");
        }
        return jsonObject;
    }

    @Override
    public JsonObject getTrafficJsonObject() {
        try {
            if(trafficJsonObject == null) {
                trafficJsonObject = new JsonUtil().getJson(new InterfaceCore().getTrafficInfo());
            }
            return trafficJsonObject;
        } catch (Exception e) {
            log.error("Error getting traffic data: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void renewTrafficJsonObject(){
        try {
            trafficJsonObject = new JsonUtil().getJson(new InterfaceCore().getTrafficInfo());
        } catch (Exception e) {
            log.error("Error renewing traffic data: {}", e.getMessage());
        }
    }

    @Override
    public JsonObject getEmergencyWrapperJson(){
        JsonObject jsonObject = new JsonObject();
        JsonObject emergencyData = getEmergencyJsonObject();
        if (emergencyData != null) {
            jsonObject.addProperty("emergencyJson", emergencyData.toString());
        } else {
            jsonObject.addProperty("emergencyJson", "{}");
        }
        return jsonObject;
    }

    @Override
    public JsonObject getEmergencyJsonObject() {
        try {
            if(emergencyJsonObject == null) {
                emergencyJsonObject = new JsonUtil().getJson(new InterfaceCore().getEmergencyInfo());
            }
            return emergencyJsonObject;
        } catch (Exception e) {
            log.error("Error getting emergency data: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void renewEmergencyJsonObject(){
        try {
            emergencyJsonObject = new JsonUtil().getJson(new InterfaceCore().getEmergencyInfo());
        } catch (Exception e) {
            log.error("Error renewing emergency data: {}", e.getMessage());
        }
    }

    @Override
    public JsonObject getYeonhapWrapperJson(){
        JsonObject jsonObject = new JsonObject();
        JsonObject yeonhapData = getNewsYeonhapJsonObject();
        if (yeonhapData != null) {
            jsonObject.addProperty("yeonhapJson", yeonhapData.toString());
        } else {
            jsonObject.addProperty("yeonhapJson", "{}");
        }
        return jsonObject;
    }

    @Override
    public JsonObject getNewsYeonhapJsonObject() {
        try {
            if(yeonhapJsonObject == null) {
                // NewsServiceImpl의 cachedNews 데이터 사용
                yeonhapJsonObject = new JsonObject();
                JsonObject dataObject = new JsonObject();
                dataObject.add("items", newsService.getCachedNews());
                yeonhapJsonObject.add("data", dataObject);
            }
            return yeonhapJsonObject;
        } catch (Exception e) {
            log.error("Error getting news data: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void renewNewsYeonhapJsonObject(){
        try {
            // NewsServiceImpl의 cachedNews 데이터로 갱신
            yeonhapJsonObject = new JsonObject();
            JsonObject dataObject = new JsonObject();
            dataObject.add("items", newsService.getCachedNews());
            yeonhapJsonObject.add("data", dataObject);
        } catch (Exception e) {
            log.error("Error renewing news data: {}", e.getMessage());
        }
    }
}
