package com.project.osh.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 한줄 브리핑(split summary) 서버 캐시.
 * <ul>
 *   <li>스케줄: 1시간마다 백그라운드 갱신</li>
 *   <li>API {@code access=true}: 사용자 접근 시 Gemini 신규 생성 후 캐시 교체</li>
 *   <li>API {@code access=false}: 유효 캐시 반환 (없거나 만료 시 1회 생성)</li>
 * </ul>
 */
@Service
public class SplitBriefingCacheService {

    private static final Logger log = LoggerFactory.getLogger(SplitBriefingCacheService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GeminiService geminiService;
    private final long cacheTtlMs;

    private final Object lock = new Object();
    private volatile String cachedJson;
    private volatile long cachedAtMs;
    private volatile boolean generating;
    private final AtomicLong accessCount = new AtomicLong(0);
    private final AtomicLong lastAccessAtMs = new AtomicLong(0);

    public SplitBriefingCacheService(
            GeminiService geminiService,
            @Value("${osh.briefing.cache-ttl-ms:3600000}") long cacheTtlMs) {
        this.geminiService = geminiService;
        this.cacheTtlMs = cacheTtlMs > 60_000 ? cacheTtlMs : 3_600_000L;
    }

    public record SplitBriefingResult(String json, long generatedAtMs, boolean fromCache, boolean userAccess) {}

    /**
     * @param userAccess true 이면 접근 기록 후 무조건 신규 생성
     */
    public SplitBriefingResult get(boolean userAccess) {
        if (userAccess) {
            accessCount.incrementAndGet();
            lastAccessAtMs.set(System.currentTimeMillis());
            log.info("AI 브리핑 사용자 접근 (누적 {}회) — 신규 생성", accessCount.get());
            return new SplitBriefingResult(regenerateLocked("user-access"), cachedAtMs, false, true);
        }

        synchronized (lock) {
            if (isCacheValid()) {
                return new SplitBriefingResult(cachedJson, cachedAtMs, true, false);
            }
        }
        return new SplitBriefingResult(regenerateLocked("cache-miss"), cachedAtMs, false, false);
    }

    /** 1시간 스케줄용 — 접근 기록이 있을 때만 백그라운드 갱신 */
    public void refreshScheduled() {
        if (accessCount.get() == 0) {
            log.debug("AI 브리핑 스케줄 스킵 — 접근 기록 없음");
            return;
        }
        synchronized (lock) {
            if (generating) {
                log.debug("AI 브리핑 스케줄 갱신 스킵 — 생성 중");
                return;
            }
            try {
                generating = true;
                long t0 = System.currentTimeMillis();
                cachedJson = geminiService.generateSplitSummary();
                cachedAtMs = System.currentTimeMillis();
                log.info("AI 브리핑 스케줄 갱신 완료 ({} ms, 다음 TTL {}분)",
                        cachedAtMs - t0, cacheTtlMs / 60_000);
            } catch (Exception e) {
                log.error("AI 브리핑 스케줄 갱신 실패: {}", e.getMessage());
            } finally {
                generating = false;
            }
        }
    }

    public long getAccessCount() {
        return accessCount.get();
    }

    public String formatCachedAt(long ms) {
        if (ms <= 0) return "-";
        return FMT.format(Instant.ofEpochMilli(ms).atZone(SEOUL));
    }

    private boolean isCacheValid() {
        return cachedJson != null && !cachedJson.isBlank()
                && (System.currentTimeMillis() - cachedAtMs) < cacheTtlMs;
    }

    private String regenerateLocked(String reason) {
        synchronized (lock) {
            if (generating && cachedJson != null && !cachedJson.isBlank()) {
                log.info("AI 브리핑 {} — 생성 중, 기존 캐시 반환", reason);
                return cachedJson;
            }
            try {
                generating = true;
                long t0 = System.currentTimeMillis();
                cachedJson = geminiService.generateSplitSummary();
                cachedAtMs = System.currentTimeMillis();
                log.info("AI 브리핑 {} 생성 완료 ({} ms)", reason, cachedAtMs - t0);
                return cachedJson;
            } catch (Exception e) {
                log.error("AI 브리핑 {} 생성 실패: {}", reason, e.getMessage());
                if (cachedJson != null && !cachedJson.isBlank()) {
                    return cachedJson;
                }
                return "{\"weather\":\"-\",\"air\":\"-\",\"emergency\":\"-\",\"traffic\":\"-\",\"news\":\"요약 생성 실패\"}";
            } finally {
                generating = false;
            }
        }
    }
}
