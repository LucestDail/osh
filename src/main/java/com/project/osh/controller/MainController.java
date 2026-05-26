package com.project.osh.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin
@RequestMapping
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Value("${osh.logging}")
    private boolean loggingFlag;
    
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView getMain(Model model) {
        ModelAndView mav = new ModelAndView();
		mav.setViewName("dashboard/main");
		return mav;
    }

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public ModelAndView getDashboard(Model model) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("dashboard");
        return mav;
    }

    @RequestMapping(value = "/afterWork", method = RequestMethod.GET)
    public ModelAndView getAfterWork(Model model) {
        ModelAndView mav = new ModelAndView();
		mav.setViewName("dashboard/afterWork");
		return mav;
    }

    @RequestMapping(value = "/beforeWork", method = RequestMethod.GET)
    public ModelAndView getBoforeWork(Model model) {
        ModelAndView mav = new ModelAndView();
		mav.setViewName("dashboard/beforeWork");
		return mav;
    }

    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
	public ModelAndView login(Model model) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("main/login");
		return mav;
	}
    
    @RequestMapping(value = "/jsoup", method = RequestMethod.GET)
    public String jsoup(Model model){
        String strUrl = "http://google.com";
        String strHtml = "";
        Document doc = null;
        try{
            doc = Jsoup.connect(strUrl).get();
        }catch(Exception e){
            log.error("jsoup fetch \uc2e4\ud328: {}", e.getMessage());
        }
        if(doc != null){
            strHtml = doc.html();
        }
        return strHtml;
    }
}