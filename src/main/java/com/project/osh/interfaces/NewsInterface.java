package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.ReactiveHttp;

@Component
public class NewsInterface {

    private static final String SAFETY_NEWS_URL = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00051";

    @Value("${osh.api.safety-news.key}")
    private String apiKey;

    private final ReactiveHttp http;

    public NewsInterface(ReactiveHttp http) {
        this.http = http;
    }

    public String getYeonhapNews() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String strToday = sdf.format(System.currentTimeMillis());
        String url = SAFETY_NEWS_URL + "?serviceKey=" + apiKey + "&inqDt=" + strToday;
        String body = http.getBlocking(url);
        return body == null ? "" : body;
    }
}
