package com.project.osh.interfaces;

import com.project.osh.util.ReactiveHttp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WeatherInterface(OpenWeather + KMA 단기예보) 단위 테스트.
 */
class WeatherInterfaceTest {

    private static final String EMPTY_KMA = "{\"response\":{\"body\":{\"items\":{\"item\":[]}}}}";

    private ReactiveHttp http;
    private WeatherInterface weather;

    @BeforeEach
    void setUp() {
        http = mock(ReactiveHttp.class);
        weather = new WeatherInterface(http);
        ReflectionTestUtils.setField(weather, "apiKey", "owm-key");
        ReflectionTestUtils.setField(weather, "kmaKey", "");
    }

    @Test
    void fetchWeatherBuildsUrlWithLatLonAndAppid() {
        when(http.get(anyString())).thenReturn(Mono.just("{\"main\":{\"temp\":298.15}}"));
        String r = weather.fetchWeather("37.5", "127.0").block();
        assertEquals("{\"main\":{\"temp\":298.15}}", r);

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(http).get(url.capture());
        assertTrue(url.getValue().contains("lat=37.5"));
        assertTrue(url.getValue().contains("lon=127.0"));
        assertTrue(url.getValue().contains("appid=owm-key"));
    }

    @Test
    void fetchWeatherEmptyMonoDefaultsToEmptyString() {
        when(http.get(anyString())).thenReturn(Mono.empty());
        assertEquals("", weather.fetchWeather("1", "2").block());
    }

    @Test
    void getOpenweathermapUsesDefaultCoordinates() {
        when(http.get(anyString())).thenReturn(Mono.just("body"));
        assertEquals("body", weather.getOpenweathermap());
        verify(http).get(anyString());
    }

    @Test
    void shortForecastWithBlankKmaKeyReturnsEmptyWrapperWithoutHttp() {
        assertEquals(EMPTY_KMA, weather.fetchShortForecast(60, 127).block());
        verify(http, never()).get(anyString());
    }

    @Test
    void shortForecastPassesThroughBodyWhenKeyPresent() {
        ReflectionTestUtils.setField(weather, "kmaKey", "kma-key");
        String body = "{\"response\":{\"body\":{\"items\":{\"item\":[{\"category\":\"TMP\"}]}}}}";
        when(http.get(anyString())).thenReturn(Mono.just(body));
        assertEquals(body, weather.fetchShortForecast(60, 127).block());
    }

    @Test
    void shortForecastHttpErrorFallsBackToEmptyWrapper() {
        ReflectionTestUtils.setField(weather, "kmaKey", "kma-key");
        when(http.get(anyString())).thenReturn(Mono.error(new RuntimeException("timeout")));
        assertEquals(EMPTY_KMA, weather.fetchShortForecast(60, 127).block());
    }

    @Test
    void shortForecastUrlCarriesNxNyAndBaseDateTime() {
        ReflectionTestUtils.setField(weather, "kmaKey", "kma-key");
        when(http.get(anyString())).thenReturn(Mono.just(EMPTY_KMA));
        weather.fetchShortForecast(55, 127).block();

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(http).get(url.capture());
        assertTrue(url.getValue().contains("nx=55"));
        assertTrue(url.getValue().contains("ny=127"));
        assertTrue(url.getValue().contains("base_date="));
        assertTrue(url.getValue().contains("base_time="));
    }
}
