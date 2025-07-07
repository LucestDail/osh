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
            if (news != null && !news.isEmpty()) {
                emitter.send(news, MediaType.APPLICATION_JSON);
            } else {
                // 빈 데이터라도 전송하여 클라이언트가 연결 상태를 유지하도록 함
                emitter.send(List.of(), MediaType.APPLICATION_JSON);
            }
        } catch (IOException e) {
            log.error("Error sending initial news data", e);
            try {
                emitter.send(List.of(), MediaType.APPLICATION_JSON);
            } catch (IOException sendError) {
                log.error("Error sending empty news data", sendError);
                emitter.completeWithError(sendError);
            }
        } catch (Exception e) {
            log.error("Unexpected error in news stream", e);
            try {
                emitter.send(List.of(), MediaType.APPLICATION_JSON);
            } catch (IOException sendError) {
                log.error("Error sending empty news data after exception", sendError);
                emitter.completeWithError(sendError);
            }
        }

        return emitter;
    }

    @GetMapping("/all")
    public List<News> getAllNews() {
        try {
            List<News> news = newsService.getAllNews();
            return news != null ? news : List.of();
        } catch (Exception e) {
            log.error("Error getting all news", e);
            return List.of();
        }
    }

    @GetMapping("/company/{company}")
    public List<News> getNewsByCompany(@PathVariable String company) {
        try {
            List<News> news = newsService.getNewsByCompany(company);
            return news != null ? news : List.of();
        } catch (Exception e) {
            log.error("Error getting news for company: {}", company, e);
            return List.of();
        }
    }
} 