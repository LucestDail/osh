package com.project.osh.controller;

import java.time.Duration;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonObject;
import com.project.osh.service.DashboardSinks;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(25);

    private final DashboardSinks sinks;

    public DashboardController(DashboardSinks sinks) {
        this.sinks = sinks;
    }

    @GetMapping(value = "/main", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getMain(Model model) {
        return new ModelAndView("dashboard/main");
    }

    /**
     * 1\ucd08 tick: application(\uc2dc\uacc4 / \uba54\ubaa8\ub9ac / load) snapshot.
     * \uc774\uc804\uc5d0\ub294 \uc774 \uc2a4\ud2b8\ub9bc\uc5d0 19\ub3c4\uc2dc \ub0a0\uc528 + \uad50\ud1b5 + \uc7ac\ub09c + \ub274\uc2a4\uac00 \ud3ec\ud568\ub418\uc5b4 1\ucd08\ub2f9 \uc218\ubc31KB \uac00 \ub098\uac14\ub358 \uc790\ub9ac.
     */
    @GetMapping(value = "/main/info", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getMainInfo() {
        return stream("main/info", sinks.dashboardStream());
    }

    /**
     * \ub0a0\uc528 \uc804\uc6a9 \uc2a4\ud2b8\ub9bc - 1\uc2dc\uac04\uc5d0 1\ud68c \ubcc0\uacbd\uc774\ubbc0\ub85c \ud3c9\uc18c \ub300\uc5ed\ud3ed \uc81c\ub85c.
     */
    @GetMapping(value = "/main/weather", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getWeatherInfo() {
        return stream("main/weather", sinks.weatherStream());
    }

    @GetMapping(value = "/main/emergency", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getEmergencyInfo() {
        return stream("main/emergency", sinks.emergencyStream());
    }

    @GetMapping(value = "/main/traffic", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getTrafficInfo() {
        return stream("main/traffic", sinks.trafficStream());
    }

    @GetMapping(value = "/main/yeonhap", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getYeonhapInfo() {
        return stream("main/yeonhap", sinks.yeonhapStream());
    }

    /**
     * \uc2f1\ud06c\uc5d0\uc11c \uc628 JsonObject \uc2a4\ud2b8\ub9bc\uc5d0 SSE \ud65c\uc2dd \ub300\uc6c5 + heartbeat \ud569\uc131.
     * \ud074\ub77c\uc774\uc5b8\ud2b8(EventSource)\ub294 \uae30\uc874\uacfc \ub3d9\uc77c\ud558\uac8c data \uc774\ubca4\ud2b8\ub97c \uc218\uc2e0.
     * 25s \uc8fc\uae30 heartbeat\ub85c \uc911\uac04 \ud504\ub85d\uc2dc/\ub85c\ub4dc\ubc38\ub7f0\uc11c\uc758 idle \uc885\ub8cc \ubc29\uc9c0.
     */
    private Flux<ServerSentEvent<String>> stream(String name, Flux<JsonObject> source) {
        Function<JsonObject, ServerSentEvent<String>> toEvent = obj -> ServerSentEvent.<String>builder()
                .id(String.valueOf(System.currentTimeMillis()))
                .data(obj.toString())
                .build();

        Flux<ServerSentEvent<String>> data = source
                .map(toEvent)
                .onErrorResume(e -> {
                    log.error("[{}] stream \uc624\ub958: {}", name, e.getMessage());
                    return Flux.empty();
                });

        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(HEARTBEAT_INTERVAL)
                .map(t -> ServerSentEvent.<String>builder().comment("keep-alive").build());

        return Flux.merge(data, heartbeat)
                .doOnCancel(() -> log.debug("[{}] stream cancelled", name))
                .doOnComplete(() -> log.debug("[{}] stream completed", name));
    }
}
