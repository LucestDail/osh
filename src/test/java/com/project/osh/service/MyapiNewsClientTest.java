package com.project.osh.service;

import com.project.osh.model.News;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * myapi /api/social/news 응답 → OSH News 매핑 단위 테스트.
 * DB/Spring 컨텍스트 없이 순수 매핑 로직만 검증(폴백 신호 포함).
 */
class MyapiNewsClientTest {

    private final MyapiNewsClient client = new MyapiNewsClient();

    @Test
    void mapsMyapiItemsToNewsEntities() {
        String body = "{\"data\":{\"items\":["
                + "{\"createDT\":\"2026-08-27 10:30:00\",\"company\":\"연합뉴스\",\"companyCode\":\"YNA\","
                + "\"title\":\"제목1\",\"content\":\"본문1\",\"link\":\"https://ex.com/1\",\"reporter\":\"홍길동\"},"
                + "{\"createDT\":\"2026-08-27 09:00:00\",\"company\":\"뉴시스\",\"companyCode\":\"NSS\","
                + "\"title\":\"제목2\",\"content\":\"본문2\",\"link\":\"https://ex.com/2\",\"reporter\":\"김철수\"}"
                + "]}}";

        List<News> result = client.parse(body);

        assertNotNull(result);
        assertEquals(2, result.size());

        News first = result.get(0);
        assertEquals("연합뉴스", first.getNewsCompany());
        assertEquals("제목1", first.getNewsTitle());
        assertEquals("본문1", first.getNewsContents());
        assertEquals("https://ex.com/1", first.getEtc1());  // link → ETC1
        assertEquals("홍길동", first.getNewsFrom());
        assertEquals(LocalDateTime.of(2026, 8, 27, 10, 30, 0), first.getNewsCreateDT());

        // getCachedNews() 가 기대하는 필드(company/title/content/createDT)가 모두 채워짐 = SSE/대시보드 호환
        News second = result.get(1);
        assertEquals("뉴시스", second.getNewsCompany());
        assertEquals(LocalDateTime.of(2026, 8, 27, 9, 0, 0), second.getNewsCreateDT());
    }

    @Test
    void handlesMissingAndNullFieldsGracefully() {
        String body = "{\"data\":{\"items\":["
                + "{\"title\":\"제목만\",\"createDT\":\"\",\"company\":null}"
                + "]}}";

        List<News> result = client.parse(body);

        assertNotNull(result);
        assertEquals(1, result.size());
        News n = result.get(0);
        assertEquals("제목만", n.getNewsTitle());
        assertNull(n.getNewsCreateDT());   // 빈 날짜 문자열 → null (파싱 안전)
        assertNull(n.getNewsCompany());
        assertNull(n.getEtc1());
    }

    @Test
    void emptyItemsYieldsEmptyList() {
        List<News> result = client.parse("{\"data\":{\"items\":[]}}");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void malformedOrUnexpectedShapeSignalsFallback() {
        // data 없음 → null (호출측이 MySQL 폴백)
        assertNull(client.parse("{\"foo\":\"bar\"}"));
        // items 없음 → null
        assertNull(client.parse("{\"data\":{}}"));
        // 최상위가 객체가 아님 → null
        assertNull(client.parse("[1,2,3]"));
        // 빈/공백 본문 → null
        assertNull(client.parse(""));
        assertNull(client.parse(null));
    }
}
