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
        log.info("{} >> DashboardController.getMain", dateFormat.format(new Date()));
        ModelAndView mav = new ModelAndView();
		mav.setViewName("dashboard/main");
		return mav;
    }

    @RequestMapping(value = "/main/info", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getMainInfo() {
        log.info("{} >> DashboardController.getMainInfo", dateFormat.format(new Date()));
        return Flux.interval(Duration.ofSeconds(1)).map(i -> ServerSentEvent.builder(dashboardService.getDashboardJsonObject().toString()).build());
    }

    @RequestMapping(value = "/main/emergency", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getEmergencyInfo() {
        log.info("{} >> DashboardController.getEmergencyInfo", dateFormat.format(new Date()));
        return Flux.interval(Duration.ofSeconds(60)).map(i -> ServerSentEvent.builder(dashboardService.getEmergencyWrapperJson().toString()).build());
    }

    @RequestMapping(value = "/main/traffic", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getTrafficInfo() {
        log.info("{} >> DashboardController.getTrafficInfo", dateFormat.format(new Date()));
        return Flux.interval(Duration.ofSeconds(60)).map(i -> ServerSentEvent.builder(dashboardService.getTrafficWrapperJson().toString()).build());
    }

    @RequestMapping(value = "/main/yeonhap", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getYeonhapInfo() {
        log.info("{} >> DashboardController.getYeonhapInfo", dateFormat.format(new Date()));
        return Flux.interval(Duration.ofSeconds(60)).map(i -> ServerSentEvent.builder(dashboardService.getYeonhapWrapperJson().toString()).build());
    }

    @RequestMapping(value = "/main", method = RequestMethod.POST)
    public String postMain(@RequestBody String entity) {
        log.info("{} >> DashboardController.postMain", dateFormat.format(new Date()));
        return entity;
    }

    @RequestMapping(value = "/weather", method = RequestMethod.GET)
    public ModelAndView getWeather() {
        
        log.info("{} >> DashboardController.getWeather", dateFormat.format(new Date()));

        ModelAndView mav = new ModelAndView();
		mav.setViewName("dashboard/weather");

        return mav;
    }

    @RequestMapping(value = "/weather/info", method = RequestMethod.GET)
    public Flux<ServerSentEvent<String>> getWeatherInfo() {
        log.info("{} >> DashboardController.getWeatherInfo", dateFormat.format(new Date()));
        return Flux.interval(Duration.ofSeconds(1)).map(i -> ServerSentEvent.builder(dashboardService.getDashboardJsonObject().toString()).build());
    }
    
    
    

}
