package com.project.osh.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Value("${osh.logging}")
    private boolean loggingFlag;

    @GetMapping("/")
    public ModelAndView getMain(Model model) {
        return new ModelAndView("dashboard/main");
    }

    @GetMapping("/dashboard")
    public ModelAndView getDashboard(Model model) {
        return new ModelAndView("dashboard");
    }
}
