package com.project.osh.service;

import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * SSE \uad6c\ub3c5\uc790\uc5d0\uac8c push \ud558\uae30 \uc704\ud55c \uacf5\uc6a9 \uc2f1\ud06c \uc800\ub2c8\ub3c0.
 * - {@link Sinks.Many} replay().latest(): \uc0c8 \uad6c\ub3c5\uc790\uc5d0\uac8c \uc989\uc2dc \ucd5c\uc2e0 \uac12 1\uac1c \uc81c\uacf5.
 * - serializeOnly(): \ub9c8\uc774\uadf8\ub808\uc774\uc158 \uac10\uc758 \uac15\ud55c \uc218\uc900\uc758 emit \uacbd\uc7c1 \ubc29\uc9c0.
 *
 * \uc774\uc804: Flux.interval(1s) \uba87\ud504 \u00d7 4 \uac1c\uac00 \ub9e4 tick\ub9c8\ub2e4 \ub3d9\uc77c payload \uc9c1\ub82c\ud654 push.
 * \ud604\uc7ac: \ub370\uc774\ud130 \uac31\uc2e0 \uc2dc\uc810\uc5d0\ub9cc push + 30s heartbeat (SSE \ucee8\ub125\uc158 \uc720\uc9c0).
 */
@Component
public class DashboardSinks {

    private final Sinks.Many<JsonObject> dashboardSink = Sinks.many().replay().latest();
    private final Sinks.Many<JsonObject> emergencySink = Sinks.many().replay().latest();
    private final Sinks.Many<JsonObject> trafficSink = Sinks.many().replay().latest();
    private final Sinks.Many<JsonObject> yeonhapSink = Sinks.many().replay().latest();

    public void pushDashboard(JsonObject obj) {
        if (obj != null) dashboardSink.tryEmitNext(obj);
    }

    public void pushEmergency(JsonObject obj) {
        if (obj != null) emergencySink.tryEmitNext(obj);
    }

    public void pushTraffic(JsonObject obj) {
        if (obj != null) trafficSink.tryEmitNext(obj);
    }

    public void pushYeonhap(JsonObject obj) {
        if (obj != null) yeonhapSink.tryEmitNext(obj);
    }

    public Flux<JsonObject> dashboardStream() {
        return dashboardSink.asFlux();
    }

    public Flux<JsonObject> emergencyStream() {
        return emergencySink.asFlux();
    }

    public Flux<JsonObject> trafficStream() {
        return trafficSink.asFlux();
    }

    public Flux<JsonObject> yeonhapStream() {
        return yeonhapSink.asFlux();
    }
}
