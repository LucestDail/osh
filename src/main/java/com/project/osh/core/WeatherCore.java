package com.project.osh.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.osh.util.HttpUtil;

public class WeatherCore {
    private static final Logger log = LoggerFactory.getLogger(WeatherCore.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean loggingFlag = true;

    public String getOpenweathermap(String lat, String lon) {
        if(loggingFlag) {
            log.info("{} >> WeatherCore.getOpenweathermap - API 호출 시작 (위도: {}, 경도: {})", dateFormat.format(new Date()), lat, lon);
        }
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=7c1c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c&units=metric&lang=kr";
            String result = new HttpUtil().executeGet(url);
            
            if(loggingFlag) {
                log.info("{} >> WeatherCore.getOpenweathermap - API 응답 결과: {}", dateFormat.format(new Date()), result);
            }
            
            return result;
        } catch (Exception e) {
            log.error("{} >> WeatherCore.getOpenweathermap - API 호출 중 오류 발생: {}", dateFormat.format(new Date()), e.getMessage());
            return null;
        }
    }
} 