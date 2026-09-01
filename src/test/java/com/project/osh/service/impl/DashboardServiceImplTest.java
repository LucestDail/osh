package com.project.osh.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.project.osh.config.OshProperties;
import com.project.osh.interfaces.AirInterface;
import com.project.osh.interfaces.InterfaceCore;
import com.project.osh.interfaces.WeatherInterface;
import com.project.osh.service.DashboardSinks;
import com.project.osh.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DashboardServiceImpl 단위 테스트 — 앱 상태 스냅샷, AirKorea 시도별 집계(평균/최악등급),
 * wrapper JSON 구조. 외부 API 는 목으로 대체(네트워크 없음). @PostConstruct 는 수동 미실행.
 */
class DashboardServiceImplTest {

    private NewsService newsService;
    private InterfaceCore interfaceCore;
    private WeatherInterface weatherInterface;
    private AirInterface airInterface;
    private OshProperties properties;
    private DashboardSinks sinks;
    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        newsService = mock(NewsService.class);
        interfaceCore = mock(InterfaceCore.class);
        weatherInterface = mock(WeatherInterface.class);
        airInterface = mock(AirInterface.class);
        properties = mock(OshProperties.class);
        sinks = mock(DashboardSinks.class);
        service = new DashboardServiceImpl(newsService, interfaceCore, weatherInterface,
                airInterface, properties, sinks);
    }

    @Test
    void applicationJsonHasRuntimeAndSystemFields() {
        JsonObject app = service.getApplicationJsonObject();
        assertTrue(app.has("currentTime"));
        assertTrue(app.has("serverNowMs"));
        assertTrue(app.has("systemArchitecture"));
        assertTrue(app.has("memory"));
        assertTrue(app.has("useMemory"));
        assertTrue(app.has("availableProcessors"));
        assertTrue(app.get("memory").getAsString().endsWith("MB"));
        assertTrue(app.get("availableProcessors").getAsInt() >= 1);
    }

    @Test
    void dashboardSnapshotExposesAllChannelKeys() {
        JsonObject snap = service.getDashboardSnapshot();
        assertTrue(snap.has("weatherJson"));
        assertTrue(snap.has("trafficJson"));
        assertTrue(snap.has("emergencyJson"));
        assertTrue(snap.has("yeonhapJson"));
        assertTrue(snap.has("applicationJson"));
    }

    @Test
    void weatherWrapperUsesWeatherJsonKey() {
        JsonObject w = service.getWeatherWrapperJson();
        assertTrue(w.has("weatherJson"));
    }

    @Test
    void airInfoAggregatesBySidoWithAverageAndWorstGrade() {
        String raw = "{\"response\":{\"body\":{\"items\":["
                + "{\"sidoName\":\"서울\",\"pm10Value\":\"40\",\"pm25Value\":\"20\",\"khaiGrade\":\"2\","
                + "\"stationName\":\"중구\",\"dataTime\":\"2026-09-01 10:00\"},"
                + "{\"sidoName\":\"서울\",\"pm10Value\":\"60\",\"pm25Value\":\"30\",\"khaiGrade\":\"3\","
                + "\"stationName\":\"강남\",\"dataTime\":\"2026-09-01 11:00\"}"
                + "]}}}";
        when(airInterface.getAirInfo()).thenReturn(raw);

        JsonObject air = service.getAirJsonObject();
        JsonArray sido = air.getAsJsonArray("sido");
        assertEquals(1, sido.size());
        JsonObject seoul = sido.get(0).getAsJsonObject();
        assertEquals("서울", seoul.get("name").getAsString());
        assertEquals(50, seoul.get("pm10").getAsInt());       // (40+60)/2
        assertEquals(25, seoul.get("pm25").getAsInt());       // (20+30)/2
        assertEquals(3, seoul.get("khaiGrade").getAsInt());   // 최악 등급
        assertEquals("강남", seoul.get("station").getAsString()); // 최신 dataTime 관측소
        assertEquals("2026-09-01 11:00", seoul.get("dataTime").getAsString());
    }

    @Test
    void airInfoWithMissingValuesUsesMinusOneSentinel() {
        String raw = "{\"response\":{\"body\":{\"items\":["
                + "{\"sidoName\":\"제주\",\"pm10Value\":\"-\",\"pm25Value\":\"\",\"khaiGrade\":\"0\"}"
                + "]}}}";
        when(airInterface.getAirInfo()).thenReturn(raw);

        JsonObject air = service.getAirJsonObject();
        JsonObject jeju = air.getAsJsonArray("sido").get(0).getAsJsonObject();
        assertEquals(-1, jeju.get("pm10").getAsInt());  // 유효 측정치 0건 → -1 센티넬
        assertEquals(-1, jeju.get("pm25").getAsInt());
    }

    @Test
    void airInfoMalformedResponseYieldsEmptySidoArray() {
        when(airInterface.getAirInfo()).thenReturn("not json at all");
        JsonObject air = service.getAirJsonObject();
        assertNotNull(air);
        assertEquals(0, air.getAsJsonArray("sido").size());
    }
}
