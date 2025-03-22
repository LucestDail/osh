package com.project.osh.repository.news;

import com.project.osh.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    @Query(value = "SELECT * FROM bs4news_news ORDER BY News_CreateDT DESC", nativeQuery = true)
    List<News> findAllOrderByNewsCreateDTDesc();
    
    @Query(value = "SELECT * FROM bs4news_news WHERE News_company = :company ORDER BY News_CreateDT DESC", nativeQuery = true)
    List<News> findByNewsCompanyOrderByNewsCreateDTDesc(String company);

    @Query(value = "SELECT * FROM bs4news_news ORDER BY News_CreateDT DESC LIMIT 100", nativeQuery = true)
    List<News> findTop100OrderByNewsCreateDTDesc();
    
    @Query(value = "SELECT * FROM bs4news_news WHERE News_company = :company ORDER BY News_CreateDT DESC LIMIT 100", nativeQuery = true)
    List<News> findTop100ByNewsCompanyOrderByNewsCreateDTDesc(@Param("company") String company);
} 