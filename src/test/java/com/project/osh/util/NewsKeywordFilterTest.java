package com.project.osh.util;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.project.osh.model.News;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 뉴스 관심 키워드 필터 단위 테스트.
 *
 * <p>계약:
 * <ul>
 *   <li>키워드가 없으면 <b>전부 통과</b> — 이 오탐 방지가 매칭 정확도보다 중요하다.</li>
 *   <li>payload 구조를 해석 못 하면 <b>원본 그대로</b> — 조용히 빈 목록으로 바꾸지 않는다.</li>
 * </ul>
 */
class NewsKeywordFilterTest {

    private static News news(String title, String content) {
        News n = new News();
        n.setNewsTitle(title);
        n.setNewsContents(content);
        return n;
    }

    /** 대시보드 통합 스트림이 실제로 싣는 전선 형태: 안쪽이 JSON "문자열". */
    private static JsonObject wrapper(String... titles) {
        JsonArray items = new JsonArray();
        for (String t : titles) {
            JsonObject o = new JsonObject();
            o.addProperty("title", t);
            o.addProperty("content", "본문:" + t);
            o.addProperty("company", "연합뉴스");
            items.add(o);
        }
        JsonObject data = new JsonObject();
        data.add("items", items);
        JsonObject root = new JsonObject();
        root.add("data", data);
        JsonObject w = new JsonObject();
        w.addProperty("yeonhapJson", root.toString());
        return w;
    }

    private static List<String> titlesOf(JsonObject w) {
        JsonObject root = JsonParser.parseString(w.get("yeonhapJson").getAsString()).getAsJsonObject();
        JsonArray items = root.getAsJsonObject("data").getAsJsonArray("items");
        return items.asList().stream().map(e -> e.getAsJsonObject().get("title").getAsString()).toList();
    }

    // ===== parse =====

    @Nested
    @DisplayName("parse — 쿼리 문자열 → 정규화 키워드")
    class Parse {

        @Test
        void nullAndBlankYieldEmptyList() {
            assertEquals(List.of(), NewsKeywordFilter.parse(null));
            assertEquals(List.of(), NewsKeywordFilter.parse(""));
            assertEquals(List.of(), NewsKeywordFilter.parse("   "));
            assertEquals(List.of(), NewsKeywordFilter.parse(",,, , ,"));
        }

        @Test
        void splitsTrimsAndLowercases() {
            assertEquals(List.of("금리", "반도체", "ai"), NewsKeywordFilter.parse(" 금리, 반도체 ,AI "));
        }

        @Test
        void dedupesPreservingOrder() {
            assertEquals(List.of("금리", "반도체"), NewsKeywordFilter.parse("금리,반도체,금리,금리"));
        }

        @Test
        void dedupesCaseInsensitively() {
            assertEquals(List.of("ai"), NewsKeywordFilter.parse("AI,ai,Ai"));
        }

        @Test
        void newlineIsAlsoASeparator() {
            assertEquals(List.of("금리", "환율"), NewsKeywordFilter.parse("금리\n환율"));
        }

