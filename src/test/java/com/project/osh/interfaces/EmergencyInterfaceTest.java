package com.project.osh.interfaces;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.osh.util.ReactiveHttp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EmergencyInterface(안전데이터 긴급재난문자, 3일치 zip 병합) 단위 테스트.
 * http.get 은 오늘/어제/그제 3회 호출되며 각 응답의 body 배열을 합친다.
 */
class EmergencyInterfaceTest {

    private ReactiveHttp http;
    private EmergencyInterface emergency;

    @BeforeEach
    void setUp() {
        http = mock(ReactiveHttp.class);
        emergency = new EmergencyInterface(http);
        ReflectionTestUtils.setField(emergency, "apiKey", "test-key");
    }

    @Test
    void mergesBodyArraysAcrossThreeDays() {
        String payload = "{\"body\":[{\"MSG_CN\":\"경보\"}]}";
        when(http.get(anyString())).thenReturn(Mono.just(payload));

        String result = emergency.getEmergencyInfo();
        JsonObject o = JsonParser.parseString(result).getAsJsonObject();
        JsonArray items = o.getAsJsonArray("items");
        // 3일 × 1건 = 3건 병합
        assertEquals(3, items.size());
        assertEquals("경보", items.get(0).getAsJsonObject().get("MSG_CN").getAsString());
    }

    @Test
    void payloadWithoutBodyContributesNothing() {
        when(http.get(anyString())).thenReturn(Mono.just("{\"header\":{\"resultCode\":\"00\"}}"));
        String result = emergency.getEmergencyInfo();
        JsonObject o = JsonParser.parseString(result).getAsJsonObject();
        assertEquals(0, o.getAsJsonArray("items").size());
    }

    @Test
    void malformedPayloadIsSkippedNotFatal() {
        when(http.get(anyString())).thenReturn(Mono.just("<<not json>>"));
        String result = emergency.getEmergencyInfo();
        JsonObject o = JsonParser.parseString(result).getAsJsonObject();
        // 파싱 실패해도 빈 items 로 정상 응답
        assertTrue(o.has("items"));
        assertEquals(0, o.getAsJsonArray("items").size());
    }

    @Test
    void emptyPayloadYieldsEmptyItems() {
        when(http.get(anyString())).thenReturn(Mono.empty());
        String result = emergency.getEmergencyInfo();
        JsonObject o = JsonParser.parseString(result).getAsJsonObject();
        assertEquals(0, o.getAsJsonArray("items").size());
    }

    @Test
    void upstreamErrorFallsBackToEmptyResponse() {
        when(http.get(anyString())).thenReturn(Mono.error(new RuntimeException("gateway down")));
        String result = emergency.getEmergencyInfo();
        assertEquals("{\"items\":[]}", result);
    }
}
