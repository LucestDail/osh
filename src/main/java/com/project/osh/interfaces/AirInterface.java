package com.project.osh.interfaces;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.osh.util.ReactiveHttp;

import reactor.core.publisher.Mono;

/**
 * 한국환경공단 에어코리아 시도별 실시간 측정정보.
 * https://www.data.go.kr/data/15073861/openapi.do
 *
 * 응답 예시 (JSON): { "response": { "body": { "items": [ {sidoName, stationName, pm10Value, pm25Value, khaiGrade, ...}, ... ] } } }
 *
 * 키 부재 시 빈 응답 wrapper 를 반환 — 운영 코드가 키 미설정 상태에서도 안전하게 동작.
 */
@Component
public class AirInterface {

    private static final Logger log = LoggerFactory.getLogger(AirInterface.class);
    private static final String AIRKOREA_URL =
            "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty";
    private static final String EMPTY_RESPONSE = "{\"response\":{\"body\":{\"items\":[]}}}";

    @Value("${osh.api.airkorea.key:}")
    private String apiKey;

    private final ReactiveHttp http;

    public AirInterface(ReactiveHttp http) {
        this.http = http;
    }

    /**
     * 전국 모든 시도 측정값 (전국 = sidoName 빈 값 → 1000건 페이지).
     * AirKorea 는 sidoName 파라미터를 받지만 '전국' 으로도 호출 가능.
     */
    public Mono<String> fetchAll() {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("airkorea api key 비어있음 — 빈 응답 반환");
            return Mono.just(EMPTY_RESPONSE);
        }
        try {
            String url = AIRKOREA_URL
                    + "?serviceKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&returnType=json"
                    + "&numOfRows=1000"
                    + "&pageNo=1"
                    + "&sidoName=" + URLEncoder.encode("전국", StandardCharsets.UTF_8)
                    + "&ver=1.0";
            return http.get(url)
                    .defaultIfEmpty(EMPTY_RESPONSE)
                    .onErrorResume(e -> {
                        log.warn("airkorea 호출 실패: {}", e.getMessage());
                        return Mono.just(EMPTY_RESPONSE);
                    });
        } catch (Exception e) {
            log.error("airkorea URL 생성 실패: {}", e.getMessage());
            return Mono.just(EMPTY_RESPONSE);
        }
    }

    public String getAirInfo() {
        return fetchAll().blockOptional().orElse(EMPTY_RESPONSE);
    }
}
