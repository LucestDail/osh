package com.project.osh.interfaces;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.ReactiveHttp;

import reactor.core.publisher.Mono;

@Component
public class WeatherInterface {

    private static final Logger log = LoggerFactory.getLogger(WeatherInterface.class);
    private static final String OPENWEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String KMA_VILAGE_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
    private static final String EMPTY_KMA = "{\"response\":{\"body\":{\"items\":{\"item\":[]}}}}";

    /** KMA \ub2e8\uae30\uc608\ubcf4 \ubc1c\ud45c\uc2dc\uac01: 02,05,08,11,14,17,20,23 */
    private static final int[] KMA_BASE_HOURS = {23, 20, 17, 14, 11, 8, 5, 2};

    @Value("${osh.api.openweather.key}")
    private String apiKey;

    @Value("${osh.api.kma.key:}")
    private String kmaKey;

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

    /**
     * \uae30\uc0c1\uccad \ub2e8\uae30\uc608\ubcf4 (\uc624\ub298 \uc774\ud6c4 \uc57d 3\uc77c \ub300\uc0c1, 3\uc2dc\uac04 \uac04\uaca9).
     * \uc6d0\uc2dc \uc751\ub2f5 \uadf8\ub300\ub85c \ubc18\ud658 \u2014 \uc11c\ube44\uc2a4 \uacc4\uce35\uc5d0\uc11c TMP/POP/SKY \ub4f1\uc744 \ucd94\ucd9c.
     * \ud0a4 \ubd80\uc7ac \uc2dc \ube48 \uc751\ub2f5 wrapper \ubc18\ud658.
     */
    public Mono<String> fetchShortForecast(int nx, int ny) {
        if (kmaKey == null || kmaKey.isBlank()) {
            return Mono.just(EMPTY_KMA);
        }
        try {
            String[] base = computeBaseDateTime();
            String url = KMA_VILAGE_URL
                    + "?serviceKey=" + URLEncoder.encode(kmaKey, StandardCharsets.UTF_8)
                    + "&pageNo=1&numOfRows=1000&dataType=JSON"
                    + "&base_date=" + base[0]
                    + "&base_time=" + base[1]
                    + "&nx=" + nx
                    + "&ny=" + ny;
            return http.get(url)
                    .defaultIfEmpty(EMPTY_KMA)
                    .onErrorResume(e -> {
                        log.warn("KMA \uc870\ud68c \uc2e4\ud328 nx={}, ny={}: {}", nx, ny, e.getMessage());
                        return Mono.just(EMPTY_KMA);
                    });
        } catch (Exception e) {
            log.error("KMA URL \uc0dd\uc131 \uc2e4\ud328: {}", e.getMessage());
            return Mono.just(EMPTY_KMA);
        }
    }

    /**
     * \uae30\uc0c1\uccad \ub2e8\uae30\uc608\ubcf4 \uad6c\uac04\uc740 \ubc1c\ud45c \uc644\ub8cc \ud6c4 \uc57d 10\ubd84 \uc774\ud6c4\ubd80\ud130 \uc751\ub2f5\ud574\uc11c
     * \uba85\ud655\ud788 \uc774\uc804 \ubc1c\ud45c\uc2dc\uac01\uc744 \uc120\ud0dd\ud55c\ub2e4. (\ucd5c\uc18c\ud55c \ud604\uc7ac\ucc28-1 \ubc1c\ud45c).
     */
    private String[] computeBaseDateTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(15);
        int hour = now.getHour();
        int base = 2;
        for (int h : KMA_BASE_HOURS) {
            if (hour >= h) { base = h; break; }
        }
        if (hour < 2) {
            now = now.minusDays(1);
            base = 23;
        }
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = String.format("%02d00", base);
        return new String[] { date, time };
    }
}
