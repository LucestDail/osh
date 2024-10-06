package com.project.osh.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtil {
	public JsonObject getJson(String strJson) {
		JsonObject jsonObj = new JsonObject();
		try {
			jsonObj = (JsonObject) JsonParser.parseString(strJson);
		}catch(Exception e) {
			e.printStackTrace();
		}
	    return jsonObj;
	}
}
