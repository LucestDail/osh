package com.project.osh.interfaces;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.ReactiveHttp;

import reactor.core.publisher.Mono;

@Component
public class WeatherInterface {

    private static final String OPENWEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";

    @Value("${osh.api.openweather.key}")
    private String apiKey;

    private final ReactiveHttp http;

    public WeatherInterface(ReactiveHttp http) {
        this.http = http;
    }

    public Mono<String> fetchWeather(String lat, String lon) {
        String url = OPENWEATHER_URL + "?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey;
        return http.get(url).defaultIfEmpty("");
    }

    public String getOpenweathermap() {
        return getOpenweathermap("37.245807", "127.057375");
    }

    public String getOpenweathermap(String lat, String lon) {
        return fetchWeather(lat, lon).blockOptional().orElse("");
    }
}
