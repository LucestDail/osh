package com.project.osh.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.CapturingEmitterHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.osh.model.News;
import com.project.osh.service.EventEmitterService;
import com.project.osh.service.NewsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * {@code GET /api/news/stream?keywords=} 배선 테스트.
 *
 * <p>확인하는 것은 둘: ①파싱된 키워드가 구독자에 등록되는가 ②최초 스냅샷도 걸러지는가.
 * 최초 스냅샷을 안 거르면 구독 직후 한 번은 관심 없는 뉴스가 통째로 쏟아진다.
 */
@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock
    private NewsService newsService;

    @Mock
    private EventEmitterService eventEmitterService;

    @InjectMocks
    private NewsController controller;

    private static News news(String title) {
        News n = new News();
        n.setNewsTitle(title);
        n.setNewsContents("본문");
        return n;
    }

    private static final List<News> ALL = List.of(
            news("금리 인상"), news("연예 소식"), news("반도체 수출"));

    @BeforeEach
    void stubEmitter() {
        when(eventEmitterService.createEmitter(anyList())).thenReturn(new SseEmitter(1000L));
    }

    @SuppressWarnings("unchecked")
    private List<String> capturedKeywords() {
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventEmitterService).createEmitter(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("키워드 파라미터가 파싱되어 구독자에 등록된다")
    void keywordsReachTheSubscriber() {
        when(newsService.getAllNews()).thenReturn(ALL);

        controller.streamNews(" 금리, 반도체 ,금리 ");

        assertEquals(List.of("금리", "반도체"), capturedKeywords());
    }

    @Test
    @DisplayName("★키워드가 없으면 빈 목록으로 등록된다 = 전량 수신 (오탐 금지)")
    void noKeywordRegistersEmptyList() {
        when(newsService.getAllNews()).thenReturn(ALL);

        controller.streamNews(null);

        assertEquals(List.of(), capturedKeywords());
    }

    @Test
    @DisplayName("공백뿐인 키워드도 전량 수신으로 취급한다")
    void blankKeywordRegistersEmptyList() {
        when(newsService.getAllNews()).thenReturn(ALL);

        controller.streamNews("  ,  , ");

        assertEquals(List.of(), capturedKeywords());
    }

    @Test
    @DisplayName("★최초 스냅샷도 걸러진다 — 구독 직후 무관한 뉴스가 쏟아지면 안 된다")
    void initialSnapshotIsFilteredToo() throws Exception {
        when(newsService.getAllNews()).thenReturn(ALL);

        // 최초 send 는 emitter 버퍼에 쌓인다 → handler 를 붙여 꺼내 본다.
        SseEmitter emitter = new SseEmitter(1000L);
        when(eventEmitterService.createEmitter(anyList())).thenReturn(emitter);

        controller.streamNews("금리");

        assertEquals(List.of("금리 인상"), sentTitles(emitter));
    }

    @Test
    @DisplayName("키워드가 없으면 최초 스냅샷은 전부 나간다")
    void initialSnapshotUnfilteredWithoutKeywords() throws Exception {
        when(newsService.getAllNews()).thenReturn(ALL);

        SseEmitter emitter = new SseEmitter(1000L);
        when(eventEmitterService.createEmitter(anyList())).thenReturn(emitter);

        controller.streamNews(null);

        assertEquals(List.of("금리 인상", "연예 소식", "반도체 수출"), sentTitles(emitter));
    }

    @Test
    @DisplayName("뉴스가 null 이어도 스트림은 열린다")
    void nullNewsDoesNotBreakStream() {
        when(newsService.getAllNews()).thenReturn(null);

        controller.streamNews("금리");

        assertEquals(List.of("금리"), capturedKeywords());
    }

    /** emitter 버퍼에 쌓인 뉴스 제목을 꺼낸다. */
    private static List<String> sentTitles(SseEmitter emitter) throws Exception {
        return CapturingEmitterHandler.attach(emitter).flattenedLists().stream()
                .filter(News.class::isInstance)
                .map(o -> ((News) o).getNewsTitle())
                .toList();
    }
}
