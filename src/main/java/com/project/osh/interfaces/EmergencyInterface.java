package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.project.osh.controller.DashboardController;
import com.project.osh.util.HttpUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EmergencyInterface {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Value("${osh.logging}")
    private boolean loggingFlag;

    public String getTrafficInfo() {
        String strTrafficInfo = "";
        String strTrafficInfoYesterday = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            java.util.TimeZone seoul = java.util.TimeZone.getTimeZone("Asia/Seoul");
            sdf.setTimeZone(seoul);
            String strToday = sdf.format(System.currentTimeMillis());
            String strYesterday = sdf.format(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

            strTrafficInfo = new HttpUtil().executeGet("https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?serviceKey=7DCBUF3EBA0Y6WQ1&crtDt="+strToday);
            strTrafficInfoYesterday = new HttpUtil().executeGet("https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?serviceKey=7DCBUF3EBA0Y6WQ1&crtDt="+strYesterday);

            // 두 JSON 데이터를 하나로 합치기
            JsonObject combinedJson = new JsonObject();
            JsonArray trafficArray = new JsonArray();
            
            // 오늘 데이터 파싱 및 추가
            JsonObject todayJson = JsonParser.parseString(strTrafficInfo).getAsJsonObject();
            if (todayJson.has("items")) {
                JsonArray todayItems = todayJson.getAsJsonArray("items");
                for (JsonElement item : todayItems) {
                    trafficArray.add(item);
                }
            }
            
            // 어제 데이터 파싱 및 추가
            JsonObject yesterdayJson = JsonParser.parseString(strTrafficInfoYesterday).getAsJsonObject();
            if (yesterdayJson.has("items")) {
                JsonArray yesterdayItems = yesterdayJson.getAsJsonArray("items");
                for (JsonElement item : yesterdayItems) {
                    trafficArray.add(item);
                }
            }
            
            // 합쳐진 데이터를 새로운 JSON 객체에 추가
            combinedJson.add("items", trafficArray);
            
            return combinedJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return strTrafficInfo;
        }
    }

    public String getEmergencyInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        java.util.TimeZone seoul = java.util.TimeZone.getTimeZone("Asia/Seoul");
        sdf.setTimeZone(seoul);
        String strToday = sdf.format(System.currentTimeMillis());
        String strYesterday = sdf.format(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

        try {
            String strEmergencyInfo = new HttpUtil().executeGet("https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?serviceKey=7DCBUF3EBA0Y6WQ1&crtDt="+strToday);
            String strEmergencyInfoYesterday = new HttpUtil().executeGet("https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?serviceKey=7DCBUF3EBA0Y6WQ1&crtDt="+strYesterday);
            // 두 JSON 데이터를 하나로 합치기
            JsonObject combinedJson = new JsonObject();
            JsonArray emergencyArray = new JsonArray();
            
            // 오늘 데이터 파싱 및 추가
            JsonObject todayJson = JsonParser.parseString(strEmergencyInfo).getAsJsonObject();
            if (todayJson.has("body")) {
                JsonArray todayItems = todayJson.getAsJsonArray("body");
                for (JsonElement item : todayItems) {
                    emergencyArray.add(item);
                }
            }
            
            // 어제 데이터 파싱 및 추가
            JsonObject yesterdayJson = JsonParser.parseString(strEmergencyInfoYesterday).getAsJsonObject();
            if (yesterdayJson.has("body")) {
                JsonArray yesterdayItems = yesterdayJson.getAsJsonArray("body");
                for (JsonElement item : yesterdayItems) {
                    emergencyArray.add(item);
                }
            }
            
            // 합쳐진 데이터를 새로운 JSON 객체에 추가
            combinedJson.add("items", emergencyArray);
            
            return combinedJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
