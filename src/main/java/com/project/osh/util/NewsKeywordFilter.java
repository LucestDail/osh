package com.project.osh.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.project.osh.model.News;

/**
 * 관심 키워드로 뉴스를 거르는 순수 헬퍼 (SSE 구독자별 필터링용).
 *
 * <p><b>왜 서버에서 거르는가</b>: SSE 구독자가 늘면 같은 100건 payload 를 사람 수만큼
 * 내려보내게 된다. 구독자별 키워드는 구독 시점에 정해지므로 브로드캐스트 직전에 거르면
 * 대역폭이 실제 관심사에 비례한다.
 *
 * <p><b>설계 원칙 — 막을 때보다 안 막을 때가 중요하다</b>:
 * <ul>
 *   <li>키워드가 <b>없으면 전부 통과</b>시킨다. 키워드를 정하지 않은 사람에게 아무것도
 *       안 보내면 제품이 망가진다(오탐이 미탐보다 해롭다).</li>
 *   <li>payload 구조가 예상과 다르면 <b>거르지 않고 원본을 그대로</b> 돌려준다.
 *       해석 못 하는 것을 조용히 버리면 뉴스가 통째로 사라진다.</li>
 *   <li>버려지는 입력(개수·길이 초과)은 {@code warn} 으로 남긴다.</li>
 * </ul>
 *
 * <p>모든 메서드는 상태가 없고 부작용이 없다(로그 제외).
 */
public final class NewsKeywordFilter {

    private static final Logger log = LoggerFactory.getLogger(NewsKeywordFilter.class);

    /** 구독자 1명이 등록할 수 있는 키워드 수 상한. 브로드캐스트 1회 비용을 (구독자 × 뉴스 × 이 값)으로 묶는다. */
    static final int MAX_KEYWORDS = 20;

    /** 키워드 1개 길이 상한. 이보다 길면 잘라 쓴다(거부하지 않는다 — 거부는 곧 전부 통과가 되어 의도와 멀어진다). */
    static final int MAX_KEYWORD_LEN = 50;

    private NewsKeywordFilter() {
    }

    /**
     * 쿼리 파라미터 문자열을 정규화된 키워드 목록으로 파싱한다.
     *
     * <p>구분자는 쉼표와 개행. 앞뒤 공백 제거, 소문자화(대소문자 무시 매칭), 중복 제거(입력 순서 유지).
     *
     * @param raw 예: {@code "금리, 반도체,금리 ,, AI"}
     * @return 정규화 목록. null/빈 문자열/공백뿐이면 빈 목록(= 전부 통과 신호).
     */
    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        int dropped = 0;
        for (String token : raw.split("[,\\n]")) {
            String kw = token.trim().toLowerCase(Locale.ROOT);
            if (kw.isEmpty()) {
                continue;
            }
            if (kw.length() > MAX_KEYWORD_LEN) {
                log.warn("뉴스 키워드가 너무 길어 {}자로 자름 — 원본 {}자", MAX_KEYWORD_LEN, kw.length());
                kw = kw.substring(0, MAX_KEYWORD_LEN);
            }
            if (seen.size() >= MAX_KEYWORDS) {
                dropped++;
                continue;
            }
            seen.add(kw);
        }
        if (dropped > 0) {
            log.warn("뉴스 키워드가 상한({})을 넘어 {}개 버림", MAX_KEYWORDS, dropped);
        }
        return List.copyOf(seen);
    }

    /**
     * 뉴스 1건이 키워드 중 하나라도 걸리는가(제목 또는 본문, 대소문자 무시 부분일치).
     *
     * @param keywords {@link #parse} 결과. <b>비었으면 항상 true</b>(전부 통과).
     */
    public static boolean matches(String title, String content, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        String haystack = ((title == null ? "" : title) + '\n' + (content == null ? "" : content))
                .toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (haystack.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@link News} 목록을 키워드로 거른다.
     *
     * @return 키워드가 비었거나 입력이 null 이면 <b>입력을 그대로</b>(복사하지 않는다).
     */
    public static List<News> filterNews(List<News> news, List<String> keywords) {
        if (news == null || keywords == null || keywords.isEmpty()) {
            return news;
        }
        List<News> out = new ArrayList<>(news.size());
        for (News n : news) {
            if (n != null && matches(n.getNewsTitle(), n.getNewsContents(), keywords)) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * 대시보드 통합 스트림의 뉴스 wrapper 를 키워드로 거른다.
     *
     * <p>전선(wire) 형태: <code>{"yeonhapJson":"{\"data\":{\"items\":[{title,content,...}]}}"}</code>
     * — 안쪽이 <b>JSON 문자열</b>이라 한 번 더 파싱해야 한다.
     *
     * @return 걸러진 새 wrapper. 키워드가 비었거나 <b>구조를 해석하지 못하면 원본 그대로</b>(fail-open).
     */
    public static JsonObject filterYeonhapWrapper(JsonObject wrapper, List<String> keywords) {
        if (wrapper == null || keywords == null || keywords.isEmpty()) {
            return wrapper;
        }
        try {
            JsonElement inner = wrapper.get("yeonhapJson");
            if (inner == null || !inner.isJsonPrimitive()) {
                log.warn("뉴스 wrapper 에 yeonhapJson 문자열이 없어 필터를 건너뛴다 — 원본 전송");
                return wrapper;
            }
            JsonElement rootEl = JsonParser.parseString(inner.getAsString());
            if (!rootEl.isJsonObject()) {
                log.warn("yeonhapJson 이 객체가 아니라 필터를 건너뛴다 — 원본 전송");
                return wrapper;
            }
            JsonObject root = rootEl.getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            if (data == null) {
                log.warn("yeonhapJson 에 data 가 없어 필터를 건너뛴다 — 원본 전송");
                return wrapper;
            }
            JsonArray items = data.getAsJsonArray("items");
            if (items == null) {
                log.warn("yeonhapJson 에 items 가 없어 필터를 건너뛴다 — 원본 전송");
                return wrapper;
            }

            JsonArray kept = new JsonArray();
            for (JsonElement el : items) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject item = el.getAsJsonObject();
                if (matches(str(item, "title"), str(item, "content"), keywords)) {
                    kept.add(item);
                }
            }

            JsonObject newData = new JsonObject();
            newData.add("items", kept);
            JsonObject newRoot = new JsonObject();
            newRoot.add("data", newData);
            JsonObject out = new JsonObject();
            out.addProperty("yeonhapJson", newRoot.toString());
            log.debug("뉴스 키워드 필터 — {}건 중 {}건 통과 (키워드 {})", items.size(), kept.size(), keywords);
            return out;
        } catch (Exception e) {
            // 해석 실패를 조용히 빈 목록으로 바꾸면 뉴스가 통째로 사라진다 → 원본을 그대로 보낸다.
            log.warn("뉴스 키워드 필터 실패 — 원본 전송. cause={}", e.getMessage());
            return wrapper;
        }
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e != null && e.isJsonPrimitive()) ? e.getAsString() : null;
    }
}
