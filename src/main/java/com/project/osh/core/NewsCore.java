package com.project.osh.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.json.JSONObject;
import org.json.JSONArray;

@Component
public class NewsCore {
    private static final Logger log = LoggerFactory.getLogger(NewsCore.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean loggingFlag = true;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public JSONObject getNewsYeonhap() {
        JSONObject result = new JSONObject();
        try {
            String sql = "SELECT n.*, c.News_Company_Name " +
                        "FROM mytools.bs4news_news n " +
                        "LEFT JOIN mytools.bs4news_news_company c " +
                        "ON n.News_company = c.News_Company_Code " +
                        "ORDER BY n.News_CreateDT DESC " +
                        "LIMIT 10";

            JSONArray newsArray = new JSONArray();
            jdbcTemplate.query(sql, (rs, rowNum) -> {
                JSONObject newsItem = new JSONObject();
                newsItem.put("createDT", rs.getString("News_CreateDT"));
                newsItem.put("company", rs.getString("News_Company_Name"));
                newsItem.put("title", rs.getString("News_title"));
                newsItem.put("content", rs.getString("News_contents"));
                newsItem.put("link", rs.getString("News_from"));
                newsArray.put(newsItem);
            });

            result.put("data", new JSONObject().put("items", newsArray));
            log.info("뉴스 데이터 조회 완료: {}건", newsArray.length());
        } catch (Exception e) {
            log.error("뉴스 데이터 조회 중 오류 발생: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }
} 