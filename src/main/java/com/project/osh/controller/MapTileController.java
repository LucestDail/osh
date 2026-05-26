package com.project.osh.controller;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/map-tile")
public class MapTileController {

    private static final Logger log = LoggerFactory.getLogger(MapTileController.class);
    private static final Set<String> ALLOWED_LAYERS = Set.of(
            "precipitation_new", "clouds_new", "temp_new", "wind_new", "pressure_new"
    );

    @Value("${osh.api.openweather.key}")
    private String owmKey;

    private final WebClient webClient = WebClient.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
            .build();

    @GetMapping("/{layer}/{z}/{x}/{y}")
    public ResponseEntity<byte[]> tile(
            @PathVariable String layer,
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y) {

        if (!ALLOWED_LAYERS.contains(layer)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (z < 0 || z > 18) {
            return ResponseEntity.badRequest().build();
        }

        String url = "https://tile.openweathermap.org/map/" + layer + "/"
                + z + "/" + x + "/" + y + ".png?appid=" + owmKey;
        try {
            byte[] bytes = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            if (bytes == null || bytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                    .body(bytes);

        } catch (Exception e) {
            log.warn("OWM 타일 프록시 실패 [{} {}/{}/{}]: {}", layer, z, x, y, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
