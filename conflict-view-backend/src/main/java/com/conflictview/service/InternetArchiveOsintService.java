package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.OsintResource;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ResourceType;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.OsintResourceRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternetArchiveOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    private static final String BASE_URL = "https://archive.org/advancedsearch.php";

    public void fetchForAllConflicts() {
        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                fetchForConflict(conflict);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Internet Archive fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchForConflict(Conflict conflict) {
        String query = buildQuery(conflict) + " AND mediatype:(movies OR image)";

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("q", query)
                .queryParam("fl[]", "identifier")
                .queryParam("fl[]", "title")
                .queryParam("fl[]", "description")
                .queryParam("fl[]", "date")
                .queryParam("fl[]", "mediatype")
                .queryParam("rows", "30")
                .queryParam("output", "json")
                .queryParam("sort[]", "downloads desc")
                .build()
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return;

            Map<String, Object> responseBody = (Map<String, Object>) response.get("response");
            if (responseBody == null) return;

            List<Map<String, Object>> docs = (List<Map<String, Object>>) responseBody.get("docs");
            if (docs == null) return;

            int saved = 0;
            for (Map<String, Object> doc : docs) {
                String identifier = extractString(doc.get("identifier"));
                if (identifier == null) continue;

                String itemUrl = "https://archive.org/details/" + identifier;
                if (osintResourceRepository.existsByUrl(itemUrl)) continue;

                String mediatype = extractString(doc.get("mediatype"));
                ResourceType resourceType = "movies".equalsIgnoreCase(mediatype)
                        ? ResourceType.VIDEO : ResourceType.IMAGE;

                String title = extractString(doc.get("title"));
                if (title == null) title = identifier;

                String description = extractString(doc.get("description"));
                if (description != null) {
                    description = description.replaceAll("<[^>]+>", "").trim();
                }

                String thumbnailUrl = "https://archive.org/services/img/" + identifier;
                LocalDateTime publishedAt = parseDate(extractString(doc.get("date")));

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(resourceType)
                        .title(truncate(title, 500))
                        .url(itemUrl)
                        .thumbnailUrl(thumbnailUrl)
                        .description(truncate(description, 1000))
                        .sourcePlatform("InternetArchive")
                        .publishedAt(publishedAt)
                        .build());
                saved++;
            }
            log.debug("InternetArchive: saved {} resources for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("InternetArchive error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String buildQuery(Conflict conflict) {
        String[] words = conflict.getName().split("\\s+");
        return String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(3, words.length)));
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            if (dateStr.length() >= 10) {
                return LocalDate.parse(dateStr.substring(0, 10)).atStartOfDay();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractString(Object obj) {
        if (obj instanceof String) return (String) obj;
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty()) return String.valueOf(list.get(0));
        }
        return obj != null ? String.valueOf(obj) : null;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
