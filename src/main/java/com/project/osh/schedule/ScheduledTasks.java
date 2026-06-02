package com.project.osh.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.osh.service.DashboardService;
import com.project.osh.service.DashboardSinks;
import com.project.osh.service.SplitBriefingCacheService;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Value("${osh.logging}")
    private boolean loggingFlag;

    private final DashboardService dashboardService;
    private final DashboardSinks sinks;
    private final SplitBriefingCacheService briefingCache;

    public ScheduledTasks(
            DashboardService dashboardService,
            DashboardSinks sinks,
            SplitBriefingCacheService briefingCache) {
        this.dashboardService = dashboardService;
        this.sinks = sinks;
        this.briefingCache = briefingCache;
    }

    /**
     * 재난문자: 10분 주기 (개발 계정 일 500회 제한 — 3일치×3회/사이클 = 144회/일).
     * initialDelay 2초: 부팅 직후 첫 수집.
     */
    @Scheduled(initialDelay = 2_000, fixedDelay = 600_000)
    public void renewEmergency() {
        safe("emergency", dashboardService::renewEmergencyJsonObject);
    }

    /**
     * 60초 주기: 교통/뉴스 캐시 갱신.
     */
    @Scheduled(initialDelay = 3_000, fixedDelay = 60_000)
    public void renewFrequent() {
        safe("traffic",   dashboardService::renewTrafficJsonObject);
        safe("news",      dashboardService::renewNewsYeonhapJsonObject);
    }

    /**
     * 1시간 주기: 31도시 날씨 + 대기질 일괄 갱신.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void renewWeather() {
        safe("weather", dashboardService::renewWeatherJsonObject);
        safe("air",     dashboardService::renewAirJsonObject);
    }

    /**
     * 1시간 주기: AI 한줄 브리핑 서버 캐시 갱신 (사용자 없을 때도 최신 유지).
     */
    @Scheduled(initialDelay = 120_000, fixedDelay = 3_600_000)
    public void renewSplitBriefing() {
        safe("split-briefing", briefingCache::refreshScheduled);
    }

    /**
     * 1\ucd08 \uc8fc\uae30: application(\uc2dc\uacc4 / \uba54\ubaa8\ub9ac / \ub85c\ub4dc) snapshot \ub9cc push.
     *
     * <p>\uc774\uc804\uc5d0\ub294 weather/traffic/emergency/yeonhap \uae4c\uc9c0 \ud3ec\ud568\ud55c \uc804\uccb4 snapshot(\uc57d 750KB)\ub97c
     * \ub9e4\ucd08 push\ud574\uc11c \ud074\ub77c\uc774\uc5b8\ud2b8\ub2f9 1MB/sec \ub2e4\uc6b4\ub85c\ub4dc\uac00 \ubc1c\uc0dd\ud588\ub2e4.
     * \uc774\uc81c application wrapper(\uc218\ubc31 B)\ub9cc push \u2192 \ub300\uc5ed\ud3ed 99% \uac10\uc18c.
     */
    @Scheduled(fixedRate = 1_000)
    public void tickDashboard() {
        try {
            sinks.pushDashboard(dashboardService.getApplicationWrapperJson());
        } catch (Exception e) {
            log.error("[tick:dashboard] push \uc2e4\ud328: {}", e.getMessage());
        }
    }

    private void safe(String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("[scheduled:{}] \uac31\uc2e0 \uc2e4\ud328: {}", name, e.getMessage());
        }
    }
}
