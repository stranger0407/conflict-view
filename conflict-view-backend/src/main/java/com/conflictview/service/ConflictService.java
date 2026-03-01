package com.conflictview.service;

import com.conflictview.dto.*;
import com.conflictview.model.Conflict;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ConflictType;
import com.conflictview.model.enums.Severity;
import com.conflictview.repository.ConflictEventRepository;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.NewsArticleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConflictService {

    private final ConflictRepository conflictRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final ConflictEventRepository conflictEventRepository;
    private final ReliabilityService reliabilityService;
    private final SentimentService sentimentService;

    @Cacheable("conflicts")
    public List<ConflictMapDTO> getAllForMap() {
        return conflictRepository.findByStatusOrderBySeverityDescNameAsc(ConflictStatus.ACTIVE)
                .stream()
                .map(this::toMapDTO)
                .toList();
    }

    @Cacheable(value = "conflictDetail", key = "#id")
    public ConflictDetailDTO getDetail(UUID id) {
        Conflict c = conflictRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conflict not found: " + id));
        return toDetailDTO(c);
    }

    public Page<NewsArticleDTO> getNews(UUID conflictId, int page, int size,
                                        String sentimentStr, String sourceDomain) {
        var sentiment = sentimentStr != null && !sentimentStr.isBlank()
                ? com.conflictview.model.enums.SentimentType.valueOf(sentimentStr.toUpperCase())
                : null;

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return newsArticleRepository.findByConflictFiltered(conflictId, sentiment, sourceDomain, pageable)
                .map(article -> {
                    int score = article.getReliabilityScore() != null
                            ? article.getReliabilityScore()
                            : reliabilityService.getScoreForDomain(article.getSourceDomain() != null ? article.getSourceDomain() : "");
                    return NewsArticleDTO.builder()
                            .id(article.getId())
                            .conflictId(conflictId)
                            .title(article.getTitle())
                            .url(article.getUrl())
                            .sourceName(article.getSourceName())
                            .sourceDomain(article.getSourceDomain())
                            .author(article.getAuthor())
                            .description(article.getDescription())
                            .publishedAt(article.getPublishedAt())
                            .reliabilityScore(score)
                            .reliabilityLabel(reliabilityService.getLabel(score))
                            .reliabilityColor(reliabilityService.getColor(score))
                            .sentiment(article.getSentiment())
                            .imageUrl(article.getImageUrl())
                            .fetchedAt(article.getFetchedAt())
                            .build();
                });
    }

    public List<ConflictEventDTO> getEvents(UUID conflictId) {
        var dbEvents = conflictEventRepository.findByConflictIdOrderByEventDateDesc(conflictId);

        if (!dbEvents.isEmpty()) {
            return dbEvents.stream()
                    .map(e -> ConflictEventDTO.builder()
                            .id(e.getId())
                            .conflictId(conflictId)
                            .eventDate(e.getEventDate())
                            .eventType(e.getEventType())
                            .description(e.getDescription())
                            .sourceUrl(e.getSourceUrl())
                            .latitude(e.getLatitude())
                            .longitude(e.getLongitude())
                            .fatalitiesReported(e.getFatalitiesReported())
                            .build())
                    .toList();
        }

        // Fallback: generate timeline from news articles
        return newsArticleRepository.findByConflictIdOrderByPublishedAtDesc(conflictId)
                .stream()
                .limit(50)
                .map(a -> ConflictEventDTO.builder()
                        .id(a.getId())
                        .conflictId(conflictId)
                        .eventDate(a.getPublishedAt() != null ? a.getPublishedAt().toLocalDate() : java.time.LocalDate.now())
                        .eventType("News Report")
                        .description(a.getTitle())
                        .sourceUrl(a.getUrl())
                        .build())
                .toList();
    }

    public ConflictStatsDTO getStats(UUID conflictId) {
        var articlesBySrc = newsArticleRepository.countBySourceForConflict(conflictId);
        var eventsByType = conflictEventRepository.countByEventType(conflictId);
        var sentimentRaw = newsArticleRepository.countBySentimentForConflict(conflictId);
        var monthlyArticleRaw = newsArticleRepository.monthlyArticleCountForConflict(conflictId);

        var articlesBySource = new java.util.LinkedHashMap<String, Long>();
        for (Object[] row : articlesBySrc) {
            articlesBySource.put((String) row[0], (Long) row[1]);
        }

        var evtByType = new java.util.LinkedHashMap<String, Long>();
        for (Object[] row : eventsByType) {
            evtByType.put((String) row[0], (Long) row[1]);
        }

        var sentimentBreakdown = new java.util.LinkedHashMap<String, Long>();
        for (Object[] row : sentimentRaw) {
            sentimentBreakdown.put((String) row[0], (Long) row[1]);
        }

        // Use article monthly counts as the trend (articles as proxy for incidents)
        var monthly = monthlyArticleRaw.stream().map(row -> {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            return ConflictStatsDTO.MonthlyDataDTO.builder()
                    .month(String.format("%04d-%02d", yr, mo))
                    .incidents(((Number) row[2]).longValue())
                    .casualties(0L)
                    .build();
        }).toList();

        long totalArticles = newsArticleRepository.countByConflictId(conflictId);
        Double avgReliability = newsArticleRepository.averageReliabilityForConflict(conflictId);

        return ConflictStatsDTO.builder()
                .conflictId(conflictId)
                .totalArticles(totalArticles)
                .totalEvents(evtByType.values().stream().mapToLong(Long::longValue).sum())
                .articlesBySource(articlesBySource)
                .sentimentBreakdown(sentimentBreakdown)
                .eventsByType(evtByType)
                .monthlyTrend(monthly)
                .averageReliabilityScore(avgReliability != null ? avgReliability : 0.0)
                .build();
    }

    public List<ConflictMapDTO> search(String q, String region, String severityStr,
                                       String typeStr, String statusStr) {
        Severity severity = severityStr != null && !severityStr.isBlank()
                ? Severity.valueOf(severityStr.toUpperCase()) : null;
        ConflictType type = typeStr != null && !typeStr.isBlank()
                ? ConflictType.valueOf(typeStr.toUpperCase()) : null;
        ConflictStatus status = statusStr != null && !statusStr.isBlank()
                ? ConflictStatus.valueOf(statusStr.toUpperCase()) : null;

        return conflictRepository.search(
                        q != null && q.isBlank() ? null : q,
                        region != null && region.isBlank() ? null : region,
                        severity, type, status)
                .stream()
                .map(this::toMapDTO)
                .toList();
    }

    private ConflictMapDTO toMapDTO(Conflict c) {
        return ConflictMapDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .region(c.getRegion())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .severity(c.getSeverity())
                .conflictType(c.getConflictType())
                .status(c.getStatus())
                .startDate(c.getStartDate())
                .involvedParties(c.getInvolvedParties())
                .articleCount(newsArticleRepository.countByConflictId(c.getId()))
                .build();
    }

    private ConflictDetailDTO toDetailDTO(Conflict c) {
        long eventCount = conflictEventRepository.findByConflictIdOrderByEventDateDesc(c.getId()).size();
        if (eventCount == 0) {
            eventCount = Math.min(newsArticleRepository.countByConflictId(c.getId()), 50);
        }
        return ConflictDetailDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .region(c.getRegion())
                .countryCodes(c.getCountryCodes())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .severity(c.getSeverity())
                .conflictType(c.getConflictType())
                .status(c.getStatus())
                .startDate(c.getStartDate())
                .summary(c.getSummary())
                .casualtyEstimate(c.getCasualtyEstimate())
                .displacedEstimate(c.getDisplacedEstimate())
                .involvedParties(c.getInvolvedParties())
                .thumbnailUrl(c.getThumbnailUrl())
                .articleCount(newsArticleRepository.countByConflictId(c.getId()))
                .eventCount(eventCount)
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
