package com.project.osh.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.osh.service.DashboardService;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Value("${osh.logging}")
    private boolean loggingFlag;

    private final DashboardService dashboardService;

    public ScheduledTasks(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 60\ucd08 \uc8fc\uae30: \uc7ac\ub09c/\uad50\ud1b5/\ub274\uc2a4 \uce90\uc2dc \uac31\uc2e0.
     * \ub0a0\uc528\ub294 1\uc2dc\uac04 \uc8fc\uae30\ub77c \ubcc4\ub3c4 \uc2a4\ucf00\uc904\ub7ec\ub85c \ubd84\ub9ac.
     */
    @Scheduled(fixedDelay = 60_000)
    public void renewFrequent() {
        safe("emergency", dashboardService::renewEmergencyJsonObject);
        safe("traffic", dashboardService::renewTrafficJsonObject);
        safe("news",    dashboardService::renewNewsYeonhapJsonObject);
    }

    /**
     * 1\uc2dc\uac04 \uc8fc\uae30: 19\ub3c4\uc2dc \ub0a0\uc528 \uc77c\uad04 \uac31\uc2e0.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void renewWeather() {
        safe("weather", dashboardService::renewWeatherJsonObject);
    }

    private void safe(String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("[scheduled:{}] \uac31\uc2e0 \uc2e4\ud328: {}", name, e.getMessage());
        }
    }
}
