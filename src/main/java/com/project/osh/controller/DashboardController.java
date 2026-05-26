package com.project.osh.controller;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.project.osh.service.DashboardService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    DashboardService dashboardService;

    @GetMapping(value = "/main", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getMain(Model model) {
        return new ModelAndView("dashboard/main");
    }

    @GetMapping(value = "/main/info", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getMainInfo() {
        return tickAndEmit(Duration.ofSeconds(1), "main/info",
                () -> dashboardService.getDashboardJsonObject() != null
                        ? dashboardService.getDashboardJsonObject().toString()
                        : "{}");
    }

    @GetMapping(value = "/main/emergency", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getEmergencyInfo() {
        return tickAndEmit(Duration.ofSeconds(60), "main/emergency",
                () -> dashboardService.getEmergencyWrapperJson() != null
                        ? dashboardService.getEmergencyWrapperJson().toString()
                        : "{}");
    }

    @GetMapping(value = "/main/traffic", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getTrafficInfo() {
        return tickAndEmit(Duration.ofSeconds(60), "main/traffic",
                () -> dashboardService.getTrafficWrapperJson() != null
                        ? dashboardService.getTrafficWrapperJson().toString()
                        : "{}");
    }

    @GetMapping(value = "/main/yeonhap", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getYeonhapInfo() {
        return tickAndEmit(Duration.ofSeconds(60), "main/yeonhap",
                () -> dashboardService.getYeonhapWrapperJson() != null
                        ? dashboardService.getYeonhapWrapperJson().toString()
                        : "{}");
    }

    private Flux<ServerSentEvent<String>> tickAndEmit(Duration interval,
                                                       String streamName,
                                                       PayloadSupplier supplier) {
        return Flux.interval(interval)
                .onBackpressureDrop()
                .map(i -> {
                    try {
                        return ServerSentEvent.<String>builder(supplier.get()).build();
                    } catch (Exception e) {
                        log.error("[{}] payload \uc0dd\uc131 \uc2e4\ud328: {}", streamName, e.getMessage());
                        return ServerSentEvent.<String>builder("{}").build();
                    }
                })
                .onErrorResume(e -> {
                    log.error("[{}] stream \uc624\ub958: {}", streamName, e.getMessage());
                    return Flux.empty();
                })
                .doOnCancel(() -> log.debug("[{}] stream cancelled", streamName))
                .doOnComplete(() -> log.debug("[{}] stream completed", streamName));
    }

    @FunctionalInterface
    private interface PayloadSupplier {
        String get();
    }
}
