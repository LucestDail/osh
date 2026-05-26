package com.project.osh.controller;

import com.project.osh.service.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * 메인 대시보드 5개 미니카드용 split summary. 응답은 JSON.
     * { weather, air, emergency, traffic, news }
     */
    @GetMapping(value = "/dashboard-split", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDashboardSplit() {
        try {
            return ResponseEntity.ok(geminiService.generateSplitSummary());
        } catch (Exception e) {
            log.error("Error generating split summary", e);
            return ResponseEntity.internalServerError()
                .body("{\"error\":\"split summary 생성 실패\"}");
        }
    }
}
