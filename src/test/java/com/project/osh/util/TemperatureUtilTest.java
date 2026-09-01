package com.project.osh.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 온도 단위 변환 단위 테스트.
 * getTemp(value, unit) 는 항상 섭씨(C)로 내부 상태를 세팅하고, getInC/F/K 로 재변환한다.
 */
class TemperatureUtilTest {

    private static final double EPS = 1e-9;

    @Test
    void celsiusInputPassesThrough() {
        TemperatureUtil t = new TemperatureUtil();
        assertEquals(25.0, t.getTemp(25.0, 'C'), EPS);
        assertEquals(25.0, t.getInC(), EPS);
    }

    @Test
    void kelvinConvertsToCelsius() {
        TemperatureUtil t = new TemperatureUtil();
        // 298.15K → 25C (openweather main.temp 가 켈빈으로 온다)
        assertEquals(25.0, t.getTemp(298.15, 'K'), EPS);
    }

    @Test
    void kelvinAbsoluteZeroBoundary() {
        TemperatureUtil t = new TemperatureUtil();
        // 0K = -273.15C
        assertEquals(-273.15, t.getTemp(0.0, 'K'), EPS);
    }

    @Test
    void fahrenheitFreezingPointIsZeroCelsius() {
        TemperatureUtil t = new TemperatureUtil();
        assertEquals(0.0, t.getTemp(32.0, 'F'), EPS);
    }

    @Test
    void fahrenheitBoilingPointIsHundredCelsius() {
        TemperatureUtil t = new TemperatureUtil();
        assertEquals(100.0, t.getTemp(212.0, 'F'), EPS);
    }

    @Test
    void unknownUnitReturnsRawInput() {
        TemperatureUtil t = new TemperatureUtil();
        // getTemp 는 switch 이전에 this.value=value 를 먼저 세팅 → 미지원 단위는 원값 그대로
        assertEquals(123.0, t.getTemp(123.0, 'X'), EPS);
    }

    @Test
    void reconvertHelpersRoundTripFromCelsius() {
        TemperatureUtil t = new TemperatureUtil();
        t.getTemp(0.0, 'C');            // value = 0C
        assertEquals(32.0, t.getInF(), EPS);   // 0C → 32F
        assertEquals(273.15, t.getInK(), EPS); // 0C → 273.15K

        t.getTemp(100.0, 'C');
        assertEquals(212.0, t.getInF(), EPS);
    }
}
