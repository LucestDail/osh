package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.HttpUtil;

@Component
public class NewsInterface {

    private static final Logger log = LoggerFactory.getLogger(NewsInterface.class);
    private static final String SAFETY_NEWS_URL = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00051";

    @Value("${osh.api.safety-news.key}")
    private String apiKey;

    private final HttpUtil http;

    public NewsInterface(HttpUtil http) {
        this.http = http;
    }

    public String getYeonhapNews() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String strToday = sdf.format(System.currentTimeMillis());
        try {
            return http.executeGet(SAFETY_NEWS_URL + "?serviceKey=" + apiKey + "&inqDt=" + strToday);
        } catch (Exception e) {
            log.error("\ub274\uc2a4 \uc815\ubcf4 API \ud638\ucd9c \uc911 \uc624\ub958 \ubc1c\uc0dd: {}", e.getMessage());
            return "";
        }
    }
}
