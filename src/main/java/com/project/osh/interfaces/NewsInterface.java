package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.project.osh.controller.DashboardController;
import com.project.osh.util.HttpUtil;

public class NewsInterface {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Value("${osh.logging}")
    private boolean loggingFlag;

    public String getYeonhapNews(){
		if(loggingFlag){
			log.info("{} >> NewsInterface.getYeonhapNews", dateFormat.format(new Date()));
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		java.util.TimeZone seoul = java.util.TimeZone.getTimeZone ( "Asia/Seoul" );
        sdf.setTimeZone ( seoul ) ;
        String strToday = sdf.format(System.currentTimeMillis());
        String strYeonhapNews = "";
		try {
			if(loggingFlag){
				log.info("{} >> getYeonhapNews.request", "https://www.safetydata.go.kr/V2/api/DSSP-IF-00051?serviceKey=0J2DA743WA9JIQIP&inqDt="+strToday);
			}
	        strYeonhapNews = new HttpUtil().executeGet("https://www.safetydata.go.kr/V2/api/DSSP-IF-00051?serviceKey=0J2DA743WA9JIQIP&inqDt="+strToday);
		}catch(Exception e) {
			e.printStackTrace();
		}
        return strYeonhapNews;
    }
    
}
