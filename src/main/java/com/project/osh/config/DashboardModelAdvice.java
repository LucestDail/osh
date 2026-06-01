package com.project.osh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.project.osh.controller.DashboardController;
import com.project.osh.controller.MainController;

/**
 * 대시보드 HTML에 정적 리소스 캐시 버전을 주입한다.
 */
@ControllerAdvice(assignableTypes = { MainController.class, DashboardController.class })
public class DashboardModelAdvice {

    @Value("${osh.static.version:dev}")
    private String staticVersion;

    @ModelAttribute("staticVersion")
    public String staticVersion() {
        return staticVersion;
    }
}
