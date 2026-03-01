package com.conflictview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalActiveConflicts;
    private long criticalConflicts;
    private long highConflicts;
    private long mediumConflicts;
    private long lowConflicts;
    private long monitoringConflicts;
    private long totalArticlesToday;
    private long totalArticlesAllTime;
    private List<ConflictMapDTO> topConflictsBySeverity;
    private List<NewsArticleDTO> latestArticles;
    private Map<String, Long> conflictsByRegion;
    private Map<String, Long> conflictsByType;
    private List<MonthlyCasualtyDTO> monthlyCasualties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyCasualtyDTO {
        private String month;
        private long incidents;
        private long casualties;
    }
}
