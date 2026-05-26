package com.project.osh.controller;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
    public ModelAndView getMain() {
        return new ModelAndView("dashboard/main");
    }

    /**
     * \ub2e8\uc77c \ud1b5\ud569 \uc2a4\ud2b8\ub9bc \u2014 \ud55c \uac1c\uc758 SSE \uc5f0\uacb0\uc73c\ub85c 5\uc885\ub958 \ub370\uc774\ud130\ub97c event type\uc73c\ub85c \uad6c\ubd84\ud574 \uc804\uc1a1.
     *
     * <p>\uc774\uc804: \ud074\ub77c\uc774\uc5b8\ud2b8\uac00 /main/info, /main/weather, /main/emergency, /main/traffic,
     * /main/yeonhap \uc5d0 \uac01\uac01 EventSource \ub97c \uc5f4\uc5b4 \ucd1d 5\uac1c \ucee4\ub125\uc158 \uc810\uc720 \u2192 \ube0c\ub77c\uc6b0\uc800
     * HTTP/1.1 origin-per-host 6\uac1c \uc81c\ud55c\uc744 \ub2e4 \ucc28\uc9c0\ud574 \uac19\uc740 \ub3c4\uba54\uc778\uc758 \ub2e4\ub978 \uc11c\ube44\uc2a4(myapi/simpleStock\u00b7\u00b7\u00b7)
     * \ud638\ucd9c\uc774 \ub300\uae30\ud558\ub294 \ubb38\uc81c\uac00 \uc788\uc5c8\ub2e4.
     *
     * <p>\ud604\uc7ac: 1\uac1c \ucee4\ub125\uc158\ub9cc \uc0ac\uc6a9. EventSource.addEventListener("dashboard"|"weather"|...) \ub85c
     * type \ubcc4 \ub514\uc2a4\ud328\uce58.
     */
    @GetMapping(value = "/main/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getMainStream() {
        Flux<ServerSentEvent<String>> dashboard = sinks.dashboardStream().map(o -> typedSse("dashboard", o));
        Flux<ServerSentEvent<String>> weather   = sinks.weatherStream()  .map(o -> typedSse("weather", o));
        Flux<ServerSentEvent<String>> air       = sinks.airStream()      .map(o -> typedSse("air", o));
        Flux<ServerSentEvent<String>> emergency = sinks.emergencyStream().map(o -> typedSse("emergency", o));
        Flux<ServerSentEvent<String>> traffic   = sinks.trafficStream()  .map(o -> typedSse("traffic", o));
        Flux<ServerSentEvent<String>> yeonhap   = sinks.yeonhapStream()  .map(o -> typedSse("yeonhap", o));

        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(HEARTBEAT_INTERVAL)
                .map(t -> ServerSentEvent.<String>builder().comment("keep-alive").build());

        return Flux.merge(dashboard, weather, air, emergency, traffic, yeonhap, heartbeat)
                .onErrorResume(e -> {
                    log.error("[main/stream] error: {}", e.getMessage());
                    return Flux.empty();
                })
                .doOnCancel(() -> log.debug("[main/stream] cancelled"));
    }

    /**
     * \ud1b5\ud569 \uc2a4\ud2b8\ub9bc\uc5d0\uc11c \uc4f0\ub294 typed event builder.
     */
    private ServerSentEvent<String> typedSse(String type, JsonObject obj) {
        return ServerSentEvent.<String>builder()
                .id(String.valueOf(System.currentTimeMillis()))
                .event(type)
                .data(obj.toString())
                .build();
    }
}
