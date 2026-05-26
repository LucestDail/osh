package com.project.osh.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.osh.service.DashboardService;
import com.project.osh.service.DashboardSinks;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Value("${osh.logging}")
    private boolean loggingFlag;

    private final DashboardService dashboardService;
    private final DashboardSinks sinks;

    public ScheduledTasks(DashboardService dashboardService, DashboardSinks sinks) {
        this.dashboardService = dashboardService;
        this.sinks = sinks;
    }

    /**
     * 60\ucd08 \uc8fc\uae30: \uc7ac\ub09c/\uad50\ud1b5/\ub274\uc2a4 \uce90\uc2dc \uac31\uc2e0.
     * initialDelay 2\ucd08: \ubd80\ud305 \uc9c1\ud6c4 \ucca0\uc218\uc758 \uace0\ub9bd\uc744 \ub9c9\uace0 \uad6c\ub3c5\uc790\uc5d0\uac8c \uc2e4\ub370\uc774\ud130\ub97c \uc870\uae30 \uc804\ub2ec.
     */
    @Scheduled(initialDelay = 2_000, fixedDelay = 60_000)
    public void renewFrequent() {
        safe("emergency", dashboardService::renewEmergencyJsonObject);
        safe("traffic",   dashboardService::renewTrafficJsonObject);
        safe("news",      dashboardService::renewNewsYeonhapJsonObject);
    }

    /**
     * 1\uc2dc\uac04 \uc8fc\uae30: 19\ub3c4\uc2dc \ub0a0\uc528 \uc77c\uad04 \uac31\uc2e0.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void renewWeather() {
        safe("weather", dashboardService::renewWeatherJsonObject);
    }

    /**
     * 1\ucd08 \uc8fc\uae30: \ub300\uc2dc\ubcf4\ub4dc \uc2a4\ub0c5\uc0f7 push.
     * - applicationJson.currentTime / \uba54\ubaa8\ub9ac / load average \ub4f1 \uc11c\ubc84 \uc9c0\ud45c\ub294 \ud56d\uc0c1 \uc2e4\uc2dc\uac04\uc73c\ub85c \uac31\uc2e0 \ud544\uc694
     * - \uc678\ubd80 API \ud638\ucd9c \uc5c6\uc774 \uba54\ubaa8\ub9ac \uce90\uc2dc \uc9c1\ub82c\ud654\ub9cc \uc218\ud589 \u2192 \ubd80\ud558 \ubbf8\ubbf8
     */
    @Scheduled(fixedRate = 1_000)
    public void tickDashboard() {
        try {
            sinks.pushDashboard(dashboardService.getDashboardSnapshot());
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
