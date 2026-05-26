package com.project.osh.interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.HttpUtil;

@Component
public class WeatherInterface {

    private static final Logger log = LoggerFactory.getLogger(WeatherInterface.class);
    private static final String OPENWEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";

    @Value("${osh.api.openweather.key}")
    private String apiKey;

    private final HttpUtil http;

    public WeatherInterface(HttpUtil http) {
        this.http = http;
    }

    public String getOpenweathermap() {
        return getOpenweathermap("37.245807", "127.057375");
    }

    public String getOpenweathermap(String lat, String lon) {
        try {
            String url = OPENWEATHER_URL + "?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey;
            return http.executeGet(url);
        } catch (Exception e) {
            log.error("\ub0a0\uc528 \uc815\ubcf4 API \ud638\ucd9c \uc911 \uc624\ub958 \ubc1c\uc0dd: {}", e.getMessage());
            return "";
        }
    }
}
