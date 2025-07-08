package com.project.osh.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.osh.controller.DashboardController;
import com.project.osh.service.DashboardService;

@Component
public class ScheduledTasks {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Value("${osh.logging}")
    private boolean loggingFlag;

	@Autowired DashboardService dashboardService;

	@Scheduled(fixedDelay = 60000)
	public void renewObjects() {
		// 각 작업을 개별적으로 실행하여 한 작업의 실패가 다른 작업에 영향을 주지 않도록 함
		
		// 긴급재난문자 갱신
		try {
			dashboardService.renewEmergencyJsonObject();
		} catch (Exception e) {
			log.error("Error renewing emergency data in scheduled task: {}", e.getMessage());
		}
		
		// 교통정보 갱신
		try {
			dashboardService.renewTrafficJsonObject();
		} catch (Exception e) {
			log.error("Error renewing traffic data in scheduled task: {}", e.getMessage());
		}
		
		// 뉴스 갱신
		try {
			dashboardService.renewNewsYeonhapJsonObject();
		} catch (Exception e) {
			log.error("Error renewing news data in scheduled task: {}", e.getMessage());
		}
		
		// 날씨 정보1 갱신
		try {
			dashboardService.renewWeatherJsonObject1("35.221316","128.682037");
		} catch (Exception e) {
			log.error("Error renewing weather data1 in scheduled task: {}", e.getMessage());
		}
		
		// 날씨 정보2 갱신
		try {
			dashboardService.renewWeatherJsonObject2("37.245807","127.057375");
		} catch (Exception e) {
			log.error("Error renewing weather data2 in scheduled task: {}", e.getMessage());
		}
	}
}