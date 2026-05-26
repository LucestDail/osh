package com.project.osh.interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.HttpUtil;

@Component
public class TrafficInterface {

    private static final Logger log = LoggerFactory.getLogger(TrafficInterface.class);
    private static final String ITS_EVENT_INFO_URL = "https://openapi.its.go.kr:9443/eventInfo";

    @Value("${osh.api.its-traffic.key}")
    private String apiKey;

    private final HttpUtil http;

    public TrafficInterface(HttpUtil http) {
        this.http = http;
    }

    public String getTrafficInfo() {
        try {
            String url = ITS_EVENT_INFO_URL + "?apiKey=" + apiKey + "&type=all&eventType=all&getType=json";
            return http.executeGet(url);
        } catch (Exception e) {
            log.error("\uad50\ud1b5 \uc815\ubcf4 API \ud638\ucd9c \uc911 \uc624\ub958 \ubc1c\uc0dd: {}", e.getMessage());
            return "{\"body\":{\"items\":[]}}";
        }
    }
}
