package com.project.osh.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * osh.briefing.enabled=false 일 때 {@link SplitBriefingCacheService#get} 이
 * paused=true 를 실어 보내는지 확인한다 — 대시보드가 이 플래그로 "일시 중지"
 * 한 줄 표시와 5카드 반복 표시를 구분한다(2026-10 대시보드 개편).
 */
class SplitBriefingCacheServiceTest {

    @Test
    @DisplayName("enabled=false면 Gemini를 부르지 않고 paused=true를 반환한다")
    void disabled_returnsPausedTrueWithoutCallingGemini() {
        // geminiService=null이어도 안전하다 — !enabled 분기는 get() 안에서 가장 먼저
        // 리턴해 geminiService 필드를 건드리지 않는다.
        SplitBriefingCacheService service = new SplitBriefingCacheService(null, 3_600_000L, false);

        SplitBriefingCacheService.SplitBriefingResult result = service.get(false);

        assertTrue(result.paused());
        assertTrue(result.fromCache());
        assertEquals(0L, result.generatedAtMs());
        assertTrue(result.json().contains("일시 중지"));
    }
}
