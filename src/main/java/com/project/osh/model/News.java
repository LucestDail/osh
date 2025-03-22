package com.project.osh.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bs4news_news")
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "News_from")
    private String newsFrom;

    @Column(name = "News_title")
    private String newsTitle;

    @Column(name = "News_company")
    private String newsCompany;

    @Column(name = "News_contents", columnDefinition = "LONGTEXT")
    private String newsContents;

    @Column(name = "News_CreateDT")
    private LocalDateTime newsCreateDT;

    @Column(name = "ETC1", columnDefinition = "LONGTEXT")
    private String etc1;

    @Column(name = "ETC2", columnDefinition = "LONGTEXT")
    private String etc2;

    @Column(name = "ETC3", columnDefinition = "LONGTEXT")
    private String etc3;

    @Column(name = "ETC4", columnDefinition = "LONGTEXT")
    private String etc4;

    @Column(name = "ETC5", columnDefinition = "LONGTEXT")
    private String etc5;

    @Column(name = "News_contents_raw", columnDefinition = "LONGTEXT")
    private String newsContentsRaw;
} 