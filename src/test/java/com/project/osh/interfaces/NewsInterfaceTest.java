package com.project.osh.interfaces;

import com.project.osh.util.ReactiveHttp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NewsInterface(안전데이터 연합뉴스) 단위 테스트 — null 응답 시 빈 문자열 폴백.
 */
class NewsInterfaceTest {

    private ReactiveHttp http;
    private NewsInterface news;

    @BeforeEach
    void setUp() {
        http = mock(ReactiveHttp.class);
        news = new NewsInterface(http);
        ReflectionTestUtils.setField(news, "apiKey", "news-key");
    }

    @Test
    void passesThroughBody() {
        when(http.getBlocking(anyString())).thenReturn("{\"body\":[]}");
        assertEquals("{\"body\":[]}", news.getYeonhapNews());
    }

    @Test
    void nullBodyBecomesEmptyString() {
        when(http.getBlocking(anyString())).thenReturn(null);
        assertEquals("", news.getYeonhapNews());
    }

    @Test
    void urlContainsServiceKeyAndTodayInquiryDate() {
        when(http.getBlocking(anyString())).thenReturn("");
        news.getYeonhapNews();
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(http).getBlocking(url.capture());
        assertTrue(url.getValue().contains("serviceKey=news-key"));
        assertTrue(url.getValue().contains("inqDt="));
    }
}
