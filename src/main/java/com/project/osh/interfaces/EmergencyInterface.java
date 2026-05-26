package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.osh.util.ReactiveHttp;

import reactor.core.publisher.Mono;

@Component
public class EmergencyInterface {

    private static final Logger log = LoggerFactory.getLogger(EmergencyInterface.class);
    private static final String SAFETY_EMERGENCY_URL = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247";
    private static final String EMPTY_RESPONSE = "{\"items\":[]}";

    @Value("${osh.api.safety-emergency.key}")
    private String apiKey;

    private final ReactiveHttp http;

    public EmergencyInterface(ReactiveHttp http) {
        this.http = http;
    }

    public String getEmergencyInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        long now = System.currentTimeMillis();
        long day = 24L * 60 * 60 * 1000;
        String d0 = sdf.format(now);
        String d1 = sdf.format(now - day);
        String d2 = sdf.format(now - 2 * day);

        try {
            Mono<String> r0 = http.get(buildUrl(d0));
            Mono<String> r1 = http.get(buildUrl(d1));
            Mono<String> r2 = http.get(buildUrl(d2));

            return Mono.zip(r0.defaultIfEmpty(""), r1.defaultIfEmpty(""), r2.defaultIfEmpty(""))
                    .map(tuple -> {
                        JsonObject combined = new JsonObject();
                        JsonArray items = new JsonArray();
                        appendBody(items, tuple.getT1());
                        appendBody(items, tuple.getT2());
                        appendBody(items, tuple.getT3());
                        combined.add("items", items);
                        return combined.toString();
                    })
                    .onErrorReturn(EMPTY_RESPONSE)
                    .blockOptional()
                    .orElse(EMPTY_RESPONSE);
        } catch (Exception e) {
            log.error("긴급재난문자 API 호출 때 오류: {}", e.getMessage());
            return EMPTY_RESPONSE;
        }
    }

    private String buildUrl(String crtDt) {
        return SAFETY_EMERGENCY_URL + "?serviceKey=" + apiKey
                + "&crtDt=" + crtDt + "&numOfRows=500&pageNo=1";
    }

    private void appendBody(JsonArray out, String payload) {
        if (payload == null || payload.isBlank()) return;
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            if (json.has("body") && !json.get("body").isJsonNull()) {
                JsonArray arr = json.getAsJsonArray("body");
                for (JsonElement item : arr) {
                    out.add(item);
                }
            }
        } catch (Exception parseEx) {
            log.warn("\uae34\uae09\uc7ac\ub09c\ubb38\uc790 \uc751\ub2f5 \ud30c\uc2f1 \uc2e4\ud328: {}", parseEx.getMessage());
        }
    }
}
