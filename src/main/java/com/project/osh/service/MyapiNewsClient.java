package com.project.osh.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.osh.model.News;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * myapi 뉴스 REST 소비 클라이언트.
 *
 * OSH 는 myapi 와 동일한 {@code bs4news_news} 테이블을 직접 JPA 로 조회하던 중복을 제거하기 위해,
 * myapi 가 이미 제공하는 {@code GET /api/social/news} 를 소비해 {@link News} 로 매핑한다.
 *
 * myapi 응답 계약 (produces application/json):
 * <pre>
 * { "data": { "items": [
 *     { "createDT":"yyyy-MM-dd HH:mm:ss", "company":"...", "companyCode":"...",
 *       "title":"...", "content":"...", "link":"...", "reporter":"..." }
 * ] } }
 * </pre>
 *
 * base-url 미설정("") 이거나 호출 실패 시 {@code null} 을 반환 — 호출측이 기존 MySQL 직결로 폴백한다.
 */
@Slf4j
@Component
public class MyapiNewsClient {

    private static final DateTimeFormatter NEWS_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** myapi 게이트웨이 base-url. 빈 값이면 REST 소비를 끄고 MySQL 직결 폴백만 사용. */
    @Value("${osh.news.myapi-base-url:http://127.0.0.1/myapi}")
    private String baseUrl;

    /** 뉴스 조회 타임아웃(ms). */
    @Value("${osh.news.myapi-timeout-ms:5000}")
    private long timeoutMs;

    private WebClient webClient;

    @PostConstruct
    void init() {
        if (isEnabled()) {
            this.webClient = WebClient.builder()
                    .baseUrl(baseUrl.trim())
                    // 뉴스 100건 JSON 이 256KB 기본 버퍼를 초과(content 필드) → 4MB 로 상향
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                    .build();
            log.info("MyapiNewsClient enabled — base-url={}", baseUrl);
        } else {
            log.info("MyapiNewsClient disabled (osh.news.myapi-base-url is empty) — falling back to MySQL");
        }
    }

    /** REST 소비 활성화 여부. */
    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.trim().isEmpty();
    }

    /**
     * myapi {@code /api/social/news} 를 호출해 {@link News} 리스트로 매핑.
     *
     * @return 매핑된 뉴스 리스트. 비활성/실패/응답형식 불일치 시 {@code null}(폴백 신호).
     */
    public List<News> fetchNews() {
        if (!isEnabled() || webClient == null) {
            return null;
        }
        try {
            String body = webClient.get()
                    .uri("/api/social/news")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofMillis(timeoutMs));
            return parse(body);
        } catch (Exception e) {
            log.warn("myapi 뉴스 조회 실패 — MySQL 폴백. cause={}", e.getMessage());
            return null;
        }
    }

    /**
     * myapi JSON 응답을 {@link News} 리스트로 파싱/매핑.
     * 패키지-프라이빗 노출: 매핑 로직 단위 테스트용.
     */
    List<News> parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject()) {
            return null;
        }
        JsonObject data = root.getAsJsonObject().getAsJsonObject("data");
        if (data == null) {
            return null;
        }
        JsonArray items = data.getAsJsonArray("items");
        if (items == null) {
            return null;
        }
        List<News> result = new ArrayList<>(items.size());
        for (JsonElement el : items) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            News news = new News();
            news.setNewsCreateDT(parseDate(str(o, "createDT")));
            news.setNewsCompany(str(o, "company"));
            news.setNewsTitle(str(o, "title"));
            news.setNewsContents(str(o, "content"));
            news.setEtc1(str(o, "link"));       // myapi: link 는 ETC1(원본 URL) 매핑
            news.setNewsFrom(str(o, "reporter"));
            result.add(news);
        }
        return result;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e != null && !e.isJsonNull()) ? e.getAsString() : null;
    }

    private static LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s.trim(), NEWS_DT_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
