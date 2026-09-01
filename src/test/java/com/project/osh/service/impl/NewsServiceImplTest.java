package com.project.osh.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.project.osh.model.News;
import com.project.osh.repository.news.NewsRepository;
import com.project.osh.service.EventEmitterService;
import com.project.osh.service.MyapiNewsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NewsServiceImpl 단위 테스트 — 캐시 JSON 직렬화, 회사별 조회 폴백,
 * myapi 우선 소비 → MySQL 폴백, 갱신 시 SSE 브로드캐스트/캐시 유지 로직.
 * Spring 컨텍스트 없이 목 주입(리플렉션)으로만 검증.
 */
class NewsServiceImplTest {

    private NewsRepository repo;
    private MyapiNewsClient myapiClient;
    private EventEmitterService emitter;
    private NewsServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(NewsRepository.class);
        myapiClient = mock(MyapiNewsClient.class);
        emitter = mock(EventEmitterService.class);
        service = new NewsServiceImpl();
        ReflectionTestUtils.setField(service, "newsRepository", repo);
        ReflectionTestUtils.setField(service, "myapiNewsClient", myapiClient);
        ReflectionTestUtils.setField(service, "eventEmitterService", emitter);
    }

    private static News news(String company, String title, LocalDateTime dt) {
        News n = new News();
        n.setNewsCompany(company);
        n.setNewsTitle(title);
        n.setNewsContents("body-" + title);
        n.setNewsCreateDT(dt);
        return n;
    }

    private void setCache(List<News> list) {
        ReflectionTestUtils.setField(service, "cachedNews", new CopyOnWriteArrayList<>(list));
    }

    @Test
    void getCachedNewsSerializesEntitiesWithSeoulTimestamp() {
        setCache(List.of(news("연합", "제목", LocalDateTime.of(2026, 9, 1, 8, 30, 0))));
        JsonArray arr = service.getCachedNews();
        assertEquals(1, arr.size());
        JsonObject o = arr.get(0).getAsJsonObject();
        assertEquals("연합", o.get("company").getAsString());
        assertEquals("제목", o.get("title").getAsString());
        assertEquals("body-제목", o.get("content").getAsString());
        assertEquals("2026-09-01 08:30:00", o.get("createDT").getAsString());
        assertTrue(o.get("createDTMs").getAsLong() > 0);
    }

    @Test
    void getCachedNewsHandlesNullCreateDate() {
        setCache(List.of(news("뉴시스", "무날짜", null)));
        JsonObject o = service.getCachedNews().get(0).getAsJsonObject();
        assertEquals("", o.get("createDT").getAsString());
        assertEquals(0, o.get("createDTMs").getAsLong());
    }

    @Test
    void getCachedNewsEmptyReturnsPlaceholder() {
        setCache(List.of());
        JsonArray arr = service.getCachedNews();
        assertEquals(1, arr.size());
        // 데이터 없을 때도 null 대신 안내 카드 1개
        assertEquals("데이터를 불러오는 중입니다...", arr.get(0).getAsJsonObject().get("title").getAsString());
    }

    @Test
    void getNewsByCompanyReturnsRepositoryResult() {
        List<News> expected = List.of(news("연합", "t", LocalDateTime.now()));
        when(repo.findTop100ByNewsCompanyOrderByNewsCreateDTDesc("연합")).thenReturn(expected);
        assertEquals(expected, service.getNewsByCompany("연합"));
    }

    @Test
    void getNewsByCompanyReturnsEmptyOnRepositoryException() {
        when(repo.findTop100ByNewsCompanyOrderByNewsCreateDTDesc("x"))
                .thenThrow(new RuntimeException("db down"));
        assertTrue(service.getNewsByCompany("x").isEmpty());
    }

    @Test
    void updateNewsDataPrefersMyapiAndBroadcasts() {
        List<News> fresh = List.of(news("연합", "새뉴스", LocalDateTime.now()));
        when(myapiClient.fetchNews()).thenReturn(fresh);

        service.updateNewsData();

        assertEquals(fresh, service.getAllNews());
        verify(emitter).broadcastNewsUpdate(fresh);
        verify(repo, never()).findTop100OrderByNewsCreateDTDesc();
    }

    @Test
    void updateNewsDataFallsBackToRepositoryWhenMyapiReturnsNull() {
        when(myapiClient.fetchNews()).thenReturn(null);
        List<News> fromDb = List.of(news("뉴시스", "db뉴스", LocalDateTime.now()));
        when(repo.findTop100OrderByNewsCreateDTDesc()).thenReturn(fromDb);

        service.updateNewsData();

        assertEquals(fromDb, service.getAllNews());
        verify(emitter).broadcastNewsUpdate(fromDb);
    }

    @Test
    void updateNewsDataKeepsExistingCacheWhenNoDataAvailable() {
        List<News> existing = List.of(news("연합", "기존", LocalDateTime.now()));
        setCache(existing);
        when(myapiClient.fetchNews()).thenReturn(null);
        when(repo.findTop100OrderByNewsCreateDTDesc()).thenReturn(List.of());

        service.updateNewsData();

        // 새 데이터가 없으면 기존 캐시 유지, 브로드캐스트 안 함
        assertEquals(1, service.getAllNews().size());
        assertEquals("기존", service.getAllNews().get(0).getNewsTitle());
        verify(emitter, never()).broadcastNewsUpdate(org.mockito.ArgumentMatchers.anyList());
    }
}
