package com.project.osh.interfaces;

import org.springframework.stereotype.Component;

/**
 * \uc678\ubd80 API \ud638\ucd9c facade. \uacb0\uacfc \ubb38\uc790\uc5f4(JSON \uadf8\ub300\ub85c)\ub97c \ubc18\ud658.
 * \uac1c\uc6d0: \uac01 Interface \ub294 Spring Bean \uc73c\ub85c \uad00\ub9ac\ub418\uc5b4 @Value \uc8fc\uc785\uc774 \uc81c\ub300\ub85c \ub3d9\uc791\ud55c\ub2e4.
 */
@Component
public class InterfaceCore {

    private final WeatherInterface weather;
    private final EmergencyInterface emergency;
    private final TrafficInterface traffic;
    private final NewsInterface news;

    public InterfaceCore(WeatherInterface weather,
                         EmergencyInterface emergency,
                         TrafficInterface traffic,
                         NewsInterface news) {
        this.weather = weather;
        this.emergency = emergency;
        this.traffic = traffic;
        this.news = news;
    }

    public String getWeatherInfo() {
        return weather.getOpenweathermap();
    }

    public String getWeatherInfo(String lat, String lon) {
        return weather.getOpenweathermap(lat, lon);
    }

    public String getEmergencyInfo() {
        return emergency.getEmergencyInfo();
    }

    public String getTrafficInfo() {
        return traffic.getTrafficInfo();
    }

    public String getYeonhapInfo() {
        return news.getYeonhapNews();
    }
}
