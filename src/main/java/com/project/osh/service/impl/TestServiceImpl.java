package com.project.osh.service.impl;

import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.project.osh.interfaces.InterfaceCore;
import com.project.osh.service.TestService;
import com.project.osh.util.JsonUtil;

@Service
public class TestServiceImpl implements TestService {
    
    public JsonObject getWeatherJsonObject(){
        JsonUtil ju = new JsonUtil();
        JsonObject weatherJson = ju.getJson(new InterfaceCore().getWeatherInfo());
        return weatherJson;
    }
    
}