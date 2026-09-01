package com.project.osh.util;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JsonUtil.getJson 방어 로직 단위 테스트.
 * 계약: 어떤 입력에도 null 을 던지지 않고 항상 JsonObject 반환(파싱 실패/이상형은 빈 객체).
 */
class JsonUtilTest {

    private final JsonUtil jsonUtil = new JsonUtil();

    @Test
    void parsesValidJsonObject() {
        JsonObject o = jsonUtil.getJson("{\"a\":1,\"b\":\"x\"}");
        assertNotNull(o);
        assertEquals(1, o.get("a").getAsInt());
        assertEquals("x", o.get("b").getAsString());
    }

    @Test
    void nullInputReturnsEmptyObject() {
        JsonObject o = jsonUtil.getJson(null);
        assertNotNull(o);
        assertEquals(0, o.size());
    }

    @Test
    void blankInputReturnsEmptyObject() {
        assertEquals(0, jsonUtil.getJson("").size());
        assertEquals(0, jsonUtil.getJson("   ").size());
    }

    @Test
    void malformedJsonReturnsEmptyObject() {
        JsonObject o = jsonUtil.getJson("{not valid json");
        assertNotNull(o);
        assertEquals(0, o.size());
    }

    @Test
    void jsonArrayTopLevelCannotCastToObjectSoReturnsEmpty() {
        // (JsonObject) 캐스팅 실패(ClassCastException) → catch → 빈 객체
        JsonObject o = jsonUtil.getJson("[1,2,3]");
        assertNotNull(o);
        assertEquals(0, o.size());
    }

    @Test
    void nestedObjectRoundTrips() {
        JsonObject o = jsonUtil.getJson("{\"response\":{\"body\":{\"items\":[]}}}");
        assertTrue(o.has("response"));
        assertFalse(o.getAsJsonObject("response").getAsJsonObject("body").getAsJsonArray("items").iterator().hasNext());
    }
}
