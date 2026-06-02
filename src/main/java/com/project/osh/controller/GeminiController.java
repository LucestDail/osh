package com.project.osh.controller;

import com.project.osh.service.SplitBriefingCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final SplitBriefingCacheService briefingCache;

    public GeminiController(SplitBriefingCacheService briefingCache) {
        this.briefingCache = briefingCache;
    }

    /**
     * 메인 대시보드 5개 미니카드용 split summary. 응답은 JSON.
     * { weather, air, emergency, traffic, news }
     *
     * @param access true: 사용자 접근 — 캐시 무시하고 신규 생성. false: 서버 캐시(1시간 TTL) 반환
     */
    @GetMapping(value = "/dashboard-split", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDashboardSplit(
            @RequestParam(name = "access", defaultValue = "false") boolean access) {
        try {
            SplitBriefingCacheService.SplitBriefingResult result = briefingCache.get(access);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Briefing-Cached", String.valueOf(result.fromCache()));
            headers.set("X-Briefing-Generated-At", briefingCache.formatCachedAt(result.generatedAtMs()));
            headers.set("X-Briefing-User-Access", String.valueOf(result.userAccess()));
            return ResponseEntity.ok().headers(headers).body(result.json());
        } catch (Exception e) {
            log.error("Error generating split summary", e);
            return ResponseEntity.internalServerError()
                .body("{\"error\":\"split summary 생성 실패\"}");
        }
    }
}
