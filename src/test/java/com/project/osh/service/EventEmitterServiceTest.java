package com.project.osh.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.CapturingEmitterHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.osh.model.News;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EventEmitterService} 브로드캐스트의 <b>구독자별</b> 키워드 필터 배선 테스트.
 *
 * <p>emitter 에 실제로 실린 payload 를 본다 — {@code initialize(Handler)} 로 붙기 전
 * {@code send} 는 버퍼에 쌓였다가 붙는 순간 handler 로 흘러나온다.
 */
class EventEmitterServiceTest {

    /** 브로드캐스트로 실린 뉴스 제목(SSE 골격 문자열은 버린다). */
    private static List<String> broadcastTitles(CapturingEmitterHandler h) {
        return h.flattenedLists().stream()
                .filter(News.class::isInstance)
                .map(o -> ((News) o).getNewsTitle())
                .toList();
    }

    private static News news(String title) {
        News n = new News();
        n.setNewsTitle(title);
        n.setNewsContents("본문");
        return n;
    }

    private static final List<News> ALL = List.of(
            news("금리 인상"), news("연예 소식"), news("반도체 수출"));

    private static CapturingEmitterHandler attach(SseEmitter emitter) throws IOException {
        return CapturingEmitterHandler.attach(emitter);
    }

    @Test
    @DisplayName("키워드를 등록한 구독자는 매칭된 뉴스만 받는다")
    void subscriberWithKeywordsGetsOnlyMatching() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        CapturingEmitterHandler h = attach(svc.createEmitter(List.of("금리")));

        svc.broadcastNewsUpdate(ALL);

        assertEquals(List.of("금리 인상"), broadcastTitles(h));
    }

    @Test
    @DisplayName("★키워드 없이 구독하면 전부 받는다 (오탐 금지)")
    void subscriberWithoutKeywordsGetsEverything() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        CapturingEmitterHandler viaNoArg = attach(svc.createEmitter());
        CapturingEmitterHandler viaEmpty = attach(svc.createEmitter(List.of()));
        CapturingEmitterHandler viaNull = attach(svc.createEmitter(null));

        svc.broadcastNewsUpdate(ALL);

        List<String> all = List.of("금리 인상", "연예 소식", "반도체 수출");
        assertEquals(all, broadcastTitles(viaNoArg), "기존 무인자 createEmitter() 가 전량 전송을 잃었다");
        assertEquals(all, broadcastTitles(viaEmpty), "빈 키워드 목록이 전량 전송을 잃었다");
        assertEquals(all, broadcastTitles(viaNull), "null 키워드가 전량 전송을 잃었다");
    }

    @Test
    @DisplayName("구독자별로 다른 키워드가 섞이지 않는다")
    void subscribersDoNotLeakIntoEachOther() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        CapturingEmitterHandler a = attach(svc.createEmitter(List.of("금리")));
        CapturingEmitterHandler b = attach(svc.createEmitter(List.of("반도체")));
        CapturingEmitterHandler c = attach(svc.createEmitter(List.of()));

        svc.broadcastNewsUpdate(ALL);

        assertEquals(List.of("금리 인상"), broadcastTitles(a));
        assertEquals(List.of("반도체 수출"), broadcastTitles(b));
        assertEquals(3, broadcastTitles(c).size());
    }

    @Test
    @DisplayName("★필터는 원본 목록을 건드리지 않는다 — 한 구독자의 필터가 다음 구독자를 굶기면 안 된다")
    void broadcastDoesNotMutateSourceList() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        List<News> source = new ArrayList<>(ALL);
        attach(svc.createEmitter(List.of("금리")));
        CapturingEmitterHandler after = attach(svc.createEmitter(List.of()));

        svc.broadcastNewsUpdate(source);

        assertEquals(3, source.size(), "브로드캐스트가 원본 목록을 줄였다");
        assertEquals(3, broadcastTitles(after).size());
    }

    @Test
    @DisplayName("매칭 0건이면 빈 목록을 보낸다 — 안 보내면 옛 뉴스가 화면에 남는다")
    void noMatchStillSendsEmptySnapshot() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        CapturingEmitterHandler h = attach(svc.createEmitter(List.of("존재하지않는키워드")));

        svc.broadcastNewsUpdate(ALL);

        assertEquals(List.of(), broadcastTitles(h));
        assertTrue(h.sentAnyList(),
                "빈 스냅샷조차 전송되지 않았다 — 클라이언트가 옛 목록을 계속 들고 있게 된다");
    }

    @Test
    @DisplayName("연결 수 집계는 종전대로 — 키워드 구독도 똑같이 센다")
    void connectionCountIsTracked() {
        EventEmitterService svc = new EventEmitterService();
        assertEquals(0, svc.getActiveConnectionCount());
        svc.createEmitter(List.of("금리"));
        svc.createEmitter();
        assertEquals(2, svc.getActiveConnectionCount());
    }

    @Test
    @DisplayName("★연결이 끊기면 구독자가 목록에서 빠진다 — 키워드를 달고도 새지 않는다")
    void completedSubscriberIsRemoved() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        SseEmitter withKeywords = svc.createEmitter(List.of("금리"));
        CapturingEmitterHandler gone = attach(withKeywords);
        CapturingEmitterHandler stays = attach(svc.createEmitter(List.of()));
        assertEquals(2, svc.getActiveConnectionCount());

        gone.fireCompletion(); // 스프링이 비동기 요청 종료 때 하는 일

        assertEquals(1, svc.getActiveConnectionCount());

        svc.broadcastNewsUpdate(ALL);
        assertEquals(List.of(), broadcastTitles(gone), "끊긴 구독자에게 계속 보내고 있다");
        assertEquals(3, broadcastTitles(stays).size());
    }

    @Test
    @DisplayName("구독자가 없으면 브로드캐스트는 조용히 지나간다")
    void broadcastWithNoSubscribersIsNoop() {
        EventEmitterService svc = new EventEmitterService();
        svc.broadcastNewsUpdate(ALL);
        assertEquals(0, svc.getActiveConnectionCount());
    }

    @Test
    @DisplayName("null 뉴스도 NPE 없이 빈 스냅샷으로 나간다")
    void nullNewsIsSafe() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        CapturingEmitterHandler h = attach(svc.createEmitter(List.of("금리")));
        svc.broadcastNewsUpdate(null);
        assertEquals(List.of(), broadcastTitles(h));
    }

    @Test
    @DisplayName("createEmitter() 와 createEmitter(List.of()) 는 같은 동작이어야 한다")
    void noArgOverloadIsEquivalentToEmptyKeywords() throws Exception {
        EventEmitterService svc = new EventEmitterService();
        CapturingEmitterHandler a = attach(svc.createEmitter());
        CapturingEmitterHandler b = attach(svc.createEmitter(List.of()));
        svc.broadcastNewsUpdate(ALL);
        assertEquals(broadcastTitles(a), broadcastTitles(b));
        assertFalse(broadcastTitles(a).isEmpty());
    }
}
