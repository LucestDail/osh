package com.project.osh.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {
	private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);
	
	public JsonObject getJson(String strJson) {
		JsonObject jsonObj = new JsonObject();
		try {
			// null 체크 추가
			if (strJson == null || strJson.trim().isEmpty()) {
				log.warn("JSON 문자열이 null이거나 비어있습니다.");
				return jsonObj;
			}
			
			jsonObj = (JsonObject) JsonParser.parseString(strJson);
		} catch (Exception e) {
			log.error("JSON 파싱 중 오류 발생: {}", e.getMessage());
			// 에러 발생 시에도 빈 JsonObject 반환
		}
	    return jsonObj;
	}
}