        @Test
        @DisplayName("개수 상한을 넘으면 앞에서부터 상한까지만 — 나머지는 버리고 warn")
        void capsKeywordCount() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < NewsKeywordFilter.MAX_KEYWORDS + 10; i++) {
                sb.append("kw").append(i).append(',');
            }
            List<String> parsed = NewsKeywordFilter.parse(sb.toString());
            assertEquals(NewsKeywordFilter.MAX_KEYWORDS, parsed.size());
            assertEquals("kw0", parsed.get(0));
        }

        @Test
        @DisplayName("너무 긴 키워드는 거부가 아니라 절단 — 거부하면 '전부 통과'가 되어 의도와 멀어진다")
        void truncatesOverlongKeyword() {
            String long1 = "가".repeat(NewsKeywordFilter.MAX_KEYWORD_LEN + 30);
            List<String> parsed = NewsKeywordFilter.parse(long1);
            assertEquals(1, parsed.size());
            assertEquals(NewsKeywordFilter.MAX_KEYWORD_LEN, parsed.get(0).length());
        }
    }

    // ===== matches =====

    @Nested
    @DisplayName("matches — 뉴스 1건 판정")
    class Matches {

        @Test
        @DisplayName("★키워드가 없으면 무조건 통과 (오탐 방지 — 이게 깨지면 제품이 망가진다)")
        void noKeywordsAlwaysPasses() {
            assertTrue(NewsKeywordFilter.matches("아무 제목", "아무 본문", List.of()));
            assertTrue(NewsKeywordFilter.matches("아무 제목", "아무 본문", null));
            assertTrue(NewsKeywordFilter.matches(null, null, List.of()));
        }

        @Test
        void matchesOnTitle() {
            assertTrue(NewsKeywordFilter.matches("한국은행 금리 동결", "본문", List.of("금리")));
        }

        @Test
        void matchesOnContent() {
            assertTrue(NewsKeywordFilter.matches("제목", "본문에 반도체 수출 언급", List.of("반도체")));
        }

        @Test
        void isCaseInsensitive() {
            assertTrue(NewsKeywordFilter.matches("AI 반도체 특수", "본문", List.of("ai")));
            assertTrue(NewsKeywordFilter.matches("ai 반도체 특수", "본문", List.of("ai")));
        }

        @Test
        void anyKeywordMatchesIsEnough() {
            assertTrue(NewsKeywordFilter.matches("환율 급등", "본문", List.of("금리", "환율")));
        }

        @Test
        void nonMatchingIsRejected() {
            assertFalse(NewsKeywordFilter.matches("연예 소식", "가수 컴백", List.of("금리", "반도체")));
        }

        @Test
        @DisplayName("제목·본문이 null 이어도 NPE 없이 판정")
        void nullFieldsAreSafe() {
            assertFalse(NewsKeywordFilter.matches(null, null, List.of("금리")));
            assertTrue(NewsKeywordFilter.matches(null, "금리 인상", List.of("금리")));
            assertTrue(NewsKeywordFilter.matches("금리 인상", null, List.of("금리")));
        }

        @Test
        @DisplayName("제목 끝과 본문 첫머리가 이어붙어 없던 낱말이 생기지 않는다")
        void titleAndContentAreNotConcatenatedIntoFalseMatch() {
            // "금" + "리" 가 붙어 "금리" 가 되면 안 된다.
            assertFalse(NewsKeywordFilter.matches("황금", "리스크 점검", List.of("금리")));
        }
    }

    // ===== filterNews =====

    @Nested
    @DisplayName("filterNews — List<News>")
    class FilterNews {

        @Test
        @DisplayName("★키워드가 없으면 입력을 그대로(복사조차 안 한다)")
        void noKeywordsReturnsInputUntouched() {
            List<News> in = List.of(news("a", "b"), news("c", "d"));
            assertSame(in, NewsKeywordFilter.filterNews(in, List.of()));
            assertSame(in, NewsKeywordFilter.filterNews(in, null));
        }

        @Test
        void nullInputStaysNull() {
            assertNull(NewsKeywordFilter.filterNews(null, List.of("금리")));
        }

        @Test
        void keepsOnlyMatching() {
            List<News> in = List.of(
                    news("금리 인상", "본문"),
                    news("연예 소식", "가수"),
                    news("제목", "반도체 수출"));
            List<News> out = NewsKeywordFilter.filterNews(in, List.of("금리", "반도체"));
            assertEquals(2, out.size());
            assertEquals("금리 인상", out.get(0).getNewsTitle());
            assertEquals("제목", out.get(1).getNewsTitle());
        }

        @Test
        @DisplayName("하나도 안 걸리면 빈 목록 — 스냅샷 교체 방식이라 '변화 없음'과 구분돼야 한다")
        void noMatchYieldsEmptyList() {
            List<News> out = NewsKeywordFilter.filterNews(List.of(news("연예", "가수")), List.of("금리"));
            assertEquals(List.of(), out);
        }

        @Test
        void nullElementsAreSkipped() {
            List<News> in = new java.util.ArrayList<>();
            in.add(null);
            in.add(news("금리 인상", "본문"));
            assertEquals(1, NewsKeywordFilter.filterNews(in, List.of("금리")).size());
        }
    }

    // ===== filterYeonhapWrapper =====

    @Nested
    @DisplayName("filterYeonhapWrapper — 대시보드 통합 스트림 payload")
    class FilterWrapper {

        @Test
        @DisplayName("★키워드가 없으면 원본 그대로")
        void noKeywordsReturnsSameObject() {
            JsonObject w = wrapper("금리 인상", "연예 소식");
            assertSame(w, NewsKeywordFilter.filterYeonhapWrapper(w, List.of()));
            assertSame(w, NewsKeywordFilter.filterYeonhapWrapper(w, null));
        }

        @Test
        void keepsOnlyMatchingItems() {
            JsonObject out = NewsKeywordFilter.filterYeonhapWrapper(
                    wrapper("금리 인상", "연예 소식", "반도체 수출"), List.of("금리", "반도체"));
            assertEquals(List.of("금리 인상", "반도체 수출"), titlesOf(out));
        }

        @Test
        @DisplayName("매칭 0건이면 빈 items — 프런트가 스냅샷을 교체하므로 옛 뉴스가 남지 않는다")
        void noMatchYieldsEmptyItems() {
            JsonObject out = NewsKeywordFilter.filterYeonhapWrapper(wrapper("연예 소식"), List.of("금리"));
            assertEquals(List.of(), titlesOf(out));
        }

        @Test
        void matchesOnContentToo() {
            JsonObject out = NewsKeywordFilter.filterYeonhapWrapper(wrapper("무관한 제목"), List.of("본문:무관한"));
            assertEquals(1, titlesOf(out).size());
        }

        @Test
        void preservesOtherItemFields() {
            JsonObject out = NewsKeywordFilter.filterYeonhapWrapper(wrapper("금리 인상"), List.of("금리"));
            JsonObject root = JsonParser.parseString(out.get("yeonhapJson").getAsString()).getAsJsonObject();
            JsonObject item = root.getAsJsonObject("data").getAsJsonArray("items").get(0).getAsJsonObject();
            assertEquals("연합뉴스", item.get("company").getAsString());
            assertEquals("본문:금리 인상", item.get("content").getAsString());
        }

        @Test
        void nullWrapperStaysNull() {
            assertNull(NewsKeywordFilter.filterYeonhapWrapper(null, List.of("금리")));
        }

        // --- fail-open: 해석 못 하는 것을 조용히 버리지 않는다 ---

        @Test
        @DisplayName("★yeonhapJson 이 없으면 원본 그대로 (뉴스를 통째로 없애지 않는다)")
        void missingYeonhapJsonFailsOpen() {
            JsonObject odd = new JsonObject();
            odd.addProperty("somethingElse", "x");
            assertSame(odd, NewsKeywordFilter.filterYeonhapWrapper(odd, List.of("금리")));
        }

        @Test
        @DisplayName("★yeonhapJson 이 문자열이 아니면 원본 그대로")
        void nonPrimitiveYeonhapJsonFailsOpen() {
            JsonObject odd = new JsonObject();
            odd.add("yeonhapJson", new JsonArray());
            assertSame(odd, NewsKeywordFilter.filterYeonhapWrapper(odd, List.of("금리")));
        }

        @Test
        @DisplayName("★안쪽이 깨진 JSON 이면 원본 그대로")
        void malformedInnerJsonFailsOpen() {
            JsonObject odd = new JsonObject();
            odd.addProperty("yeonhapJson", "{not json");
            assertSame(odd, NewsKeywordFilter.filterYeonhapWrapper(odd, List.of("금리")));
        }

        @Test
        @DisplayName("★data / items 가 없으면 원본 그대로")
        void missingDataOrItemsFailsOpen() {
            JsonObject noData = new JsonObject();
            noData.addProperty("yeonhapJson", "{}");
            assertSame(noData, NewsKeywordFilter.filterYeonhapWrapper(noData, List.of("금리")));

            JsonObject noItems = new JsonObject();
            noItems.addProperty("yeonhapJson", "{\"data\":{}}");
            assertSame(noItems, NewsKeywordFilter.filterYeonhapWrapper(noItems, List.of("금리")));
        }

        @Test
        @DisplayName("초기화 안내문(제목만 있는 placeholder)은 키워드에 안 걸려 사라진다 — 프런트가 빈 목록으로 처리")
        void initPlaceholderIsFilteredOutWhenKeywordsSet() {
            JsonObject out = NewsKeywordFilter.filterYeonhapWrapper(
                    wrapper("데이터를 불러오는 중입니다..."), List.of("금리"));
            assertEquals(List.of(), titlesOf(out));
        }
    }
}
