package com.project.osh.service;

import com.project.osh.model.News;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class EventEmitterService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> {
            log.error("SSE connection error", e);
            emitters.remove(emitter);
        });

        return emitter;
    }

    public void broadcastNewsUpdate(List<News> news) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        emitters.forEach(emitter -> {
            try {
                emitter.send(news, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                log.error("Error sending news update", e);
                deadEmitters.add(emitter);
            }
        });
        
        emitters.removeAll(deadEmitters);
    }
} 