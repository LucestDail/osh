package com.project.osh.controller;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.project.osh.service.DashboardSinks;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 통합 스트림({@code /dashboard/main/stream})의 <b>구독자별</b> 뉴스 키워드 필터 배선 테스트.
 *
 * <p>순수 필터({@code NewsKeywordFilterTest})가 통과해도 컨트롤러가 그것을 부르지 않으면
 * 기능은 없는 것이다 — 그 배선을 여기서 잡는다.
 *
 * <p>이 스트림이 화면이 실제로 쓰는 경로다(프런트 {@code main-dashboard.js} 가 여기에 붙는다).
 */
class DashboardControllerTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    private static JsonObject yeonhapWrapper(String... titles) {
        JsonArray items = new JsonArray();
        for (String t : titles) {
            JsonObject o = new JsonObject();
            o.addProperty("title", t);
            o.addProperty("content", "본문");
            items.add(o);
        }
        JsonObject data = new JsonObject();
        data.add("items", items);
        JsonObject root = new JsonObject();
        root.add("data", data);
        JsonObject w = new JsonObject();
        w.addProperty("yeonhapJson", root.toString());
        return w;
    }

    private static java.util.List<String> titlesIn(ServerSentEvent<String> ev) {
        JsonObject wrapper = JsonParser.parseString(ev.data()).getAsJsonObject();
        JsonObject root = JsonParser.parseString(wrapper.get("yeonhapJson").getAsString()).getAsJsonObject();
        return root.getAsJsonObject("data").getAsJsonArray("items").asList()
                .stream().map(e -> e.getAsJsonObject().get("title").getAsString()).toList();
    }

    /** 뉴스 이벤트만 골라낸다(통합 스트림엔 6종 + heartbeat 가 섞인다). */
    private static Flux<ServerSentEvent<String>> yeonhapOnly(Flux<ServerSentEvent<String>> stream) {
        return stream.filter(ev -> "yeonhap".equals(ev.event()));
    }

    @Test
    @DisplayName("키워드를 주면 매칭된 뉴스만 내려간다")
    void filtersNewsByKeyword() {
        DashboardSinks sinks = new DashboardSinks();
        sinks.pushYeonhap(yeonhapWrapper("금리 인상", "연예 소식", "반도체 수출"));

        StepVerifier.create(yeonhapOnly(new DashboardController(sinks).getMainStream("금리,반도체")))
                .assertNext(ev -> assertEquals(java.util.List.of("금리 인상", "반도체 수출"), titlesIn(ev)))
                .thenCancel()
                .verify(WAIT);
    }

    @Test
    @DisplayName("★키워드가 없으면 전부 내려간다 (오탐 금지 — 이 방향이 더 해롭다)")
    void noKeywordSendsEverything() {
        DashboardSinks sinks = new DashboardSinks();
        sinks.pushYeonhap(yeonhapWrapper("금리 인상", "연예 소식", "반도체 수출"));

        for (String param : new String[] { null, "", "   " }) {
            StepVerifier.create(yeonhapOnly(new DashboardController(sinks).getMainStream(param)))
                    .assertNext(ev -> assertEquals(
                            java.util.List.of("금리 인상", "연예 소식", "반도체 수출"), titlesIn(ev),
                            "키워드 파라미터 [" + param + "] 에서 전량 전송이 깨졌다"))
                    .thenCancel()
                    .verify(WAIT);
        }
    }

    @Test
    @DisplayName("구독자마다 자기 키워드로 걸러진다 — 한 싱크를 공유해도 섞이지 않는다")
    void eachSubscriberGetsItsOwnFilteredView() {
        DashboardSinks sinks = new DashboardSinks();
        sinks.pushYeonhap(yeonhapWrapper("금리 인상", "연예 소식", "반도체 수출"));
        DashboardController controller = new DashboardController(sinks);

        StepVerifier.create(yeonhapOnly(controller.getMainStream("금리")))
                .assertNext(ev -> assertEquals(java.util.List.of("금리 인상"), titlesIn(ev)))
                .thenCancel()
                .verify(WAIT);

        StepVerifier.create(yeonhapOnly(controller.getMainStream("연예")))
                .assertNext(ev -> assertEquals(java.util.List.of("연예 소식"), titlesIn(ev)))
                .thenCancel()
                .verify(WAIT);

        // 원본 싱크는 그대로 — 필터가 공유 상태를 건드리지 않았다.
        StepVerifier.create(yeonhapOnly(controller.getMainStream(null)))
                .assertNext(ev -> assertEquals(3, titlesIn(ev).size()))
                .thenCancel()
                .verify(WAIT);
    }

    @Test
    @DisplayName("뉴스 외 다른 이벤트는 키워드와 무관하게 그대로 간다")
    void otherEventTypesAreUntouched() {
        DashboardSinks sinks = new DashboardSinks();
        JsonObject weather = new JsonObject();
        weather.addProperty("weatherJson", "{}");
        sinks.pushWeather(weather);

        StepVerifier.create(new DashboardController(sinks).getMainStream("금리")
                        .filter(ev -> "weather".equals(ev.event())))
                .assertNext(ev -> assertTrue(ev.data().contains("weatherJson")))
                .thenCancel()
                .verify(WAIT);
    }
}
