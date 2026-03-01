package com.conflictview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictStatsDTO {
    private UUID conflictId;
    private long totalArticles;
    private long totalEvents;
    private Map<String, Long> articlesBySource;
    private Map<String, Long> sentimentBreakdown;
    private Map<String, Long> eventsByType;
    private List<MonthlyDataDTO> monthlyTrend;
    private Double averageReliabilityScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDataDTO {
        private String month;   // "2024-01"
        private long incidents;
        private long casualties;
    }
}
