package com.project.osh.interfaces;

import com.project.osh.util.ReactiveHttp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AirInterface(에어코리아 시도별 실시간 측정) 단위 테스트.
 * ReactiveHttp 를 목으로 대체하고 @Value 키는 리플렉션으로 주입 — 네트워크 없이 검증.
 */
class AirInterfaceTest {

    private static final String EMPTY = "{\"response\":{\"body\":{\"items\":[]}}}";

    private ReactiveHttp http;
    private AirInterface air;

    @BeforeEach
    void setUp() {
        http = mock(ReactiveHttp.class);
        air = new AirInterface(http);
    }

    @Test
    void blankKeyReturnsEmptyResponseWithoutCallingHttp() {
        ReflectionTestUtils.setField(air, "apiKey", "");
        assertEquals(EMPTY, air.fetchAll().block());
        verify(http, never()).get(anyString());
    }

    @Test
    void nullKeyReturnsEmptyResponse() {
        ReflectionTestUtils.setField(air, "apiKey", null);
        assertEquals(EMPTY, air.fetchAll().block());
        verify(http, never()).get(anyString());
    }

    @Test
    void validKeyPassesThroughBody() {
        ReflectionTestUtils.setField(air, "apiKey", "SECRET+KEY/1");
        String body = "{\"response\":{\"body\":{\"items\":[{\"sidoName\":\"서울\"}]}}}";
        when(http.get(anyString())).thenReturn(Mono.just(body));
        assertEquals(body, air.fetchAll().block());
    }

    @Test
    void urlIsBuiltWithEncodedKeyAndJsonReturnType() {
        ReflectionTestUtils.setField(air, "apiKey", "a b+c");
        when(http.get(anyString())).thenReturn(Mono.just("{}"));
        air.fetchAll().block();
        // URLEncoder.encode 는 form 인코딩(공백→'+', '+'→%2B). returnType=json 도 붙어야 함
        verify(http).get(contains("serviceKey=a+b%2Bc"));
        verify(http).get(contains("returnType=json"));
    }

    @Test
    void emptyHttpMonoFallsBackToEmptyResponse() {
        ReflectionTestUtils.setField(air, "apiKey", "K");
        when(http.get(anyString())).thenReturn(Mono.empty());
        assertEquals(EMPTY, air.fetchAll().block());
    }

    @Test
    void httpErrorFallsBackToEmptyResponse() {
        ReflectionTestUtils.setField(air, "apiKey", "K");
        when(http.get(anyString())).thenReturn(Mono.error(new RuntimeException("500 boom")));
        assertEquals(EMPTY, air.fetchAll().block());
    }

    @Test
    void getAirInfoBlockingWrapsEmptyKey() {
        ReflectionTestUtils.setField(air, "apiKey", "");
        assertEquals(EMPTY, air.getAirInfo());
    }
}
