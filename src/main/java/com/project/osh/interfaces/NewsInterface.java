package com.project.osh.interfaces;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.osh.controller.DashboardController;
import com.project.osh.util.HttpUtil;

public class NewsInterface {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

     public String getYeonhapNews(){

		log.info("{} >> NewsInterface.getYeonhapNews", dateFormat.format(new Date()));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar c1 = Calendar.getInstance();
        String strToday = sdf.format(c1.getTime());
        String strYeonhapNews = "";
		try {
	        strYeonhapNews = new HttpUtil().executeGet("https://www.safetydata.go.kr/V2/api/DSSP-IF-00051?serviceKey=0J2DA743WA9JIQIP&inqDt="+strToday);
		}catch(Exception e) {
			e.printStackTrace();
		}
        return strYeonhapNews;
    }
    
}
