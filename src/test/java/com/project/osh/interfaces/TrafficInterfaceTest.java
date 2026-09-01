package com.project.osh.interfaces;

import com.project.osh.util.ReactiveHttp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TrafficInterface(ITS 돌발정보) 단위 테스트 — 빈/누락 응답 시 안전한 빈 wrapper 폴백.
 */
class TrafficInterfaceTest {

    private static final String EMPTY = "{\"body\":{\"items\":[]}}";

    private ReactiveHttp http;
    private TrafficInterface traffic;

    @BeforeEach
    void setUp() {
        http = mock(ReactiveHttp.class);
        traffic = new TrafficInterface(http);
        ReflectionTestUtils.setField(traffic, "apiKey", "its-key");
    }

    @Test
    void passesThroughNonBlankBody() {
        String body = "{\"body\":{\"items\":[{\"eventType\":\"acc\"}]}}";
        when(http.getBlocking(anyString())).thenReturn(body);
        assertEquals(body, traffic.getTrafficInfo());
    }

    @Test
    void nullBodyFallsBackToEmpty() {
        when(http.getBlocking(anyString())).thenReturn(null);
        assertEquals(EMPTY, traffic.getTrafficInfo());
    }

    @Test
    void blankBodyFallsBackToEmpty() {
        when(http.getBlocking(anyString())).thenReturn("   ");
        assertEquals(EMPTY, traffic.getTrafficInfo());
    }
}
