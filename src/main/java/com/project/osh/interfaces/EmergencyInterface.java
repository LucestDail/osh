package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.project.osh.controller.DashboardController;
import com.project.osh.util.HttpUtil;

public class EmergencyInterface {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Value("${osh.logging}")
    private boolean loggingFlag;

    public String getEmergencyInfo() {
		if(loggingFlag) {
			log.info("{} >> EmergencyInterface.getEmergencyInfo", dateFormat.format(new Date()));
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		java.util.TimeZone seoul = java.util.TimeZone.getTimeZone ( "Asia/Seoul" );
        sdf.setTimeZone ( seoul ) ;
        String strToday = sdf.format(System.currentTimeMillis());
        String strTrafficInfo = "";
		try {
			if(loggingFlag) {
				log.info("{} >> getEmergencyInfo.request", "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?serviceKey=7DCBUF3EBA0Y6WQ1&crtDt="+strToday);
			}
	        strTrafficInfo = new HttpUtil().executeGet("https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?serviceKey=7DCBUF3EBA0Y6WQ1&crtDt="+strToday);
		}catch(Exception e) {
			e.printStackTrace();
		}

        return strTrafficInfo;
    }
}
