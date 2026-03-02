package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.OsintResource;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ResourceType;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.OsintResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdeltImageOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;
    private final ObjectMapper objectMapper;

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

    @SuppressWarnings("unchecked")
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
            // Fetch as String to handle any content type (JSON or HTML rate-limit page)
            String responseStr = restTemplate.getForObject(url, String.class);
            if (responseStr == null || responseStr.isBlank()) return;

            // Skip HTML responses (rate limit pages)
            if (responseStr.trim().startsWith("<")) {
                log.debug("GDELT returned HTML (rate limit), skipping for '{}'", conflict.getName());
                return;
            }

            Map<String, Object> response = objectMapper.readValue(responseStr, Map.class);
            List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
            if (articles == null) return;

            int saved = 0;
            for (Map<String, Object> article : articles) {
                String socialimage = (String) article.get("socialimage");
                String articleUrl = (String) article.get("url");
                if (socialimage == null || socialimage.isBlank()) continue;
                if (articleUrl == null) continue;

                // Use socialimage URL as unique key since same article URL can have different images
                if (osintResourceRepository.existsByUrl(socialimage)) continue;

                String title = (String) article.get("title");
                String domain = (String) article.get("domain");
                String seendate = (String) article.get("seendate");

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(ResourceType.IMAGE)
                        .title(truncate(title, 500))
                        .url(socialimage)
                        .thumbnailUrl(socialimage)
                        .description("Source: " + domain)
                        .sourcePlatform("GDELT")
                        .author(domain)
                        .publishedAt(parseGdeltDate(seendate))
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
}
