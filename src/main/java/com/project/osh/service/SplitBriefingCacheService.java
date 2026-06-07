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
            generating = true;
        }
        try {
            long t0 = System.currentTimeMillis();
            String result = geminiService.generateSplitSummary();
            if (isValidBriefing(result)) {
                storeCache(result);
                log.info("AI 브리핑 스케줄 갱신 완료 ({} ms, 다음 TTL {}분)",
                        System.currentTimeMillis() - t0, cacheTtlMs / 60_000);
            } else {
                log.warn("AI 브리핑 스케줄 갱신 — 실패 응답, 기존 캐시 유지");
            }
        } catch (Exception e) {
            log.error("AI 브리핑 스케줄 갱신 실패: {}", e.getMessage());
        } finally {
            synchronized (lock) {
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
        return isValidBriefing(cachedJson)
                && (System.currentTimeMillis() - cachedAtMs) < cacheTtlMs;
    }

    private String regenerateLocked(String reason) {
        synchronized (lock) {
            if (generating) {
                if (isValidBriefing(cachedJson)) {
                    log.info("AI 브리핑 {} — 생성 중, 기존 캐시 반환", reason);
                    return cachedJson;
                }
                log.info("AI 브리핑 {} — 생성 중, 대기 없이 실패 응답", reason);
                return failureJson();
            }
            generating = true;
        }

        long t0 = System.currentTimeMillis();
        try {
            String result = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                result = geminiService.generateSplitSummary();
                if (isValidBriefing(result)) {
                    storeCache(result);
                    log.info("AI 브리핑 {} 생성 완료 ({} ms, attempt {})", reason,
                            System.currentTimeMillis() - t0, attempt);
                    return result;
                }
                log.warn("AI 브리핑 {} attempt {}/3 — 실패 응답, 재시도", reason, attempt);
                if (attempt < 3) {
                    try {
                        Thread.sleep(2_000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            synchronized (lock) {
                if (isValidBriefing(cachedJson)) {
                    return cachedJson;
                }
            }
            return result != null ? result : failureJson();
        } catch (Exception e) {
            log.error("AI 브리핑 {} 생성 실패: {}", reason, e.getMessage());
            synchronized (lock) {
                if (isValidBriefing(cachedJson)) {
                    return cachedJson;
                }
            }
            return failureJson();
        } finally {
            synchronized (lock) {
                generating = false;
            }
        }
    }

    private void storeCache(String json) {
        synchronized (lock) {
            cachedJson = json;
            cachedAtMs = System.currentTimeMillis();
        }
    }

    private boolean isValidBriefing(String json) {
        return json != null && !json.isBlank() && !json.contains("\"news\":\"요약 생성 실패\"");
    }

    private static String failureJson() {
        return "{\"weather\":\"-\",\"air\":\"-\",\"emergency\":\"-\",\"traffic\":\"-\",\"news\":\"요약 생성 실패\"}";
    }
}
