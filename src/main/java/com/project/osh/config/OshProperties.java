package com.project.osh.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * osh \uc560\ud50c\ub9ac\ucf00\uc774\uc158 \ub3c4\uba54\uc778 \uc124\uc815.
 * - cities: \ub300\uc2dc\ubcf4\ub4dc\uac00 \uc8fc\uae30\uc801\uc73c\ub85c \ub0a0\uc528\ub97c \uc870\ud68c\ud558\ub294 \uc804\uad6d \uc8fc\uc694 \ub3c4\uc2dc.
 *   3\uc911 \uc911\ubcf5(\uc774\uc804: weather.properties, DashboardServiceImpl, GeminiService) \uc77c\uc6d0\ud654.
 */
@Component
@PropertySource("classpath:cities.properties")
@ConfigurationProperties(prefix = "osh")
public class OshProperties {

    private List<City> cities = new ArrayList<>();

    public List<City> getCities() {
        return cities;
    }

    public void setCities(List<City> cities) {
        this.cities = cities;
    }

    public static class City {
        private String name;
        private String lat;
        private String lon;
        /** \uae30\uc0c1\uccad \uaca9\uc790 X (nx). KMA \ub2e8\uae30\uc608\ubcf4 \uc870\ud68c\uc6a9. */
        private Integer nx;
        /** \uae30\uc0c1\uccad \uaca9\uc790 Y (ny). */
        private Integer ny;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLat() {
            return lat;
        }

        public void setLat(String lat) {
            this.lat = lat;
        }

        public String getLon() {
            return lon;
        }

        public void setLon(String lon) {
            this.lon = lon;
        }

        public Integer getNx() {
            return nx;
        }

        public void setNx(Integer nx) {
            this.nx = nx;
        }

        public Integer getNy() {
            return ny;
        }

        public void setNy(Integer ny) {
            this.ny = ny;
        }
    }
}
