package com.project.osh.controller;

import com.project.osh.model.News;
import com.project.osh.service.NewsService;
import com.project.osh.service.EventEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private EventEmitterService eventEmitterService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNews() {
        SseEmitter emitter = eventEmitterService.createEmitter();

        try {
            List<News> news = newsService.getAllNews();
            emitter.send(news, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            log.error("Error sending initial news data", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @GetMapping("/all")
    public List<News> getAllNews() {
        List<News> news = newsService.getAllNews();
        return news;
    }

    @GetMapping("/company/{company}")
    public List<News> getNewsByCompany(@PathVariable String company) {
        List<News> news = newsService.getNewsByCompany(company);
        return news;
    }
} 