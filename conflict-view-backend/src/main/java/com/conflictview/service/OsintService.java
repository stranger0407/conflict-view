package com.conflictview.service;

import com.conflictview.dto.OsintResourceDTO;
import com.conflictview.dto.OsintSummaryDTO;
import com.conflictview.model.enums.ResourceType;
import com.conflictview.repository.OsintResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OsintService {

    private final OsintResourceRepository osintResourceRepository;

    public Page<OsintResourceDTO> getOsintResources(UUID conflictId, String typeStr, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));

        if (typeStr != null && !typeStr.isBlank()) {
            ResourceType type = ResourceType.valueOf(typeStr.toUpperCase());
            return osintResourceRepository.findByConflictIdAndResourceTypeOrderByPublishedAtDesc(
                    conflictId, type, pageable).map(this::toDTO);
        }

        return osintResourceRepository.findByConflictIdOrderByPublishedAtDesc(conflictId, pageable)
                .map(this::toDTO);
    }

    @Cacheable(value = "osintSummary", key = "#conflictId")
    public OsintSummaryDTO getOsintSummary(UUID conflictId) {
        var rows = osintResourceRepository.countByConflictIdGroupByResourceType(conflictId);

        long videoCount = 0, imageCount = 0, mapCount = 0, infographicCount = 0, reportCount = 0,
                satelliteCount = 0, eventDataCount = 0;
        for (Object[] row : rows) {
            String type = (String) row[0];
            long count = (Long) row[1];
            switch (type) {
                case "VIDEO" -> videoCount = count;
                case "IMAGE" -> imageCount = count;
                case "MAP" -> mapCount = count;
                case "INFOGRAPHIC" -> infographicCount = count;
                case "REPORT" -> reportCount = count;
                case "SATELLITE" -> satelliteCount = count;
                case "EVENT_DATA" -> eventDataCount = count;
            }
        }

        return OsintSummaryDTO.builder()
                .videoCount(videoCount)
                .imageCount(imageCount)
                .mapCount(mapCount)
                .infographicCount(infographicCount)
                .reportCount(reportCount)
                .satelliteCount(satelliteCount)
                .eventDataCount(eventDataCount)
                .totalCount(videoCount + imageCount + mapCount + infographicCount + reportCount + satelliteCount + eventDataCount)
                .build();
    }

    private OsintResourceDTO toDTO(com.conflictview.model.OsintResource o) {
        return OsintResourceDTO.builder()
                .id(o.getId())
                .conflictId(o.getConflict().getId())
                .resourceType(o.getResourceType())
                .title(o.getTitle())
                .url(o.getUrl())
                .thumbnailUrl(o.getThumbnailUrl())
                .description(o.getDescription())
                .sourcePlatform(o.getSourcePlatform())
                .author(o.getAuthor())
                .publishedAt(o.getPublishedAt())
                .duration(o.getDuration())
                .latitude(o.getLatitude())
                .longitude(o.getLongitude())
                .fatalities(o.getFatalities())
                .eventType(o.getEventType())
                .confidence(o.getConfidence())
                .build();
    }
}
