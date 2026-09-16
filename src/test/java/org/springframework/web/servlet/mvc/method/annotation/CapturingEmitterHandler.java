package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.http.MediaType;

/**
 * 테스트 전용 — {@link SseEmitter} 가 실제로 내보낸 payload 를 들여다보는 가짜 handler.
 *
 * <p><b>왜 이 패키지에 있나</b>: {@code ResponseBodyEmitter.Handler} 는 package-private 라
 * 바깥에서 구현할 수 없다. 프로덕션 코드에 테스트용 주입점(emitter 팩토리 등)을 새로 뚫는 대신,
 * 이 helper 하나만 스프링 패키지에 둔다. 프로덕션 코드는 그대로다.
 *
 * <p>{@code initialize()} 를 부르기 전의 {@code send()} 는 emitter 내부에 쌓였다가
 * 붙는 순간 handler 로 흘러나온다 — 그래서 "보낸 뒤에 붙여도" 전부 잡힌다.
 */
public final class CapturingEmitterHandler implements ResponseBodyEmitter.Handler {

    private final List<Object> data = new ArrayList<>();
    private Runnable completionCallback;

    private CapturingEmitterHandler() {
    }

    /** emitter 에 붙이고, 그때까지 쌓인 것 + 이후 전송분을 모으는 캡처를 돌려준다. */
    public static CapturingEmitterHandler attach(ResponseBodyEmitter emitter) throws IOException {
        CapturingEmitterHandler handler = new CapturingEmitterHandler();
        emitter.initialize(handler);
        return handler;
    }

    /** 전송된 raw data 조각 전부(이벤트명·개행 같은 SSE 골격 문자열 포함). */
    public List<Object> data() {
        return data;
    }

    /** 전송된 조각 중 List 인 것들을 평평하게 — 뉴스 목록 payload 를 꺼내는 용도. */
    public List<Object> flattenedLists() {
        List<Object> out = new ArrayList<>();
        for (Object d : data) {
            if (d instanceof List<?> list) {
                out.addAll(list);
            }
        }
        return out;
    }

    /** List payload 가 한 번이라도 전송됐는가(빈 목록이어도 true). */
    public boolean sentAnyList() {
        return data.stream().anyMatch(List.class::isInstance);
    }

    @Override
    public void send(Object d, MediaType mediaType) {
        data.add(d);
    }

    @Override
    public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) {
        items.forEach(i -> data.add(i.getData()));
    }

    /**
     * 실제 스프링이 비동기 요청 종료 시 하는 일을 흉내낸다 — {@code emitter.onCompletion(...)} 으로
     * 등록된 콜백을 부른다. {@code emitter.complete()} 만으로는 이 콜백이 불리지 않는다
     * (핸들러가 부르는 구조라 단위 테스트에서 직접 태워야 한다).
     */
    public void fireCompletion() {
        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    @Override public void complete() { }
    @Override public void completeWithError(Throwable failure) { }
    @Override public void onTimeout(Runnable callback) { }
    @Override public void onError(Consumer<Throwable> callback) { }

    @Override
    public void onCompletion(Runnable callback) {
        this.completionCallback = callback;
    }
}
