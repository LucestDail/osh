package com.project.osh.service.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.project.osh.config.OshProperties;
import com.project.osh.interfaces.AirInterface;
import com.project.osh.interfaces.InterfaceCore;
import com.project.osh.interfaces.WeatherInterface;
import com.project.osh.service.DashboardService;
import com.project.osh.service.DashboardSinks;
import com.project.osh.service.NewsService;
import com.project.osh.util.JsonUtil;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private static final long WEATHER_UPDATE_INTERVAL = 3_600_000L; // 1\uc2dc\uac04
    private static final long TRAFFIC_UPDATE_INTERVAL = 300_000L;   // 5\ubd84
    private static final long EMERGENCY_UPDATE_INTERVAL = 300_000L; // 5\ubd84
    private static final long NEWS_UPDATE_INTERVAL = 300_000L;      // 5\ubd84
    private static final long AIR_UPDATE_INTERVAL = 3_600_000L;     // 1\uc2dc\uac04

    // \uc778\uc2a4\ud134\uc2a4 \ucea0\uc2dc (\uc774\uc804: static \uc774\uc5c8\uc74c)
    private volatile JsonObject weatherJsonObject;
    private volatile JsonObject trafficJsonObject;
    private volatile JsonObject emergencyJsonObject;
    private volatile JsonObject yeonhapJsonObject;
    private volatile JsonObject airJsonObject;

    private volatile long lastWeatherUpdate = 0;
    private volatile long lastTrafficUpdate = 0;
    private volatile long lastEmergencyUpdate = 0;
    private volatile long lastNewsUpdate = 0;
    private volatile long lastAirUpdate = 0;

    @Value("${osh.logging}")
    private boolean loggingFlag;

    private final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    private final NewsService newsService;
    private final InterfaceCore interfaceCore;
    private final WeatherInterface weatherInterface;
    private final AirInterface airInterface;
    private final OshProperties properties;
    private final DashboardSinks sinks;
    private final JsonUtil jsonUtil = new JsonUtil();

    public DashboardServiceImpl(NewsService newsService,
                                InterfaceCore interfaceCore,
                                WeatherInterface weatherInterface,
                                AirInterface airInterface,
                                OshProperties properties,
                                DashboardSinks sinks) {
        this.newsService = newsService;
        this.interfaceCore = interfaceCore;
        this.weatherInterface = weatherInterface;
        this.airInterface = airInterface;
        this.properties = properties;
        this.sinks = sinks;

        // \ubd80\ud305 \uc9c0\uc5f0 \ubc29\uc9c0\ub97c \uc704\ud574 \ucea0\uc2dc\ub294 \ube48 \uac1d\uccb4\ub85c \ucd08\uae30\ud654.
        // \uc2e4\uc81c \uc678\ubd80 API \ud638\ucd9c\uc740 @PostConstruct \uc5d0\uc11c \ube44\ub3d9\uae30\ub85c.
        long now = System.currentTimeMillis();
        weatherJsonObject = new JsonObject();
        trafficJsonObject = wrapEmptyItems();
        emergencyJsonObject = wrapEmptyItems();
        yeonhapJsonObject = wrapInitMessage("\uc11c\ube44\uc2a4 \ucd08\uae30\ud654 \uc911\uc785\ub2c8\ub2e4...", "\ub274\uc2a4 \uc11c\ube44\uc2a4\uac00 \uc900\ube44\ub418\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4.");
        airJsonObject = new JsonObject();
        lastWeatherUpdate = now;
        lastTrafficUpdate = now;
        lastEmergencyUpdate = now;
        lastNewsUpdate = now;
        lastAirUpdate = now;
    }

    /**
     * \uc560\ud50c \uc2dc\uc791 \ud6c4 \ube44\ub3d9\uae30\ub85c \ucd08\uae30 \ub0a0\uc528 \uc801\uc7ac.
     * \uc774\uc804\uc5d0\ub294 \uc0dd\uc131\uc790\uc5d0\uc11c 19\ub3c4\uc2dc \ub3d9\uae30 \ud638\ucd9c\ub85c \uc810\uc720\ub418\uc5c8\uc74c (\ubd80\ud305 23\ucd08 \u2192 \ubaa9\ud45c <5\ucd08).
     */
    @PostConstruct
    void initAsync() {
        // \uc2dc\uc791 \uc9c1\ud6c4 \uad6c\ub3c5\uc790\ub3c4 \ucd5c\ucd08 1\ud68c emit \ubc1b\uc744 \uc218 \uc788\ub3c4\ub85d \ube48 \uac1d\uccb4\ub77c\ub3c4 \uc120\ud589 emit
        sinks.pushEmergency(emergencyJsonObject);
        sinks.pushTraffic(trafficJsonObject);
        sinks.pushYeonhap(yeonhapJsonObject);
        sinks.pushWeather(getWeatherWrapperJson());
        sinks.pushAir(getAirWrapperJson());
        sinks.pushDashboard(getApplicationWrapperJson());

        CompletableFuture.runAsync(() -> {
            try {
                // NewsServiceImpl \ub3c4 @PostConstruct \ube44\ub3d9\uae30 \ub85c\ub4dc(\ub300\ub7b5 1\ucd08 \uc774\ub0b4) \ud6c4 yeonhap re-emit
                Thread.sleep(1500);
                sinks.pushYeonhap(getYeonhapWrapperJson());

                long t0 = System.currentTimeMillis();
                updateWeatherData();
                lastWeatherUpdate = System.currentTimeMillis();
                if (loggingFlag) {
                    log.info("\ucd08\uae30 \ub0a0\uc528 \ub85c\ub4dc \uc644\ub8cc ({} ms, {} cities)",
                            System.currentTimeMillis() - t0, properties.getCities().size());
                }
                sinks.pushWeather(getWeatherWrapperJson());

                // \ub300\uae30\uc9c8 \ucd08\uae30 \ub85c\ub4dc (\ud0a4 \uc5c6\uc73c\uba74 AirInterface \uac00 \ube48 \uac1d\uccb4 \ubc18\ud658)
                long ta = System.currentTimeMillis();
                updateAirData();
                lastAirUpdate = System.currentTimeMillis();
                if (loggingFlag) {
                    log.info("\ucd08\uae30 \ub300\uae30\uc9c8 \ub85c\ub4dc \uc644\ub8cc ({} ms)", System.currentTimeMillis() - ta);
                }
                sinks.pushAir(getAirWrapperJson());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("\ucd08\uae30 \ub370\uc774\ud130 \ub85c\ub4dc \uc2e4\ud328: {}", e.getMessage());
            }
        });
    }

    @Override
    public JsonObject getDashboardSnapshot() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("weatherJson", weatherJsonObject != null ? weatherJsonObject.toString() : "{}");
        jsonObject.addProperty("trafficJson", trafficJsonObject != null ? trafficJsonObject.toString() : "{}");
        jsonObject.addProperty("emergencyJson", emergencyJsonObject != null ? emergencyJsonObject.toString() : "{}");
        jsonObject.addProperty("yeonhapJson", yeonhapJsonObject != null ? yeonhapJsonObject.toString() : "{}");
        jsonObject.addProperty("applicationJson", getApplicationJsonObject().toString());
        return jsonObject;
    }

    @Override
    public JsonObject getApplicationWrapperJson() {
        JsonObject root = new JsonObject();
        root.addProperty("applicationJson", getApplicationJsonObject().toString());
        return root;
    }

    @Override
    public JsonObject getWeatherWrapperJson() {
        JsonObject root = new JsonObject();
        root.addProperty("weatherJson", weatherJsonObject != null ? weatherJsonObject.toString() : "{}");
        return root;
    }

    // ===== \uc870\ud68c =====

    @Override
    public JsonObject getDashboardJsonObject() {
        long currentTime = System.currentTimeMillis();
        JsonObject jsonObject = new JsonObject();

        try {
            if (currentTime - lastWeatherUpdate >= WEATHER_UPDATE_INTERVAL) {
                updateWeatherData();
                lastWeatherUpdate = currentTime;
            }
            jsonObject.addProperty("weatherJson", weatherJsonObject != null ? weatherJsonObject.toString() : "{}");

            if (currentTime - lastTrafficUpdate >= TRAFFIC_UPDATE_INTERVAL) {
                renewTrafficJsonObject();
                lastTrafficUpdate = currentTime;
            }
            jsonObject.addProperty("trafficJson", trafficJsonObject != null ? trafficJsonObject.toString() : "{}");

            if (currentTime - lastEmergencyUpdate >= EMERGENCY_UPDATE_INTERVAL) {
                renewEmergencyJsonObject();
                lastEmergencyUpdate = currentTime;
            }
            jsonObject.addProperty("emergencyJson", emergencyJsonObject != null ? emergencyJsonObject.toString() : "{}");

            if (currentTime - lastNewsUpdate >= NEWS_UPDATE_INTERVAL) {
                renewNewsYeonhapJsonObject();
                lastNewsUpdate = currentTime;
            }
            jsonObject.addProperty("yeonhapJson", yeonhapJsonObject != null ? yeonhapJsonObject.toString() : "{}");

            if (currentTime - lastAirUpdate >= AIR_UPDATE_INTERVAL) {
                renewAirJsonObject();
                lastAirUpdate = currentTime;
            }
            jsonObject.addProperty("airJson", airJsonObject != null ? airJsonObject.toString() : "{}");

            jsonObject.addProperty("applicationJson", getApplicationJsonObject().toString());

            return jsonObject;
        } catch (Exception e) {
            log.error("\ub300\uc2dc\ubcf4\ub4dc \ub370\uc774\ud130 \uc870\ud569 \uc2e4\ud328: {}", e.getMessage());
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("weatherJson", "{}");
            errorJson.addProperty("trafficJson", "{}");
            errorJson.addProperty("emergencyJson", "{}");
            errorJson.addProperty("yeonhapJson", "{}");
            errorJson.addProperty("applicationJson", "{}");
            return errorJson;
        }
    }

    @Override
    public JsonObject getApplicationJsonObject() {
        JsonObject jsonObject = new JsonObject();
        SimpleDateFormat seoulSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        seoulSdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        long nowMs = System.currentTimeMillis();
        jsonObject.addProperty("currentTime", seoulSdf.format(new Timestamp(nowMs)));
        jsonObject.addProperty("serverNowMs", nowMs);
        jsonObject.addProperty("systemArchitecture", osBean.getArch());
        jsonObject.addProperty("systemName", osBean.getName());
        jsonObject.addProperty("systemVersion", osBean.getVersion());
        jsonObject.addProperty("systemLoadAverage", osBean.getSystemLoadAverage());
        jsonObject.addProperty("memory", totalMemory + "MB");
        jsonObject.addProperty("useMemory", usedMemory + "MB");
        jsonObject.addProperty("freeMemory", freeMemory + "MB");
        jsonObject.addProperty("availableProcessors", Runtime.getRuntime().availableProcessors());
        return jsonObject;
    }

    // ===== \ub0a0\uc528 =====

    @Override
    public JsonObject getWeatherJsonObject() {
        if (weatherJsonObject == null) {
            weatherJsonObject = jsonUtil.getJson(interfaceCore.getWeatherInfo());
        }
        return weatherJsonObject;
    }

    @Override
    public void renewWeatherJsonObject() {
        try {
            updateWeatherData();
            lastWeatherUpdate = System.currentTimeMillis();
            sinks.pushWeather(getWeatherWrapperJson());
        } catch (Exception e) {
            log.error("\ub0a0\uc528 \uc804\uccb4 \uac31\uc2e0 \uc2e4\ud328: {}", e.getMessage());
        }
    }

    /**
     * 도시별 날씨 병렬 조회 → weatherJsonObject 교체.
     * WebClient 동시 호출 수는 cities.properties 목록 크기에 맞춤.
     */
    private void updateWeatherData() {
        JsonObject next = new JsonObject();
        try {
            Flux.fromIterable(properties.getCities())
                    .index()
                    .flatMap(tuple -> {
                        OshProperties.City c = tuple.getT2();
                        long idx = tuple.getT1() + 1;
                        // OpenWeather \u00b7 KMA \ub3d9\uc2dc \ud638\ucd9c \u2192 zip
                        return weatherInterface.fetchWeather(c.getLat(), c.getLon()).defaultIfEmpty("")
                                .zipWith(fetchKmaSafe(c))
                                .map(t -> Tuples.of(idx, c.getName(), t.getT1(), t.getT2()))
                                .onErrorReturn(Tuples.of(idx, c.getName(), "", "{}"));
                    }, /* maxConcurrency */ Math.max(19, properties.getCities().size()))
                    .toStream()
                    .forEach(t -> {
                        long idx = t.getT1();
                        String name = t.getT2();
                        String raw = t.getT3();
                        String fcstSummary = t.getT4();
                        if (raw == null || raw.isBlank()) {
                            log.warn("city={} \ub0a0\uc528 \uc870\ud68c \ube48 \uc751\ub2f5", name);
                            return;
                        }
                        try {
                            JsonObject entry = jsonUtil.getJson(raw);
                            entry.addProperty("cityName", name);
                            if (fcstSummary != null && !fcstSummary.isBlank()) {
                                entry.addProperty("fcst", fcstSummary);
                            }
                            next.addProperty("weatherJson" + idx, entry.toString());
                        } catch (Exception parseEx) {
                            log.warn("city={} \ub0a0\uc528 \ud30c\uc2f1 \uc2e4\ud328: {}", name, parseEx.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("\ub0a0\uc528 \ubcd1\ub82c \uc870\ud68c \uc2e4\ud328: {}", e.getMessage());
        }
        weatherJsonObject = next;
    }

    /**
     * KMA \ub2e8\uae30\uc608\ubcf4 \uc870\ud68c \ud6c4 \ud504\ub860\ud2b8\uc5d0\uc11c \ubc14\ub85c \uc4f8 \uc218 \uc788\ub294 \uc694\uc57d\ud55c JSON \ubb38\uc790\uc5f4\uc744 \ub9cc\ub4e0\ub2e4.
     *
     *  \ucd9c\ub825 \uad6c\uc870 (\ubb38\uc790\uc5f4\uc740 ScheduledTasks weather \uac31\uc2e0 \uc8fc\uae30\uc640 \ub3d9\uc77c, 1\uc2dc\uac04\uc5d0 1\ud68c):
     *  {
     *    "today": { "tmn": "12", "tmx": "23", "pop": "20", "sky": "1" },
     *    "tomorrow": { "tmn": "10", "tmx": "21", "pop": "60", "sky": "3" }
     *  }
     *
     *  city.nx/ny \uac00 \uc5c6\uac70\ub098 KMA \ud0a4\uac00 \uc5c6\uc73c\uba74 \ube48 \ubb38\uc790\uc5f4 \ubc18\ud658.
     */
    private reactor.core.publisher.Mono<String> fetchKmaSafe(OshProperties.City c) {
        if (c.getNx() == null || c.getNy() == null) {
            return reactor.core.publisher.Mono.just("");
        }
        return weatherInterface.fetchShortForecast(c.getNx(), c.getNy())
                .map(this::summarizeKma)
                .onErrorReturn("");
    }

    private String summarizeKma(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            JsonObject root = jsonUtil.getJson(raw);
            if (root == null || !root.has("response")) return "";
            JsonObject body = root.getAsJsonObject("response").getAsJsonObject("body");
            if (body == null || !body.has("items")) return "";
            JsonArray items = body.getAsJsonObject("items").getAsJsonArray("item");

            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
            java.time.LocalDate tomorrow = today.plusDays(1);
            String d0 = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String d1 = tomorrow.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

            JsonObject t0 = new JsonObject();
            JsonObject t1 = new JsonObject();

            for (int i = 0; i < items.size(); i++) {
                JsonObject it = items.get(i).getAsJsonObject();
                String fcstDate = it.has("fcstDate") ? it.get("fcstDate").getAsString() : "";
                String category = it.has("category") ? it.get("category").getAsString() : "";
                String value    = it.has("fcstValue") ? it.get("fcstValue").getAsString() : "";
                JsonObject bucket = fcstDate.equals(d0) ? t0 : (fcstDate.equals(d1) ? t1 : null);
                if (bucket == null) continue;
                switch (category) {
                    case "TMN": bucket.addProperty("tmn", value); break;
                    case "TMX": bucket.addProperty("tmx", value); break;
                    case "POP":
                        if (!bucket.has("pop")) bucket.addProperty("pop", value); break;
                    case "SKY":
                        if (!bucket.has("sky")) bucket.addProperty("sky", value); break;
                    case "PTY":
                        if (!bucket.has("pty")) bucket.addProperty("pty", value); break;
                    default: break;
                }
            }

            JsonObject out = new JsonObject();
            out.add("today", t0);
            out.add("tomorrow", t1);
            return out.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ===== \uad50\ud1b5 =====

    @Override
    public JsonObject getTrafficWrapperJson() {
        JsonObject jsonObject = new JsonObject();
        JsonObject data = getTrafficJsonObject();
        jsonObject.addProperty("trafficJson", data != null ? data.toString() : "{}");
        return jsonObject;
    }

    @Override
    public JsonObject getTrafficJsonObject() {
        try {
            if (trafficJsonObject == null) {
                trafficJsonObject = jsonUtil.getJson(interfaceCore.getTrafficInfo());
            }
            return trafficJsonObject;
        } catch (Exception e) {
            log.error("\uad50\ud1b5 \ub370\uc774\ud130 \uc870\ud68c \uc2e4\ud328: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void renewTrafficJsonObject() {
        try {
            String trafficInfo = interfaceCore.getTrafficInfo();
            if (trafficInfo != null && !trafficInfo.trim().isEmpty()) {
                trafficJsonObject = jsonUtil.getJson(trafficInfo);
                sinks.pushTraffic(getTrafficWrapperJson());
            } else {
                log.warn("\uad50\ud1b5 \uc815\ubcf4\uac00 \ube44\uc5b4\uc788\uc74c. \uae30\uc874 \uce90\uc2dc \uc720\uc9c0");
            }
        } catch (Exception e) {
            log.error("\uad50\ud1b5 \uac31\uc2e0 \uc2e4\ud328: {}", e.getMessage());
        }
    }

    // ===== \uc7ac\ub09c =====

    @Override
    public JsonObject getEmergencyWrapperJson() {
        JsonObject jsonObject = new JsonObject();
        JsonObject data = getEmergencyJsonObject();
        jsonObject.addProperty("emergencyJson", data != null ? data.toString() : "{}");
        return jsonObject;
    }

    @Override
    public JsonObject getEmergencyJsonObject() {
        try {
            if (emergencyJsonObject == null) {
                emergencyJsonObject = jsonUtil.getJson(interfaceCore.getEmergencyInfo());
            }
            return emergencyJsonObject;
        } catch (Exception e) {
            log.error("\uae34\uae09\uc7ac\ub09c \uc870\ud68c \uc2e4\ud328: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void renewEmergencyJsonObject() {
        try {
            String info = interfaceCore.getEmergencyInfo();
            if (info != null && !info.trim().isEmpty()) {
                emergencyJsonObject = jsonUtil.getJson(info);
                sinks.pushEmergency(getEmergencyWrapperJson());
            } else {
                log.warn("\uae34\uae09\uc7ac\ub09c \uc815\ubcf4\uac00 \ube44\uc5b4\uc788\uc74c. \uae30\uc874 \uce90\uc2dc \uc720\uc9c0");
            }
        } catch (Exception e) {
            log.error("\uae34\uae09\uc7ac\ub09c \uac31\uc2e0 \uc2e4\ud328: {}", e.getMessage());
        }
    }

    // ===== \ub274\uc2a4 =====

    @Override
    public JsonObject getYeonhapWrapperJson() {
        JsonObject jsonObject = new JsonObject();
        JsonObject data = getNewsYeonhapJsonObject();
        jsonObject.addProperty("yeonhapJson", data != null ? data.toString() : "{}");
        return jsonObject;
    }

    @Override
    public JsonObject getNewsYeonhapJsonObject() {
        try {
            if (yeonhapJsonObject == null) {
                yeonhapJsonObject = buildNewsObject();
            }
            return yeonhapJsonObject;
        } catch (Exception e) {
            log.error("\ub274\uc2a4 \uc870\ud68c \uc2e4\ud328: {}", e.getMessage());
            return wrapInitMessage("\ub370\uc774\ud130 \ub85c\ub4dc \uc911 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4", "\uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574\uc8fc\uc138\uc694.");
        }
    }

    @Override
    public void renewNewsYeonhapJsonObject() {
        try {
            yeonhapJsonObject = buildNewsObject();
            sinks.pushYeonhap(getYeonhapWrapperJson());
        } catch (Exception e) {
            log.error("\ub274\uc2a4 \uac31\uc2e0 \uc2e4\ud328: {}", e.getMessage());
            yeonhapJsonObject = wrapInitMessage("\ub370\uc774\ud130 \uac31\uc2e0 \uc911 \uc624\ub958\uac00 \ubc1c\uc0dd\ud588\uc2b5\ub2c8\ub2e4", "\uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574\uc8fc\uc138\uc694.");
        }
    }

    // ===== \ub300\uae30\uc9c8 =====

    @Override
    public JsonObject getAirWrapperJson() {
        JsonObject root = new JsonObject();
        root.addProperty("airJson", airJsonObject != null ? airJsonObject.toString() : "{}");
        return root;
    }

    @Override
    public JsonObject getAirJsonObject() {
        if (airJsonObject == null || airJsonObject.size() == 0) {
            updateAirData();
        }
        return airJsonObject;
    }

    @Override
    public void renewAirJsonObject() {
        try {
            updateAirData();
            lastAirUpdate = System.currentTimeMillis();
            sinks.pushAir(getAirWrapperJson());
        } catch (Exception e) {
            log.error("\ub300\uae30\uc9c8 \uac31\uc2e0 \uc2e4\ud328: {}", e.getMessage());
        }
    }

    /**
     * AirKorea \uc758 \uc2dc\ub3c4\ubcc4 \uce21\uc815\uc18c \uc751\ub2f5 \u2192 \uc2dc\ub3c4\ubcc4 \ub300\ud45c\uac12(\ud3c9\uade0) \uc73c\ub85c \uc555\ucd95.
     * \ucd9c\ub825 \uad6c\uc870 (airJsonObject):
     *  { sido: [ {name, pm10, pm25, khaiGrade, station, dataTime}, ... ] }
     */
    private void updateAirData() {
        try {
            String raw = airInterface.getAirInfo();
            JsonObject root = jsonUtil.getJson(raw);
            JsonArray items = new JsonArray();
            if (root != null
                    && root.has("response")
                    && root.getAsJsonObject("response").has("body")
                    && root.getAsJsonObject("response").getAsJsonObject("body").has("items")) {
                items = root.getAsJsonObject("response").getAsJsonObject("body").getAsJsonArray("items");
            }

            // \uc2dc\ub3c4\ubcc4 \uadf8\ub8f9 \u2014 PM10/PM2.5 \ud3c9\uade0 + \ucd5c\uc545 \ub4f1\uae09
            java.util.Map<String, java.util.List<JsonObject>> bySido = new java.util.LinkedHashMap<>();
            for (int i = 0; i < items.size(); i++) {
                JsonObject it = items.get(i).getAsJsonObject();
                String sido = it.has("sidoName") && !it.get("sidoName").isJsonNull() ? it.get("sidoName").getAsString() : "?";
                bySido.computeIfAbsent(sido, k -> new java.util.ArrayList<>()).add(it);
            }

            JsonArray sidoArr = new JsonArray();
            for (java.util.Map.Entry<String, java.util.List<JsonObject>> e : bySido.entrySet()) {
                JsonObject agg = new JsonObject();
                String sido = e.getKey();
                java.util.List<JsonObject> list = e.getValue();
                int pm10Sum = 0, pm10N = 0, pm25Sum = 0, pm25N = 0, worstGrade = 0;
                String latestTime = "";
                String repStation = "-";
                for (JsonObject it : list) {
                    pm10Sum += parseInt(it, "pm10Value"); if (parseInt(it, "pm10Value") > 0) pm10N++;
                    pm25Sum += parseInt(it, "pm25Value"); if (parseInt(it, "pm25Value") > 0) pm25N++;
                    int g = parseInt(it, "khaiGrade");
                    if (g > worstGrade) worstGrade = g;
                    String t = it.has("dataTime") && !it.get("dataTime").isJsonNull() ? it.get("dataTime").getAsString() : "";
                    if (t.compareTo(latestTime) > 0) {
                        latestTime = t;
                        if (it.has("stationName") && !it.get("stationName").isJsonNull()) {
                            repStation = it.get("stationName").getAsString();
                        }
                    }
                }
                agg.addProperty("name", sido);
                agg.addProperty("pm10", pm10N > 0 ? pm10Sum / pm10N : -1);
                agg.addProperty("pm25", pm25N > 0 ? pm25Sum / pm25N : -1);
                agg.addProperty("khaiGrade", worstGrade);
                agg.addProperty("station", repStation);
                agg.addProperty("dataTime", latestTime);
                sidoArr.add(agg);
            }

            JsonObject next = new JsonObject();
            next.add("sido", sidoArr);
            airJsonObject = next;
        } catch (Exception e) {
            log.warn("\ub300\uae30\uc9c8 \ud30c\uc2f1 \uc2e4\ud328: {}", e.getMessage());
            if (airJsonObject == null) airJsonObject = new JsonObject();
        }
    }

    private int parseInt(JsonObject obj, String key) {
        try {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
            String s = obj.get(key).getAsString();
            if (s == null || s.isBlank() || "-".equals(s)) return 0;
            return Integer.parseInt(s.trim());
        } catch (Exception e) { return 0; }
    }

    private JsonObject buildNewsObject() {
        JsonObject root = new JsonObject();
        JsonObject data = new JsonObject();
        if (newsService != null) {
            data.add("items", newsService.getCachedNews());
        } else {
            log.warn("NewsService is null \u2014 \uc784\uc2dc \uc744 \uba54\uc2dc\uc9c0\ub85c \uad50\uccb4");
            data.add("items", emptyNewsArray("\uc11c\ube44\uc2a4 \ucd08\uae30\ud654 \uc911\uc785\ub2c8\ub2e4..."));
        }
        root.add("data", data);
        return root;
    }

    // ===== \ud5ec\ud37c =====

    private JsonObject wrapEmptyItems() {
        JsonObject root = new JsonObject();
        root.add("items", new JsonArray());
        return root;
    }

    private JsonObject wrapInitMessage(String title, String content) {
        JsonObject root = new JsonObject();
        JsonObject data = new JsonObject();
        data.add("items", emptyNewsArray(title, content));
        root.add("data", data);
        return root;
    }

    private JsonArray emptyNewsArray(String title) {
        return emptyNewsArray(title, "\uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \uc2dc\ub3c4\ud574\uc8fc\uc138\uc694.");
    }

    private JsonArray emptyNewsArray(String title, String content) {
        JsonArray arr = new JsonArray();
        JsonObject obj = new JsonObject();
        obj.addProperty("createDT", "");
        obj.addProperty("company", "");
        obj.addProperty("title", title);
        obj.addProperty("content", content);
        arr.add(obj);
        return arr;
    }
}
