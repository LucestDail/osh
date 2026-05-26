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
import com.project.osh.util.HttpUtil;

@Component
public class EmergencyInterface {

    private static final Logger log = LoggerFactory.getLogger(EmergencyInterface.class);
    private static final String SAFETY_EMERGENCY_URL = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247";

    @Value("${osh.api.safety-emergency.key}")
    private String apiKey;

    private final HttpUtil http;

    public EmergencyInterface(HttpUtil http) {
        this.http = http;
    }

    public String getEmergencyInfo() {
        // SimpleDateFormat \uc740 thread-safe \ud558\uc9c0 \uc54a\uc544 \ub9e4 \ud638\ucd9c\uc5d0\uc11c \uc0dd\uc131.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String strToday = sdf.format(System.currentTimeMillis());
        String strYesterday = sdf.format(System.currentTimeMillis() - 24L * 60 * 60 * 1000);

        try {
            String today = http.executeGet(SAFETY_EMERGENCY_URL + "?serviceKey=" + apiKey + "&crtDt=" + strToday);
            String yesterday = http.executeGet(SAFETY_EMERGENCY_URL + "?serviceKey=" + apiKey + "&crtDt=" + strYesterday);

            JsonObject combined = new JsonObject();
            JsonArray items = new JsonArray();
            appendBody(items, today);
            appendBody(items, yesterday);
            combined.add("items", items);
            return combined.toString();
        } catch (Exception e) {
            log.error("\uae34\uae09\uc7ac\ub09c\ubb38\uc790 API \ud638\ucd9c \uc911 \uc624\ub958 \ubc1c\uc0dd: {}", e.getMessage());
            return "{\"items\":[]}";
        }
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
