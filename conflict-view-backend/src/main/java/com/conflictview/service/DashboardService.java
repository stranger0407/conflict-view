package com.conflictview.service;

import com.conflictview.dto.DashboardStatsDTO;
import com.conflictview.dto.ConflictMapDTO;
import com.conflictview.dto.NewsArticleDTO;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.Severity;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ConflictRepository conflictRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final ConflictService conflictService;
    private final ReliabilityService reliabilityService;

    @Cacheable("dashboardStats")
    public DashboardStatsDTO getDashboardStats() {
        long active = conflictRepository.countByStatus(ConflictStatus.ACTIVE);
        long monitoring = conflictRepository.countByStatus(ConflictStatus.MONITORING);
        long critical = conflictRepository.countActiveBySeverity(Severity.CRITICAL);
        long high = conflictRepository.countActiveBySeverity(Severity.HIGH);
        long medium = conflictRepository.countActiveBySeverity(Severity.MEDIUM);
        long low = conflictRepository.countActiveBySeverity(Severity.LOW);

        long totalArticles = newsArticleRepository.count();

        // Top conflicts (first 5 by severity)
        List<ConflictMapDTO> top = conflictService.getAllForMap().stream()
                .sorted(Comparator.comparing(ConflictMapDTO::getSeverity).reversed())
                .limit(5)
                .toList();

        // Latest articles
        var latestArticlesPage = newsArticleRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt")));
        List<NewsArticleDTO> latestArticles = latestArticlesPage.stream()
                .map(a -> {
                    int score = a.getReliabilityScore() != null ? a.getReliabilityScore()
                            : reliabilityService.getScoreForDomain(
                            a.getSourceDomain() != null ? a.getSourceDomain() : "");
                    return NewsArticleDTO.builder()
                            .id(a.getId())
                            .conflictId(a.getConflict().getId())
                            .title(a.getTitle())
                            .url(a.getUrl())
                            .sourceName(a.getSourceName())
                            .sourceDomain(a.getSourceDomain())
                            .publishedAt(a.getPublishedAt())
                            .reliabilityScore(score)
                            .reliabilityLabel(reliabilityService.getLabel(score))
                            .reliabilityColor(reliabilityService.getColor(score))
                            .sentiment(a.getSentiment())
                            .imageUrl(a.getImageUrl())
                            .build();
                }).toList();

        // Conflicts by region
        Map<String, Long> byRegion = conflictRepository.findByStatus(ConflictStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getRegion() != null ? c.getRegion() : "Unknown",
                        Collectors.counting()));

        // Conflicts by type
        Map<String, Long> byType = conflictRepository.findByStatus(ConflictStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getConflictType().name(),
                        Collectors.counting()));

        return DashboardStatsDTO.builder()
                .totalActiveConflicts(active)
                .criticalConflicts(critical)
                .highConflicts(high)
                .mediumConflicts(medium)
                .lowConflicts(low)
                .monitoringConflicts(monitoring)
                .totalArticlesAllTime(totalArticles)
                .topConflictsBySeverity(top)
                .latestArticles(latestArticles)
                .conflictsByRegion(byRegion)
                .conflictsByType(byType)
                .build();
    }
}
