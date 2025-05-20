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
        log.info("Received prompt: {}", prompt);
        String response = geminiService.generateContent(prompt);
        log.info("Generated response: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/dashboard-summary", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getDashboardSummary() {
        try {
            log.info("Generating dashboard summary");
            String summary = geminiService.generateDashboardSummary();
            log.info("Dashboard summary generated successfully");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error generating dashboard summary", e);
            return ResponseEntity.internalServerError()
                .body("대시보드 요약을 생성하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 