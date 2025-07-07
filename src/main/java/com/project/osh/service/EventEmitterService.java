package com.project.osh.service;

import com.project.osh.model.News;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EventEmitterService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicInteger emitterCount = new AtomicInteger(0);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.add(emitter);
        emitterCount.incrementAndGet();
        
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            emitterCount.decrementAndGet();
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitterCount.decrementAndGet();
        });
        
        emitter.onError(e -> {
            emitters.remove(emitter);
            emitterCount.decrementAndGet();
        });

        try {
            // Send initial heartbeat
            emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data("connected", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            log.error("Error sending initial heartbeat", e);
            emitters.remove(emitter);
            emitterCount.decrementAndGet();
        }

        return emitter;
    }

    public void broadcastNewsUpdate(List<News> news) {
        if (emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        
        emitters.forEach(emitter -> {
            try {
                // news가 null이거나 비어있어도 빈 리스트로 전송
                List<News> newsToSend = (news != null) ? news : List.of();
                emitter.send(SseEmitter.event()
                    .name("news")
                    .data(newsToSend, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.debug("Client disconnected during news broadcast");
            } catch (Exception e) {
                deadEmitters.add(emitter);
                log.error("Unexpected error while sending news update", e);
            }
        });
        
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            emitterCount.addAndGet(-deadEmitters.size());
            log.debug("Removed {} dead emitters, {} remaining", deadEmitters.size(), emitters.size());
        }
    }

    public int getActiveConnectionCount() {
        return emitterCount.get();
    }
} 