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
        String strToday = sdf.format(System.currentTimeMillis());
        String strYesterday = sdf.format(System.currentTimeMillis() - 24L * 60 * 60 * 1000);

        // \uc624\ub298 + \uc5b4\uc81c \ub3d9\uc2dc \uc870\ud68c \ud6c4 \ud569\uce58\uae30
        try {
            Mono<String> today = http.get(buildUrl(strToday));
            Mono<String> yesterday = http.get(buildUrl(strYesterday));

            return Mono.zip(today.defaultIfEmpty(""), yesterday.defaultIfEmpty(""))
                    .map(tuple -> {
                        JsonObject combined = new JsonObject();
                        JsonArray items = new JsonArray();
                        appendBody(items, tuple.getT1());
                        appendBody(items, tuple.getT2());
                        combined.add("items", items);
                        return combined.toString();
                    })
                    .onErrorReturn(EMPTY_RESPONSE)
                    .blockOptional()
                    .orElse(EMPTY_RESPONSE);
        } catch (Exception e) {
            log.error("\uae34\uae09\uc7ac\ub09c\ubb38\uc790 API \ud638\ucd9c \ub54c \uc624\ub958: {}", e.getMessage());
            return EMPTY_RESPONSE;
        }
    }

    private String buildUrl(String crtDt) {
        return SAFETY_EMERGENCY_URL + "?serviceKey=" + apiKey + "&crtDt=" + crtDt;
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
