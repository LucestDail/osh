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
