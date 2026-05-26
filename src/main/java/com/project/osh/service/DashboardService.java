package com.project.osh.service;

import com.google.gson.JsonObject;

/**
 * \ub300\uc2dc\ubcf4\ub4dc \uc6c0\uc9c1\uc774\ub294 \ub370\uc774\ud130 \uc81c\uacf5/\uac31\uc2e0 \uba85\uc138.
 * - get* \ub294 \ucea0\uc2dc\ub41c \ucd5c\uadfc \uac12 \ubc18\ud658 (null \uc548\uc804).
 * - renew* \ub294 \uc678\ubd80 API \ud638\ucd9c\ub85c \ucea0\uc2dc \uac31\uc2e0.
 */
public interface DashboardService {

    JsonObject getDashboardJsonObject();

    JsonObject getApplicationJsonObject();

    JsonObject getWeatherJsonObject();

    void renewWeatherJsonObject();

    JsonObject getTrafficWrapperJson();

    JsonObject getTrafficJsonObject();

    void renewTrafficJsonObject();

    JsonObject getEmergencyWrapperJson();

    JsonObject getEmergencyJsonObject();

    void renewEmergencyJsonObject();

    JsonObject getYeonhapWrapperJson();

    JsonObject getNewsYeonhapJsonObject();

    void renewNewsYeonhapJsonObject();
}
