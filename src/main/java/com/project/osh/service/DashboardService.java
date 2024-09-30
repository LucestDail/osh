package com.project.osh.service;

import com.google.gson.JsonObject;

public interface DashboardService {

    public JsonObject getDashboardJsonObject();

    public JsonObject getWeatherJsonObject();

    public void renewWeatherJsonObject();

    public JsonObject getWeatherJsonObject(String lat, String lon);

    public void renewWeatherJsonObject(String lat, String lon);

    public JsonObject getWeatherJsonObject1(String lat, String lon);

    public void renewWeatherJsonObject1(String lat, String lon);

    public JsonObject getWeatherJsonObject2(String lat, String lon);

    public void renewWeatherJsonObject2(String lat, String lon);

    public JsonObject getApplicationJsonObject();

    public JsonObject getTrafficWrapperJson();

    public JsonObject getTrafficJsonObject();

    public void renewTrafficJsonObject();

    public JsonObject getEmergencyWrapperJson();

    public JsonObject getEmergencyJsonObject();

    public void renewEmergencyJsonObject();

    public JsonObject getYeonhapWrapperJson();

    public JsonObject getNewsYeonhapJsonObject();

    public void renewNewsYeonhapJsonObject();
}
