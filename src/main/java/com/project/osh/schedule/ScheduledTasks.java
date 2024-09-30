package com.project.osh.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.osh.controller.DashboardController;
import com.project.osh.service.DashboardService;

@Component
public class ScheduledTasks {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Autowired DashboardService dashboardService;

	@Scheduled(fixedDelay = 60000)
	public void renewObjects() {
		log.info("{} >> ScheduledTasks.renewObjects", dateFormat.format(new Date()));
		dashboardService.renewEmergencyJsonObject();
		dashboardService.renewTrafficJsonObject();
		dashboardService.renewNewsYeonhapJsonObject();
		dashboardService.renewWeatherJsonObject1("35.221316","128.682037");
		dashboardService.renewWeatherJsonObject2("37.245807","127.057375");
	}
}