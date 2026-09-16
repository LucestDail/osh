package com.project.osh.service;

import com.project.osh.model.News;
import com.project.osh.util.NewsKeywordFilter;
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

    /**
     * 구독자 1명 = emitter + 그 연결 전용 관심 키워드.
     * 키워드가 빈 목록이면 전부 받는다(키워드를 안 정한 사람에게 아무것도 안 보내면 제품이 망가진다).
     */
    private record Subscriber(SseEmitter emitter, List<String> keywords) {
    }

    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final AtomicInteger emitterCount = new AtomicInteger(0);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    /** 키워드 없는 구독(= 전부 수신). 기존 호출자 호환. */
    public SseEmitter createEmitter() {
        return createEmitter(List.of());
    }

    /**
     * 관심 키워드를 붙여 구독한다. 브로드캐스트는 이 구독자에게 매칭된 뉴스만 보낸다.
     *
     * @param keywords {@link com.project.osh.util.NewsKeywordFilter#parse} 결과. 비었으면 전부 수신.
     */
    public SseEmitter createEmitter(List<String> keywords) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        Subscriber subscriber = new Subscriber(emitter, keywords == null ? List.of() : keywords);
        subscribers.add(subscriber);
        emitterCount.incrementAndGet();

        emitter.onCompletion(() -> {
            subscribers.remove(subscriber);
            emitterCount.decrementAndGet();
        });

        emitter.onTimeout(() -> {
            subscribers.remove(subscriber);
            emitterCount.decrementAndGet();
        });

        emitter.onError(e -> {
            subscribers.remove(subscriber);
            emitterCount.decrementAndGet();
        });

        try {
            // Send initial heartbeat
            emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data("connected", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            log.error("Error sending initial heartbeat", e);
            subscribers.remove(subscriber);
            emitterCount.decrementAndGet();
        }

        return emitter;
    }

    public void broadcastNewsUpdate(List<News> news) {
        if (subscribers.isEmpty()) {
            return;
        }

        List<Subscriber> deadSubscribers = new CopyOnWriteArrayList<>();

        subscribers.forEach(subscriber -> {
            try {
                // news가 null이거나 비어있어도 빈 리스트로 전송
                List<News> newsToSend = (news != null) ? news : List.of();
                // 구독자별 키워드 필터 — 키워드가 없으면 filterNews 가 입력을 그대로 돌려준다.
                newsToSend = NewsKeywordFilter.filterNews(newsToSend, subscriber.keywords());
                subscriber.emitter().send(SseEmitter.event()
                    .name("news")
                    .data(newsToSend, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                deadSubscribers.add(subscriber);
                log.debug("Client disconnected during news broadcast");
            } catch (Exception e) {
                deadSubscribers.add(subscriber);
                log.error("Unexpected error while sending news update", e);
            }
        });

        if (!deadSubscribers.isEmpty()) {
            subscribers.removeAll(deadSubscribers);
            emitterCount.addAndGet(-deadSubscribers.size());
            log.debug("Removed {} dead emitters, {} remaining", deadSubscribers.size(), subscribers.size());
        }
    }

    public int getActiveConnectionCount() {
        return emitterCount.get();
    }
} 