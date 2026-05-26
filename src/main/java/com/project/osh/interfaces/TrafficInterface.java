package com.project.osh.interfaces;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.ReactiveHttp;

@Component
public class TrafficInterface {

    private static final String ITS_EVENT_INFO_URL = "https://openapi.its.go.kr:9443/eventInfo";
    private static final String EMPTY_RESPONSE = "{\"body\":{\"items\":[]}}";

    @Value("${osh.api.its-traffic.key}")
    private String apiKey;

    private final ReactiveHttp http;

    public TrafficInterface(ReactiveHttp http) {
        this.http = http;
    }

    public String getTrafficInfo() {
        String url = ITS_EVENT_INFO_URL + "?apiKey=" + apiKey + "&type=all&eventType=all&getType=json";
        String body = http.getBlocking(url);
        return (body == null || body.isBlank()) ? EMPTY_RESPONSE : body;
    }
}
