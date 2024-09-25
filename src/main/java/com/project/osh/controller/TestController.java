package com.project.osh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonObject;
import com.project.osh.service.TestService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@RestController
@CrossOrigin
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private TestService testService;
    
    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
	public ModelAndView login(Model model) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("main/login");
		return mav;
	}
    
    @RequestMapping(value = "/weather", method = RequestMethod.GET)
    public JsonObject weather(Model model){
        return testService.getWeatherJsonObject();
    }
}