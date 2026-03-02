package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.OsintResource;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ResourceType;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.OsintResourceRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    @Value("${app.youtube.key:}")
    private String apiKey;

    @Value("${app.youtube.enabled:true}")
    private boolean enabled;

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/search";

    public void fetchForAllConflicts() {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.info("YouTube OSINT disabled or API key not configured — skipping");
            return;
        }

        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                fetchForConflict(conflict);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("YouTube fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    private void fetchForConflict(Conflict conflict) {
        String query = buildQuery(conflict);
        String publishedAfter = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("maxResults", 25)
                .queryParam("order", "date")
                .queryParam("relevanceLanguage", "en")
                .queryParam("safeSearch", "moderate")
                .queryParam("videoDuration", "medium")
                .queryParam("publishedAfter", publishedAfter)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        try {
            YouTubeResponse response = restTemplate.getForObject(url, YouTubeResponse.class);
            if (response == null || response.getItems() == null) return;

            int saved = 0;
            for (YouTubeResponse.Item item : response.getItems()) {
                var snippet = item.getSnippet();
                var videoId = item.getId() != null ? item.getId().getVideoId() : null;
                if (snippet == null || videoId == null) continue;

                // Skip 24/7 live streams and low-quality content
                String title = snippet.getTitle();
                if (title != null) {
                    String lower = title.toLowerCase();
                    if (lower.contains("24/7") || lower.contains("live stream") || lower.contains("livestream")
                            || lower.contains("lofi") || lower.contains("asmr")) continue;
                }

                String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
                if (osintResourceRepository.existsByUrl(videoUrl)) continue;

                String thumbUrl = null;
                if (snippet.getThumbnails() != null && snippet.getThumbnails().getMedium() != null) {
                    thumbUrl = snippet.getThumbnails().getMedium().getUrl();
                }

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(ResourceType.VIDEO)
                        .title(truncate(snippet.getTitle(), 500))
                        .url(videoUrl)
                        .thumbnailUrl(thumbUrl)
                        .description(truncate(snippet.getDescription(), 1000))
                        .sourcePlatform("YouTube")
                        .author(truncate(snippet.getChannelTitle(), 200))
                        .publishedAt(parseDate(snippet.getPublishedAt()))
                        .build());
                saved++;
            }
            log.debug("YouTube: saved {} videos for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("YouTube error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String buildQuery(Conflict conflict) {
        String[] words = conflict.getName().split("\\s+");
        String name = String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(4, words.length)));

        // Use conflict keywords for more targeted results
        StringBuilder query = new StringBuilder(name);
        if (conflict.getKeywords() != null && !conflict.getKeywords().isBlank()) {
            String[] kws = conflict.getKeywords().split(",");
            for (int i = 0; i < Math.min(2, kws.length); i++) {
                String kw = kws[i].trim();
                if (!kw.isEmpty()) query.append(" ").append(kw);
            }
        } else {
            query.append(" conflict news");
        }
        return query.toString();
    }

    private LocalDateTime parseDate(String iso) {
        if (iso == null) return LocalDateTime.now();
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
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
    public static class YouTubeResponse {
        private List<Item> items;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Item {
            private VideoId id;
            private Snippet snippet;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class VideoId {
            private String videoId;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Snippet {
            private String title;
            private String description;
            private String publishedAt;
            private String channelTitle;
            private Thumbnails thumbnails;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Thumbnails {
            @JsonProperty("medium")
            private Thumbnail medium;
            @JsonProperty("high")
            private Thumbnail high;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Thumbnail {
            private String url;
            private int width;
            private int height;
        }
    }
}
