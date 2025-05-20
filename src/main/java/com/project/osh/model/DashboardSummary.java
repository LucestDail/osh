package com.project.osh.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class DashboardSummary {
    private String newsSummary;
    private String marketTrends;
    private String keyHighlights;
    private String recommendations;
    private String comprehensiveAnalysis;
} 