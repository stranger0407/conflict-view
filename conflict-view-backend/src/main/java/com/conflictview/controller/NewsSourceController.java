package com.conflictview.controller;

import com.conflictview.dto.NewsSourceDTO;
import com.conflictview.model.NewsSource;
import com.conflictview.repository.NewsSourceRepository;
import com.conflictview.service.ReliabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@Tag(name = "Sources", description = "News source reliability information")
public class NewsSourceController {

    private final NewsSourceRepository newsSourceRepository;
    private final ReliabilityService reliabilityService;

    @GetMapping
    @Cacheable("newsSources")
    @Operation(summary = "Get all news sources with reliability scores")
    public ResponseEntity<List<NewsSourceDTO>> getAllSources() {
        List<NewsSourceDTO> sources = newsSourceRepository.findAll().stream()
                .sorted(Comparator.comparing(NewsSource::getReliabilityScore).reversed())
                .map(s -> NewsSourceDTO.builder()
                        .id(s.getId())
                        .domain(s.getDomain())
                        .name(s.getName())
                        .reliabilityScore(s.getReliabilityScore())
                        .reliabilityLabel(reliabilityService.getLabel(s.getReliabilityScore()))
                        .reliabilityColor(reliabilityService.getColor(s.getReliabilityScore()))
                        .biasRating(s.getBiasRating())
                        .category(s.getCategory())
                        .country(s.getCountry())
                        .logoUrl(s.getLogoUrl())
                        .build())
                .toList();
        return ResponseEntity.ok(sources);
    }
}
