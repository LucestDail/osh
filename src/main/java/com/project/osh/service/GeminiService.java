package com.project.osh.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.osh.config.OshProperties;
import com.project.osh.interfaces.EmergencyInterface;
import com.project.osh.interfaces.TrafficInterface;
import com.project.osh.interfaces.WeatherInterface;
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

@Slf4j
@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;
    private final NewsService newsService;
    private final WeatherInterface weatherInterface;
    private final EmergencyInterface emergencyInterface;
    private final TrafficInterface trafficInterface;
    private final OshProperties properties;
    private final DashboardService dashboardService;

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl,
            NewsService newsService,
            WeatherInterface weatherInterface,
            EmergencyInterface emergencyInterface,
            TrafficInterface trafficInterface,
            OshProperties properties,
            @Lazy DashboardService dashboardService) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.newsService = newsService;
        this.weatherInterface = weatherInterface;
        this.emergencyInterface = emergencyInterface;
        this.trafficInterface = trafficInterface;
        this.properties = properties;
        this.dashboardService = dashboardService;
    }

    public String generateContent(String prompt) {
        try {
            GeminiRequest request = createRequest(prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);
            String url = apiUrl + "?key=" + apiKey;

            GeminiResponse response = restTemplate.postForObject(url, entity, GeminiResponse.class);
            
            if (response != null && 
                !response.getCandidates().isEmpty() && 
                !response.getCandidates().get(0).getContent().getParts().isEmpty()) {
                String result = response.getCandidates().get(0).getContent().getParts().get(0).getText();
                return result;
            }
            
            return "No response generated";
        } catch (Exception e) {
            log.error("Error generating content with Gemini API", e);
            throw new RuntimeException("Failed to generate content", e);
        }
    }

    public String generateDashboardSummary() {
        try {
            // 데이터 수집
            List<News> recentNews = newsService.getAllNews();
            
            // 각 도시별 날씨 정보 수집 (OshProperties 단일 출처)
            StringBuilder weatherDataBuilder = new StringBuilder();
            weatherDataBuilder.append("전국 주요 도시 날씨 정보:\n\n");
            for (OshProperties.City city : properties.getCities()) {
                String cityWeather = weatherInterface.getOpenweathermap(city.getLat(), city.getLon());
                weatherDataBuilder.append(city.getName()).append(" 날씨 정보:\n");
                weatherDataBuilder.append(cityWeather).append("\n\n");
            }
            String weatherData = weatherDataBuilder.toString();
            
            String emergencyData = emergencyInterface.getEmergencyInfo();
            String trafficData = trafficInterface.getTrafficInfo();
            
            if (recentNews.isEmpty()) {
                return "데이터를 불러오는 중 오류가 발생했습니다.";
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("다음 데이터를 바탕으로 종합적인 대시보드 분석을 생성해주세요. 마크다운 형식으로 응답해주세요:\n\n");
            
            // 데이터 제공
            promptBuilder.append("데이터:\n\n");
            
            // 1. 뉴스 데이터 - 더 많은 뉴스 항목 포함
            promptBuilder.append("1. 뉴스 데이터 (최근 주요 뉴스):\n");
            int newsCount = Math.min(recentNews.size(), 50); // 뉴스 항목 수 증가
            for (int i = 0; i < newsCount; i++) {
                News news = recentNews.get(i);
                promptBuilder.append("- 제목: ").append(news.getNewsTitle()).append("\n");
                promptBuilder.append("  내용: ").append(news.getNewsContents()).append("\n\n");
            }

            // 2. 날씨 데이터 - 더 상세한 분석 요청
            promptBuilder.append("2. 현재 날씨 정보 (종합 분석):\n");
            promptBuilder.append(weatherData).append("\n\n");
            promptBuilder.append("날씨 분석 시 다음 사항을 포함해주세요:\n");
            promptBuilder.append("- 전국 주요 지역의 날씨 특이사항\n");
            promptBuilder.append("- 특별히 주의가 필요한 지역의 날씨 상황\n");
            promptBuilder.append("- 전반적인 기온 추이와 변화 방향\n");
            promptBuilder.append("- 향후 24시간 날씨 전망\n");
            promptBuilder.append("- 날씨 관련 주의사항이나 특이사항\n\n");

            // 3. 긴급재난문자 데이터
            promptBuilder.append("3. 긴급재난문자 정보:\n");
            promptBuilder.append(emergencyData).append("\n\n");
            
            // 4. 교통돌발정보 데이터
            promptBuilder.append("4. 교통돌발정보:\n");
            promptBuilder.append(trafficData).append("\n\n");

            // 추가 분석 지시사항
            promptBuilder.append("분석 시 다음 사항을 고려해주세요:\n");
            promptBuilder.append("1. 뉴스 데이터는 사회, 정치, 경제 등 주요 이슈를 중심으로 분석하고, 관련성이 있는 뉴스들을 그룹화하여 설명해주세요.\n");
            promptBuilder.append("2. 날씨 정보는 켈빈 단위로 표시하지 말아야 하며, 썹씨 기준으로 표시합니다. 단 표시 기준을 명시하지는 말아주세요. 날씨 정보는 전국적인 관점에서 분석하고, 특이사항이 있는 지역을 중심으로 설명해주세요.\n");
            promptBuilder.append("3. 긴급재난문자와 교통돌발정보는 현재 발생한 주요 사건들을 중심으로 분석해주세요.\n");
            promptBuilder.append("4. 모든 정보를 종합하여 현재 상황에 대한 전반적인 평가와 향후 주의사항을 제시하되, 추상적이고 보편적인 내용이 아닌 핵심적인 내용을 포함해주세요.\n\n");

            String prompt = promptBuilder.toString();
            
            String summaryResponse = generateContent(prompt);
            
            return summaryResponse;
                    
        } catch (Exception e) {
            log.error("Error generating dashboard summary", e);
            return "데이터를 불러오는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 5 \uce74\ub4dc \uc6a9 \uc9e7\uc740 \ubd84\ud560 \uc694\uc57d. \uc751\ub2f5\uc740 \uc21c\uc218 JSON \ubb38\uc790\uc5f4.
     *   { "weather": "...", "air": "...", "emergency": "...", "traffic": "...", "news": "..." }
     * \ub300\uc2dc\ubcf4\ub4dc \ucea0\uc2dc\ub97c \uadf8\ub300\ub85c \uc0ac\uc6a9\ud574 19\ud68c \uc7ac\ud638\ucd9c \ud68c\ud53c.
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
                    "\ub2e4\uc74c \uc6b4\uc601 \ub300\uc2dc\ubcf4\ub4dc \uc2a4\ub0c5\uc0f7\uc744 5\uac1c \uce74\ub4dc(weather, air, emergency, traffic, news) \uc6a9 \uc9e7\uc740 \ud55c\uad6d\uc5b4 \uc694\uc57d\uc73c\ub85c \uc555\ucd95\ud574\uc8fc\uc138\uc694.\n" +
                    "\uaddc\uce59:\n" +
                    "1. \uc774\ubaa8\uc9c0/\uba38\ub9ac\ub9d0 \uc5c6\uc774 \uac01 \ud56d\ubaa9 1\u20132\ubb38\uc7a5(\ucd5c\ub300 80\uc790).\n" +
                    "2. weather \ub370\uc774\ud130\uc758 main.temp \uac12\uc740 \uc910\ub300\uc628\ub3c4(K) \uc774\ubbc0\ub85c \ubc18\ub4dc\uc2dc -273 \ud574\uc11c \uc12d\uc528\ub85c \ubcc0\ud658\ud574\uc11c\ub9cc \ud45c\uae30(\uc608: 298 \u2192 \"25\ub3c4\"). 'K', 'kelvin', '300K' \uac19\uc740 \uc6d0\ubcc0\ud658 \ud45c\uae30 \uc808\ub300 \uae08\uc9c0.\n" +
                    "3. \ub0a0\uc528\ub294 \ud2b9\uc774 \uc9c0\uc5ed/\uae30\uc628/\uac15\uc218 \uc911\uc2ec\uc73c\ub85c 1\ubb38\uc7a5.\n" +
                    "4. air \ub294 \uac00\uc7a5 \ub098\uc05c \uc2dc\ub3c4 1\u20132\uacf3\uacfc \uc804\ubc18\uc801 \ub4f1\uae09\ub9cc.\n" +
                    "5. \uc7ac\ub09c/\uad50\ud1b5\uc740 \ud604\uc2dc\uc810 \uc8fc\uc694 \uc774\uc288 \ud55c\ub450\uac1c.\n" +
                    "6. \ub274\uc2a4\ub294 \uc624\ub298 \ud575\uc2ec \ud0a4\uc6cc\ub4dc\uc640 \ud750\ub984.\n" +
                    "7. \ubc18\ub4dc\uc2dc \uc544\ub798 \uc2a4\ud0a4\ub9c8\uc758 \uc21c\uc218 JSON \ub9cc \ucd9c\ub825 (\ucf54\ub4dc\ud3f0\uc2a4 \uae08\uc9c0):\n" +
                    "{\"weather\":\"...\",\"air\":\"...\",\"emergency\":\"...\",\"traffic\":\"...\",\"news\":\"...\"}\n\n" +
                    "[weather wrapper] (main.temp \ub294 K, \uba3c\uc800 \uc12d\uc528\ub85c \ubcc0\ud658 \ud6c4 \uc4f8 \uac83)\n" + weatherCache + "\n\n" +
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
            err.addProperty("news", "\uc694\uc57d \uc0dd\uc131 \uc2e4\ud328");
            return err.toString();
        }
    }

    /** \ucf54\ub4dc\ud3f0\uc2a4 \uac10\uc2f8\uc9c0\uac70\ub098 \ubd80\uc218 \ud14d\uc2a4\ud2b8\uac00 \ub09c \uc751\ub2f5\uc744 \uc21c\uc218 JSON \uc73c\ub85c \uc815\ub9ac. */
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