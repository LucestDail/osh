package com.project.osh.interfaces;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.osh.controller.DashboardController;
import com.project.osh.util.HttpUtil;

public class TrafficInterface {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

     public String getTrafficInfo(){

		log.info("{} >> TrafficInterface.getTrafficInfo", dateFormat.format(new Date()));
        String strTrafficInfo = "";
		try {
	        strTrafficInfo = new HttpUtil().executeGet("https://openapi.its.go.kr:9443/eventInfo?apiKey=409b7fa7f23c4baf956e166d78b97726&type=all&eventType=all&getType=json");
		}catch(Exception e) {
			e.printStackTrace();
		}
        return strTrafficInfo;
    }
}
