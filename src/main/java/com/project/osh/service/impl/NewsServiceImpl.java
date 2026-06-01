package com.project.osh.service.impl;

import com.project.osh.model.News;
import com.project.osh.repository.news.NewsRepository;
import com.project.osh.service.NewsService;
import com.project.osh.service.EventEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter NEWS_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private EventEmitterService eventEmitterService;

    private List<News> cachedNews = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean isInitialized = false;

    @PostConstruct
    public void init() {
        // 초기화 시 빈 리스트로 설정하여 null 접근 방지
        cachedNews = new CopyOnWriteArrayList<>();
        executorService.submit(this::loadInitialNewsData);
    }

    private void loadInitialNewsData() {
        int retryCount = 0;
        final int maxRetries = 5;
        final long retryDelayMs = 10000; // 10초

        while (retryCount < maxRetries && !isInitialized) {
            try {
                log.info("Loading initial news data (attempt {}/{})", retryCount + 1, maxRetries);
                List<News> newsList = newsRepository.findTop100OrderByNewsCreateDTDesc();
                
                if (newsList != null && !newsList.isEmpty()) {
                    cachedNews = newsList;
                    eventEmitterService.broadcastNewsUpdate(newsList);
                    isInitialized = true;
                    log.info("Successfully loaded {} news items", newsList.size());
                } else {
                    log.warn("No news data found, keeping empty cache");
                    // 빈 리스트라도 캐시에 설정하여 null 접근 방지
                    cachedNews = new CopyOnWriteArrayList<>();
                    isInitialized = true;
                }
            } catch (Exception e) {
                retryCount++;
                log.error("Error loading initial news data (attempt {}/{}): {}", retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Failed to load initial news data after {} attempts, keeping empty cache", maxRetries);
                    // 최종적으로 빈 리스트라도 캐시에 설정
                    cachedNews = new CopyOnWriteArrayList<>();
                    isInitialized = true;
                }
            }
        }
    }

    @Override
    public List<News> getAllNews() {
        return cachedNews;
    }

    @Override
    public List<News> getNewsByCompany(String company) {
        try {
            return newsRepository.findTop100ByNewsCompanyOrderByNewsCreateDTDesc(company);
        } catch (Exception e) {
            log.error("Error getting news for company: {}", company, e);
            return List.of();
        }
    }

    @Override
    @Scheduled(fixedRate = 60000)
    public void updateNewsData() {
        try {
            List<News> newsList = newsRepository.findTop100OrderByNewsCreateDTDesc();
            
            if (newsList != null && !newsList.isEmpty()) {
                cachedNews = newsList;
                eventEmitterService.broadcastNewsUpdate(newsList);
                log.debug("Successfully updated news cache with {} items", newsList.size());
            } else {
                log.warn("No news data found during update, keeping existing cache");
                // 데이터가 없어도 기존 캐시 유지
            }
        } catch (Exception e) {
            log.error("Error updating news data: {}", e.getMessage());
            // 에러 발생 시에도 기존 캐시 유지
        }
    }

    @Override
    public JsonArray getCachedNews() {
        JsonArray jsonArray = new JsonArray();
        
        try {
            if (cachedNews != null && !cachedNews.isEmpty()) {
                for (News news : cachedNews) {
                    JsonObject newsObject = new JsonObject();
                    if (news.getNewsCreateDT() != null) {
                        ZonedDateTime zdt = news.getNewsCreateDT().atZone(SEOUL);
                        newsObject.addProperty("createDT", zdt.format(NEWS_DT_FMT));
                        newsObject.addProperty("createDTMs", zdt.toInstant().toEpochMilli());
                    } else {
                        newsObject.addProperty("createDT", "");
                        newsObject.addProperty("createDTMs", 0);
                    }
                    newsObject.addProperty("company", news.getNewsCompany());
                    newsObject.addProperty("title", news.getNewsTitle());
                    newsObject.addProperty("content", news.getNewsContents());
                    jsonArray.add(newsObject);
                }
            } else {
                // 데이터가 없을 때 빈 객체라도 반환하여 null 접근 방지
                JsonObject emptyNewsObject = new JsonObject();
                emptyNewsObject.addProperty("createDT", "");
                emptyNewsObject.addProperty("company", "");
                emptyNewsObject.addProperty("title", "데이터를 불러오는 중입니다...");
                emptyNewsObject.addProperty("content", "뉴스 데이터가 준비되지 않았습니다.");
                jsonArray.add(emptyNewsObject);
            }
        } catch (Exception e) {
            log.error("Error creating JSON from cached news: {}", e.getMessage());
            // 에러 발생 시에도 빈 객체 반환
            JsonObject errorNewsObject = new JsonObject();
            errorNewsObject.addProperty("createDT", "");
            errorNewsObject.addProperty("company", "");
            errorNewsObject.addProperty("title", "데이터 로드 중 오류가 발생했습니다");
            errorNewsObject.addProperty("content", "잠시 후 다시 시도해주세요.");
            jsonArray.add(errorNewsObject);
        }
        
        return jsonArray;
    }
} 