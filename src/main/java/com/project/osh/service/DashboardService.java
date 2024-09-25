package com.project.osh.service;

import com.google.gson.JsonObject;

public interface DashboardService {

    public JsonObject getDashboardJsonObject();

    public JsonObject getWeatherJsonObject();

    public JsonObject getWeatherJsonObject(String lat, String lon);

    public JsonObject getWeatherJsonObject1(String lat, String lon);

    public JsonObject getWeatherJsonObject2(String lat, String lon);

    public JsonObject getApplicationJsonObject();

    public JsonObject getTrafficJsonObject();

    public JsonObject getEmergencyJsonObject();

    public JsonObject getNewsYeonhapJsonObject();

    public JsonObject getNewsChosunJsonObject();
}
