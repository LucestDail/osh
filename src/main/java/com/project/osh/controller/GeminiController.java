package com.project.osh.controller;

import com.project.osh.service.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    @Autowired
    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateContent(@RequestBody String prompt) {
        String response = geminiService.generateContent(prompt);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/dashboard-summary", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getDashboardSummary() {
        try {
            String summary = geminiService.generateDashboardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error generating dashboard summary", e);
            return ResponseEntity.internalServerError()
                .body("대시보드 요약을 생성하는 중 오류가 발생했습니다: " + e.getMessage());
        }
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