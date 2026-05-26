package com.project.osh.util;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * \uc678\ubd80 API \uacf5\uc6a9 \ud638\ucd9c \ud5ec\ud37c.
 * - timeout + retry-with-backoff
 * - 4xx \ub294 \uc7ac\uc2dc\ub3c4 X (\ud074\ub77c\uc774\uc5b8\ud2b8 \uc624\ub958)
 * - 5xx / Timeout / I/O \ub294 \uc7ac\uc2dc\ub3c4
 */
@Component
public class ReactiveHttp {

    private static final Logger log = LoggerFactory.getLogger(ReactiveHttp.class);

    private final WebClient webClient;

    public ReactiveHttp(WebClient externalApiWebClient) {
        this.webClient = externalApiWebClient;
    }

    /**
     * GET \ud638\ucd9c. \uc2e4\ud328 \uc2dc \ud638\ucd9c\ucd1d \uc18c\uc694\uc2dc\uac04 \ub300\ub7b5 = (timeout * (retry+1)) \uc774\ub0b4.
     */
    public Mono<String> get(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(8))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(400))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter(t -> {
                            // 4xx \ub294 \uc7ac\uc2dc\ub3c4 X
                            if (t instanceof WebClientResponseException wcre) {
                                return wcre.getStatusCode().is5xxServerError();
                            }
                            return true; // timeout / IO \ub4f1\uc740 \uc7ac\uc2dc\ub3c4
                        }))
                .doOnError(e -> log.warn("HTTP GET \ucd5c\uc885 \uc2e4\ud328 [{}] {}", url, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * \ub3d9\uae30 \ud638\ucd9c \ud5ec\ud37c. \uc2e4\ud328 \uc2dc null \ubc18\ud658 (\uae30\uc874 HttpUtil \ud638\ud658 \uc6a9).
     */
    public String getBlocking(String url) {
        return get(url).blockOptional().orElse(null);
    }
}
