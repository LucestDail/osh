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

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private EventEmitterService eventEmitterService;

    private List<News> cachedNews = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        executorService.submit(this::loadInitialNewsData);
    }

    private void loadInitialNewsData() {
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
            List<News> newsList = newsRepository.findTop100OrderByNewsCreateDTDesc();
            cachedNews = newsList;
            eventEmitterService.broadcastNewsUpdate(newsList);
        } catch (Exception e) {
            log.error("Error loading initial news data", e);
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
            cachedNews = newsList;
            eventEmitterService.broadcastNewsUpdate(newsList);
        } catch (Exception e) {
            log.error("Error updating news data", e);
        }
    }

    @Override
    public JsonArray getCachedNews() {
        JsonArray jsonArray = new JsonArray();
        for (News news : cachedNews) {
            JsonObject newsObject = new JsonObject();
            newsObject.addProperty("createDT", news.getNewsCreateDT().toString());
            newsObject.addProperty("company", news.getNewsCompany());
            newsObject.addProperty("title", news.getNewsTitle());
            newsObject.addProperty("content", news.getNewsContents());
            jsonArray.add(newsObject);
        }
        return jsonArray;
    }
} 