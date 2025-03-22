package com.project.osh.service;

import com.project.osh.model.News;
import java.util.List;
import com.google.gson.JsonArray;

public interface NewsService {
    List<News> getAllNews();
    List<News> getNewsByCompany(String company);
    void updateNewsData();
    JsonArray getCachedNews();
} 