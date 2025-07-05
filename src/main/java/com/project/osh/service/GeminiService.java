package com.project.osh.service;

import com.project.osh.model.GeminiRequest;
import com.project.osh.model.GeminiResponse;
import com.project.osh.model.DashboardSummary;
import com.project.osh.model.News;
import com.project.osh.interfaces.WeatherInterface;
import com.project.osh.interfaces.EmergencyInterface;
import com.project.osh.interfaces.TrafficInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl,
            NewsService newsService,
            WeatherInterface weatherInterface,
            EmergencyInterface emergencyInterface,
            TrafficInterface trafficInterface) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.newsService = newsService;
        this.weatherInterface = weatherInterface;
        this.emergencyInterface = emergencyInterface;
        this.trafficInterface = trafficInterface;
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
            
            // 각 도시별 날씨 정보 수집
            StringBuilder weatherDataBuilder = new StringBuilder();
            String[][] cities = {
                {"창원", "35.2273", "128.6817"},  // 창원
                {"서울", "37.5665", "126.9780"},  // 서울
                {"부산", "35.1796", "129.0756"},  // 부산
                {"인천", "37.4563", "126.7052"},  // 인천
                {"대구", "35.8687", "128.5990"},  // 대구
                {"대전", "36.3505", "127.3750"},  // 대전
                {"광주", "35.1600", "126.8514"},  // 광주
                {"수원", "37.2636", "127.0286"},  // 수원
                {"울산", "35.5384", "129.3114"},  // 울산
                {"고양", "37.6584", "126.8320"},  // 고양
                {"용인", "37.2411", "127.1776"},  // 용인
                {"포항", "36.0320", "129.3650"},  // 포항
                {"김해", "35.2284", "128.8893"},  // 김해
                {"김천", "36.1398", "128.1136"},  // 김천
                {"제주", "33.4996", "126.5312"},  // 제주
                {"춘천", "37.8813", "127.7300"},  // 춘천
                {"원주", "37.3442", "127.9200"},  // 원주
                {"강릉", "37.7519", "128.8960"},  // 강릉
                {"속초", "38.2070", "128.5928"}   // 속초
            };

            weatherDataBuilder.append("전국 주요 도시 날씨 정보:\n\n");
            for (String[] city : cities) {
                String cityName = city[0];
                String lat = city[1];
                String lon = city[2];
                String cityWeather = weatherInterface.getOpenweathermap(lat, lon);
                weatherDataBuilder.append(cityName).append(" 날씨 정보:\n");
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