package com.project.osh.controller;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.project.osh.service.DashboardService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
@CrossOrigin
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired DashboardService dashboardService;

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    
    @RequestMapping(value = "/main", method = RequestMethod.GET)
    public ModelAndView getMain(Model model) {
        ModelAndView mav = new ModelAndView();
		mav.setViewName("dashboard/main");
		return mav;
    }

    @RequestMapping(value = "/main/info", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getMainInfo() {
        return Flux.interval(Duration.ofSeconds(1))
            .onBackpressureDrop()
            .map(i -> {
                try {
                    String data = dashboardService.getDashboardJsonObject() != null ? 
                        dashboardService.getDashboardJsonObject().toString() : "{}";
                    return ServerSentEvent.builder(data).build();
                } catch (Exception e) {
                    log.error("Error getting dashboard info: {}", e.getMessage());
                    return ServerSentEvent.builder("{}").build();
                }
            })
            .onErrorResume(e -> {
                log.error("Error in dashboard info stream: {}", e.getMessage());
                return Flux.empty();
            })
            .onErrorReturn(ServerSentEvent.builder("{}").build())
            .doOnCancel(() -> log.debug("Dashboard info stream cancelled"))
            .doOnComplete(() -> log.debug("Dashboard info stream completed"));
    }

    @RequestMapping(value = "/main/emergency", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getEmergencyInfo() {
        return Flux.interval(Duration.ofSeconds(60))
            .onBackpressureDrop()
            .map(i -> {
                try {
                    String data = dashboardService.getEmergencyWrapperJson() != null ? 
                        dashboardService.getEmergencyWrapperJson().toString() : "{}";
                    return ServerSentEvent.builder(data).build();
                } catch (Exception e) {
                    log.error("Error getting emergency info: {}", e.getMessage());
                    return ServerSentEvent.builder("{}").build();
                }
            })
            .onErrorResume(e -> {
                log.error("Error in emergency info stream: {}", e.getMessage());
                return Flux.empty();
            })
            .onErrorReturn(ServerSentEvent.builder("{}").build())
            .doOnCancel(() -> log.debug("Emergency info stream cancelled"))
            .doOnComplete(() -> log.debug("Emergency info stream completed"));
    }

    @RequestMapping(value = "/main/traffic", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getTrafficInfo() {
        return Flux.interval(Duration.ofSeconds(60))
            .onBackpressureDrop()
            .map(i -> {
                try {
                    String data = dashboardService.getTrafficWrapperJson() != null ? 
                        dashboardService.getTrafficWrapperJson().toString() : "{}";
                    return ServerSentEvent.builder(data).build();
                } catch (Exception e) {
                    log.error("Error getting traffic info: {}", e.getMessage());
                    return ServerSentEvent.builder("{}").build();
                }
            })
            .onErrorResume(e -> {
                log.error("Error in traffic info stream: {}", e.getMessage());
                return Flux.empty();
            })
            .onErrorReturn(ServerSentEvent.builder("{}").build())
            .doOnCancel(() -> log.debug("Traffic info stream cancelled"))
            .doOnComplete(() -> log.debug("Traffic info stream completed"));
    }

    @RequestMapping(value = "/main/yeonhap", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getYeonhapInfo() {
        return Flux.interval(Duration.ofSeconds(60))
            .onBackpressureDrop()
            .map(i -> {
                try {
                    String data = dashboardService.getYeonhapWrapperJson() != null ? 
                        dashboardService.getYeonhapWrapperJson().toString() : "{}";
                    return ServerSentEvent.builder(data).build();
                } catch (Exception e) {
                    log.error("Error getting yeonhap info: {}", e.getMessage());
                    return ServerSentEvent.builder("{}").build();
                }
            })
            .onErrorResume(e -> {
                log.error("Error in yeonhap info stream: {}", e.getMessage());
                return Flux.empty();
            })
            .onErrorReturn(ServerSentEvent.builder("{}").build())
            .doOnCancel(() -> log.debug("Yeonhap info stream cancelled"))
            .doOnComplete(() -> log.debug("Yeonhap info stream completed"));
    }

    @RequestMapping(value = "/main", method = RequestMethod.POST)
    public String postMain(@RequestBody String entity) {
        return entity;
    }

    @RequestMapping(value = "/weather", method = RequestMethod.GET)
    public ModelAndView getWeather() {
        ModelAndView mav = new ModelAndView();
		mav.setViewName("dashboard/weather");
        return mav;
    }

    @RequestMapping(value = "/weather/info", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getWeatherInfo() {
        return Flux.interval(Duration.ofSeconds(1))
            .onBackpressureDrop()
            .map(i -> {
                try {
                    String data = dashboardService.getDashboardJsonObject() != null ? 
                        dashboardService.getDashboardJsonObject().toString() : "{}";
                    return ServerSentEvent.builder(data).build();
                } catch (Exception e) {
                    log.error("Error getting weather info: {}", e.getMessage());
                    return ServerSentEvent.builder("{}").build();
                }
            })
            .onErrorResume(e -> {
                log.error("Error in weather info stream: {}", e.getMessage());
                return Flux.empty();
            })
            .onErrorReturn(ServerSentEvent.builder("{}").build())
            .doOnCancel(() -> log.debug("Weather info stream cancelled"))
            .doOnComplete(() -> log.debug("Weather info stream completed"));
    }
}
