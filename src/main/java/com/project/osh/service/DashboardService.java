package com.project.osh.service;

import com.google.gson.JsonObject;

/**
 * \ub300\uc2dc\ubcf4\ub4dc \uc6c0\uc9c1\uc774\ub294 \ub370\uc774\ud130 \uc81c\uacf5/\uac31\uc2e0 \uba85\uc138.
 * - get* \ub294 \ucea0\uc2dc\ub41c \ucd5c\uadfc \uac12 \ubc18\ud658 (null \uc548\uc804).
 * - renew* \ub294 \uc678\ubd80 API \ud638\ucd9c\ub85c \ucea0\uc2dc \uac31\uc2e0.
 */
public interface DashboardService {

    JsonObject getDashboardJsonObject();

    /**
     * \uce90\uc2dc\ub9cc \ubcf4\uace0 \uc989\uc2dc \uc9c1\ub82c\ud654\ud55c \ub300\uc2dc\ubcf4\ub4dc \uc2a4\ub0c5\uc0f7 (\ub808\uac70\uc2dc).
     * \uc8fc\uc758: \uc774 \uba54\uc11c\ub4dc\ub294 \uc774\uc81c SSE 1\ucd08 tick \uc5d0 \uc4f0\uc774\uc9c0 \uc54a\uc74c (\ud398\uc774\ub85c\ub4dc\uac00 \ud074).
     * 1\ucd08 tick \uc740 {@link #getApplicationWrapperJson()} \ub9cc emit \ud55c\ub2e4.
     */
    JsonObject getDashboardSnapshot();

    /**
     * 1\ucd08 tick \uc758 \uc791\uc740 \ud398\uc774\ub85c\ub4dc \u2014 \uc2dc\uacc4 / \uba54\ubaa8\ub9ac / \ub85c\ub4dc \ub4f1 application \uc815\ubcf4\ub9cc.
     * \ud074\ub77c\uc774\uc5b8\ud2b8 \ucabd applicationJsonParser \uc640 \ud638\ud658\uc744 \uc704\ud574 wrapper \ud0a4\ub294 "applicationJson".
     */
    JsonObject getApplicationWrapperJson();

    /**
     * weather \uc804\uc6a9 wrapper (\ud074\ub77c\uc774\uc5b8\ud2b8\uac00 weatherJson \ud0a4\ub85c \uc77d\uc74c).
     */
    JsonObject getWeatherWrapperJson();

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

    /** \ub300\uae30\uc9c8(AirKorea) wrapper. */
    JsonObject getAirWrapperJson();

    JsonObject getAirJsonObject();

    void renewAirJsonObject();
}
