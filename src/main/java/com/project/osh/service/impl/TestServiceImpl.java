package com.project.osh.service.impl;

import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.project.osh.interfaces.InterfaceCore;
import com.project.osh.service.TestService;
import com.project.osh.util.JsonUtil;

@Service
public class TestServiceImpl implements TestService {

    private final InterfaceCore interfaceCore;
    private final JsonUtil jsonUtil = new JsonUtil();

    public TestServiceImpl(InterfaceCore interfaceCore) {
        this.interfaceCore = interfaceCore;
    }

    public JsonObject getWeatherJsonObject() {
        return jsonUtil.getJson(interfaceCore.getWeatherInfo());
    }
}
