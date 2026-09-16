package com.project.osh.controller;

import com.project.osh.model.News;
import com.project.osh.service.NewsService;
import com.project.osh.service.EventEmitterService;
import com.project.osh.util.NewsKeywordFilter;
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

    /**
     * 뉴스 SSE 스트림.
     *
     * @param keywords 관심 키워드(쉼표 구분). 주지 않으면 <b>전부</b> 내려보낸다 —
     *                 키워드를 정하지 않은 사람에게 빈 스트림을 주면 제품이 망가진다.
     *                 필터링은 구독자별로 <b>서버에서</b> 한다(구독자가 늘어도 대역폭이 관심사에 비례).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNews(@RequestParam(name = "keywords", required = false) String keywords) {
        List<String> parsed = NewsKeywordFilter.parse(keywords);
        if (parsed.isEmpty()) {
            // "검사 안 함"과 "통과"를 구분해 남긴다.
            log.debug("[news/stream] 키워드 없음 — 전체 전송");
        } else {
            log.info("[news/stream] 키워드 필터 {}건 적용 — {}", parsed.size(), parsed);
        }
        SseEmitter emitter = eventEmitterService.createEmitter(parsed);

        try {
            List<News> news = NewsKeywordFilter.filterNews(newsService.getAllNews(), parsed);
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