package com.project.osh.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.osh.model.GeminiRequest;
import com.project.osh.model.GeminiResponse;
import com.project.osh.model.News;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Gemini API 호출. 현재는 메인 대시보드 상단 5카드용 split summary 만 노출한다.
 * (이전의 마크다운 형식 종합 분석은 split 으로 대체되어 제거)
 */
@Slf4j
@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final NewsService newsService;
    private final DashboardService dashboardService;

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl,
            NewsService newsService,
            @Lazy DashboardService dashboardService) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.newsService = newsService;
        this.dashboardService = dashboardService;
    }

    /**
     * 5 카드용 짧은 분할 요약. 응답은 순수 JSON 문자열.
     *   { "weather": "...", "air": "...", "emergency": "...", "traffic": "...", "news": "..." }
     * 대시보드 캐시를 그대로 사용해 19회 재호출 회피.
     */
    public String generateSplitSummary() {
        try {
            String weatherCache   = dashboardService.getWeatherWrapperJson().toString();
            String airCache       = dashboardService.getAirWrapperJson().toString();
            String emergencyCache = dashboardService.getEmergencyWrapperJson().toString();
            String trafficCache   = dashboardService.getTrafficWrapperJson().toString();

            List<News> recentNews = newsService.getAllNews();
            StringBuilder newsBrief = new StringBuilder();
            int n = Math.min(recentNews.size(), 15);
            for (int i = 0; i < n; i++) {
                News nw = recentNews.get(i);
                if (nw != null && nw.getNewsTitle() != null) {
                    newsBrief.append("- ").append(nw.getNewsTitle()).append('\n');
                }
            }

            String prompt =
                    "다음 운영 대시보드 스냅샷을 5개 카드(weather, air, emergency, traffic, news) 용 짧은 한국어 요약으로 압축해주세요.\n" +
                    "규칙:\n" +
                    "1. 이모지/머리말 없이 각 항목 1–2문장(최대 80자).\n" +
                    "2. weather 데이터의 main.temp 값은 켈빈(K) 이므로 반드시 -273 해서 섭씨로 변환해서만 표기(예: 298 → \"25도\"). 'K', 'kelvin', '300K' 같은 원본 단위 표기 절대 금지.\n" +
                    "3. 날씨는 특이 지역/기온/강수 중심으로 1문장.\n" +
                    "4. air 는 가장 나쁜 시도 1–2곳과 전반적 등급만.\n" +
                    "5. 재난/교통은 현시점 주요 이슈 한두개.\n" +
                    "6. 뉴스는 오늘 핵심 키워드와 흐름.\n" +
                    "7. 반드시 아래 스키마의 순수 JSON 만 출력 (코드펜스 금지):\n" +
                    "{\"weather\":\"...\",\"air\":\"...\",\"emergency\":\"...\",\"traffic\":\"...\",\"news\":\"...\"}\n\n" +
                    "[weather wrapper] (main.temp 는 K, 먼저 섭씨로 변환 후 쓸 것)\n" + weatherCache + "\n\n" +
                    "[air wrapper]\n" + airCache + "\n\n" +
                    "[emergency wrapper]\n" + emergencyCache + "\n\n" +
                    "[traffic wrapper]\n" + trafficCache + "\n\n" +
                    "[news headlines]\n" + newsBrief.toString();

            String raw = generateContent(prompt);
            return sanitizeJson(raw);
        } catch (Exception e) {
            log.error("Error generating split summary", e);
            JsonObject err = new JsonObject();
            err.addProperty("weather", "-");
            err.addProperty("air", "-");
            err.addProperty("emergency", "-");
            err.addProperty("traffic", "-");
            err.addProperty("news", "요약 생성 실패");
            return err.toString();
        }
    }

    /** Gemini 호출. 내부 전용 (외부에 노출하지 않음). */
    private String generateContent(String prompt) {
        try {
            GeminiRequest request = createRequest(prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);
            String url = apiUrl + "?key=" + apiKey;

            GeminiResponse response = restTemplate.postForObject(url, entity, GeminiResponse.class);

            if (response != null
                    && !response.getCandidates().isEmpty()
                    && !response.getCandidates().get(0).getContent().getParts().isEmpty()) {
                return response.getCandidates().get(0).getContent().getParts().get(0).getText();
            }
            return "{}";
        } catch (Exception e) {
            log.error("Error generating content with Gemini API", e);
            throw new RuntimeException("Failed to generate content", e);
        }
    }

    /** 코드펜스 감싸지거나 부수 텍스트가 난 응답을 순수 JSON 으로 정리. */
    private String sanitizeJson(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) s = s.substring(firstNl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        int start = s.indexOf('{');
        int end   = s.lastIndexOf('}');
        if (start >= 0 && end > start) s = s.substring(start, end + 1);
        try {
            JsonParser.parseString(s);
            return s;
        } catch (Exception e) {
            JsonObject fallback = new JsonObject();
            fallback.addProperty("weather", "-");
            fallback.addProperty("air", "-");
            fallback.addProperty("emergency", "-");
            fallback.addProperty("traffic", "-");
            fallback.addProperty("news", s.length() > 200 ? s.substring(0, 200) + "..." : s);
            return fallback.toString();
        }
    }

    private GeminiRequest createRequest(String prompt) {
        GeminiRequest request = new GeminiRequest();
        GeminiRequest.Content content = new GeminiRequest.Content();
        GeminiRequest.Part part = new GeminiRequest.Part();
        part.setText(prompt);
        content.setParts(List.of(part));
        request.setContents(List.of(content));
        return request;
    }
}
