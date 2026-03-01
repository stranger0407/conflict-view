package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.NewsArticle;
import com.conflictview.repository.NewsArticleRepository;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.model.enums.ConflictStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsApiService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final ReliabilityService reliabilityService;
    private final SentimentService sentimentService;

    @Value("${app.news-api.key:}")
    private String apiKey;

    @Value("${app.news-api.base-url}")
    private String baseUrl;

    @Value("${app.news-api.enabled:true}")
    private boolean enabled;

    public void fetchForAllConflicts() {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.info("NewsAPI disabled or API key not configured — skipping");
            return;
        }

        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                fetchForConflict(conflict);
                Thread.sleep(200); // polite rate limiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("NewsAPI fetch failed for conflict {}: {}", conflict.getName(), e.getMessage());
            }
        }
    }

    private void fetchForConflict(Conflict conflict) {
        String query = buildQuery(conflict.getName());
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/everything")
                .queryParam("q", query)
                .queryParam("language", "en")
                .queryParam("sortBy", "publishedAt")
                .queryParam("pageSize", 20)
                .queryParam("apiKey", apiKey)
                .build()
                .toUriString();

        try {
            NewsApiResponse response = restTemplate.getForObject(url, NewsApiResponse.class);
            if (response == null || response.getArticles() == null) return;

            for (NewsApiResponse.Article a : response.getArticles()) {
                if (a.getUrl() == null || a.getTitle() == null
                        || a.getTitle().equals("[Removed]")
                        || newsArticleRepository.existsByUrl(a.getUrl())) {
                    continue;
                }

                String domain = extractDomain(a.getUrl());
                int reliability = reliabilityService.getScoreForDomain(domain);
                var sentiment = sentimentService.analyze(a.getTitle(), a.getDescription());
                LocalDateTime published = parseDate(a.getPublishedAt());

                newsArticleRepository.save(NewsArticle.builder()
                        .conflict(conflict)
                        .title(truncate(a.getTitle(), 500))
                        .url(a.getUrl())
                        .sourceName(a.getSource() != null ? a.getSource().getName() : null)
                        .sourceDomain(domain)
                        .author(truncate(a.getAuthor(), 200))
                        .description(a.getDescription())
                        .publishedAt(published)
                        .reliabilityScore(reliability)
                        .sentiment(sentiment)
                        .imageUrl(a.getUrlToImage())
                        .build());
            }
            log.debug("NewsAPI: saved articles for '{}'", conflict.getName());
        } catch (Exception e) {
            log.warn("NewsAPI error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String buildQuery(String conflictName) {
        // Take first 3 words to avoid query being too long
        String[] words = conflictName.split("\\s+");
        return String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(3, words.length)));
    }

    private String extractDomain(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host == null) return "unknown";
            return host.toLowerCase().replaceFirst("^www\\.", "");
        } catch (Exception e) {
            return "unknown";
        }
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

    // DTO inner classes for JSON deserialization
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsApiResponse {
        private String status;
        private int totalResults;
        private List<Article> articles;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Article {
            private Source source;
            private String author;
            private String title;
            private String description;
            private String url;
            private String urlToImage;
            private String publishedAt;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Source {
                private String id;
                private String name;
            }
        }
    }
}
