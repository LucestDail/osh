package com.project.osh.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    /**
     * \uc678\ubd80 API \ud638\ucd9c\uc6a9 \uacf5\uc6a9 WebClient.
     * - 5\ucd08 connect / 8\ucd08 read / 8\ucd08 write timeout
     * - codec max in-memory 8MB (\uad6d\uac00 \uc548\uc804\ub9dd \uac19\uc740 \uc81c\uacf5 API \uc751\ub2f5\uc774 \ud074 \uc218 \uc788\uc74c)
     * - SSL\uc740 \uae30\ubcf8 \uc124\uc815\uc744 \uc0ac\uc6a9 (openapi.its.go.kr:9443 \uac19\uc740 https \uc11c\ubc84)
     */
    @Bean
    public WebClient externalApiWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(8))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(8, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(8, TimeUnit.SECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();
    }
}
