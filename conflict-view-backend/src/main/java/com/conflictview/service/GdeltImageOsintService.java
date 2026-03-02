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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdeltImageOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    private static final String BASE_URL = "https://api.gdeltproject.org/api/v2/doc/doc";
    private static final DateTimeFormatter GDELT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public void fetchForAllConflicts() {
        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                fetchForConflict(conflict);
                Thread.sleep(500); // GDELT rate limiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("GDELT image fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    private void fetchForConflict(Conflict conflict) {
        // Query 1: Direct conflict name + keywords
        String nameQuery = buildNameQuery(conflict);
        fetchImages(conflict, nameQuery);

        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }

        // Query 2: Image-tag based search for conflict imagery
        String tagQuery = buildImageTagQuery(conflict);
        fetchImages(conflict, tagQuery);
    }

    private void fetchImages(Conflict conflict, String query) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("query", query)
                .queryParam("mode", "artlist")
                .queryParam("format", "json")
                .queryParam("maxrecords", 50)
                .queryParam("timespan", "3months")
                .build()
                .toUriString();

        try {
            GdeltArticle[] articles = restTemplate.getForObject(url, GdeltArticle[].class);
            if (articles == null) return;

            int saved = 0;
            for (GdeltArticle article : articles) {
                if (article.getSocialimage() == null || article.getSocialimage().isBlank()) continue;
                if (article.getUrl() == null) continue;

                // Use socialimage URL as unique key since same article URL can have different images
                String imageUrl = article.getSocialimage();
                if (osintResourceRepository.existsByUrl(imageUrl)) continue;

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(ResourceType.IMAGE)
                        .title(truncate(article.getTitle(), 500))
                        .url(imageUrl)
                        .thumbnailUrl(imageUrl)
                        .description("Source: " + article.getDomain())
                        .sourcePlatform("GDELT")
                        .author(article.getDomain())
                        .publishedAt(parseGdeltDate(article.getSeendate()))
                        .build());
                saved++;
                if (saved >= 30) break; // Cap per query
            }
            log.debug("GDELT Images: saved {} for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("GDELT image query error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String buildNameQuery(Conflict conflict) {
        String[] words = conflict.getName().split("\\s+");
        String name = String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
        String keywords = "";
        if (conflict.getKeywords() != null && !conflict.getKeywords().isBlank()) {
            String[] kws = conflict.getKeywords().split(",");
            keywords = " " + String.join(" ", java.util.Arrays.copyOfRange(kws, 0, Math.min(2, kws.length))).trim();
        }
        return "\"" + name + "\"" + keywords;
    }

    private String buildImageTagQuery(Conflict conflict) {
        String[] words = conflict.getName().split("\\s+");
        String name = String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(3, words.length)));
        return name + " (imagetag:\"military\" OR imagetag:\"explosion\" OR imagetag:\"protest\" OR imagetag:\"refugee\" OR imagetag:\"fire\")";
    }

    private LocalDateTime parseGdeltDate(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(dateStr, GDELT_DATE);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GdeltArticle {
        private String url;
        private String title;
        private String seendate;
        private String socialimage;
        private String domain;
        private String language;
        private String sourcecountry;
    }
}
