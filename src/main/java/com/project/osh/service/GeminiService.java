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
    private final boolean gatewayMode;
    private final String gatewayToken;
    private final String gatewayServiceId;
    private final NewsService newsService;
    private final DashboardService dashboardService;

    public GeminiService(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl,
            @Value("${gemini.gateway.token:}") String gatewayToken,
            @Value("${gemini.gateway.service-id:osh}") String gatewayServiceId,
            NewsService newsService,
            @Lazy DashboardService dashboardService) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey != null ? apiKey : "";
        this.apiUrl = apiUrl;
        this.gatewayToken = gatewayToken != null ? gatewayToken : "";
        this.gatewayServiceId = gatewayServiceId != null && !gatewayServiceId.isBlank()
                ? gatewayServiceId : "osh";
        this.gatewayMode = !this.gatewayToken.isBlank()
                || !apiUrl.contains("generativelanguage.googleapis.com");
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
                    "다음 운영 대시보드 스냅샷을 5개 카드(weather, air, emergency, traffic, news) 용 한국어 브리핑으로 정리해주세요.\n" +
                    "규칙:\n" +
                    "1. 이모지/머리말 없이 각 항목 3~5문장(180~280자 사이). 운영자가 한 눈에 상황을 파악할 수 있도록 핵심+근거+영향 순으로.\n" +
                    "2. weather 데이터의 main.temp 값은 켈빈(K) 이므로 반드시 -273 해서 섭씨로 변환해서만 표기(예: 298 → \"25도\"). 'K', 'kelvin', '300K' 같은 원본 단위 절대 금지.\n" +
                    "3. weather: 전국 평균/편차, 특이 지역(가장 더운/추운/비 오는 곳) 2~3곳, 단기예보(오늘 최고/내일 비 여부) 포함.\n" +
                    "4. air: 최악 시도 2~3곳 + PM10/PM2.5 대표 수치 + 전반 등급 + 야외활동 권고 한마디.\n" +
                    "5. emergency: 가장 심각한 1~2건의 지역·내용·발령단계 + 영향 범위. 발령이 없으면 \"진행 중인 긴급재난 없음\" 명시.\n" +
                    "6. traffic: 주요 사고/공사 1~2건의 도로·방향·내용 + 우회 가능 여부.\n" +
                    "7. news: 오늘 핵심 키워드 3~4개로 정치/사회/경제 흐름 요약. 같은 사건 묶어서.\n" +
                    "8. 반드시 아래 스키마의 순수 JSON 만 출력 (코드펜스 금지, 키 순서 고정):\n" +
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

    /** Gemini 호출. gateway 경유 또는 Google 직연결. 내부 전용. */
    private String generateContent(String prompt) {
        try {
            GeminiRequest request = createRequest(prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url;
            if (gatewayMode) {
                if (gatewayToken.isBlank()) {
                    throw new IllegalStateException(
                            "gemini.gateway.token (AI_GATEWAY_TOKEN) is required for gateway mode");
                }
                url = apiUrl;
                headers.set("X-Gateway-Token", gatewayToken);
                headers.set("X-Service-Id", gatewayServiceId);
            } else {
                if (apiKey.isBlank()) {
                    throw new IllegalStateException("gemini.api.key is required for direct Google API mode");
                }
                url = apiUrl + "?key=" + apiKey;
            }

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

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
