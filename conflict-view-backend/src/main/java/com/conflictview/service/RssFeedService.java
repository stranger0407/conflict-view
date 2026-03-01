package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.NewsArticle;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.NewsArticleRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssFeedService {

    private final ConflictRepository conflictRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final ReliabilityService reliabilityService;
    private final SentimentService sentimentService;
    private final RssFeedProperties feedProperties;

    public void fetchAllFeeds() {
        if (!feedProperties.isEnabled()) {
            log.info("RSS feeds disabled — skipping");
            return;
        }

        Map<String, String> feeds = feedProperties.getFeeds();
        if (feeds == null || feeds.isEmpty()) {
            log.warn("No RSS feeds configured");
            return;
        }

        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        // Also include monitoring conflicts so they get articles
        conflicts.addAll(conflictRepository.findByStatus(ConflictStatus.MONITORING));

        // Pre-parse keyword sets for each conflict
        Map<Conflict, Set<String>> conflictKeywordMap = new HashMap<>();
        for (Conflict c : conflicts) {
            Set<String> kw = new HashSet<>();
            // Keywords column — primary matching source
            if (c.getKeywords() != null && !c.getKeywords().isBlank()) {
                Arrays.stream(c.getKeywords().split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(s -> !s.isEmpty())
                        .forEach(kw::add);
            }
            // Also add name words >3 chars as fallback
            Arrays.stream(c.getName().toLowerCase().split("[\\s\\-/]+"))
                    .filter(w -> w.length() > 3)
                    .forEach(kw::add);
            conflictKeywordMap.put(c, kw);
        }

        int totalNew = 0;
        for (Map.Entry<String, String> feed : feeds.entrySet()) {
            try {
                int count = fetchFeed(feed.getKey(), feed.getValue(), conflicts, conflictKeywordMap);
                totalNew += count;
            } catch (Exception e) {
                log.warn("RSS fetch failed for {}: {}", feed.getKey(), e.getMessage());
            }
        }
        log.info("RSS: fetched {} new articles from {} feeds", totalNew, feeds.size());
    }

    private int fetchFeed(String feedKey, String feedUrl, List<Conflict> conflicts,
                          Map<Conflict, Set<String>> conflictKeywordMap) {
        int newArticles = 0;
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(feedUrl).toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "ConflictView/1.0 (News Aggregator)");
            conn.setInstanceFollowRedirects(true);

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(conn));
            String feedTitle = feed.getTitle();
            String domain = extractDomain(feedUrl);
            int reliability = reliabilityService.getScoreForDomain(domain);

            for (SyndEntry entry : feed.getEntries()) {
                String title = entry.getTitle();
                String entryUrl = entry.getLink();
                if (title == null || entryUrl == null || newsArticleRepository.existsByUrl(entryUrl)) continue;

                String description = extractDescription(entry);
                String combined = (title + " " + (description != null ? description : "")).toLowerCase();

                // Match to best conflict using weighted keyword scoring
                Conflict matchedConflict = findBestMatchingConflict(combined, conflicts, conflictKeywordMap);
                if (matchedConflict == null) continue;

                var sentiment = sentimentService.analyze(title, description);
                LocalDateTime published = entry.getPublishedDate() != null
                        ? entry.getPublishedDate().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime()
                        : LocalDateTime.now();

                String imageUrl = extractImageUrl(entry);

                newsArticleRepository.save(NewsArticle.builder()
                        .conflict(matchedConflict)
                        .title(truncate(title, 500))
                        .url(entryUrl)
                        .sourceName(feedTitle)
                        .sourceDomain(domain)
                        .description(truncate(description, 2000))
                        .publishedAt(published)
                        .reliabilityScore(reliability)
                        .sentiment(sentiment)
                        .imageUrl(truncate(imageUrl, 1000))
                        .build());
                newArticles++;
            }
            if (newArticles > 0) {
                log.info("RSS: {} new articles from {} ({})", newArticles, feedKey, domain);
            }
        } catch (Exception e) {
            log.warn("Error parsing RSS from {}: {}", feedKey, e.getMessage());
        }
        return newArticles;
    }

    /**
     * Matches article text to the best conflict using weighted keyword scoring.
     * Longer/more specific keywords score higher. Returns null if no confident match.
     */
    private Conflict findBestMatchingConflict(String text, List<Conflict> conflicts,
                                               Map<Conflict, Set<String>> conflictKeywordMap) {
        Conflict bestMatch = null;
        int bestScore = 0;

        for (Conflict c : conflicts) {
            Set<String> keywords = conflictKeywordMap.get(c);
            if (keywords == null || keywords.isEmpty()) continue;

            int score = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    // Multi-word keywords are highly specific — worth more
                    if (keyword.contains(" ")) {
                        score += 5;
                    } else if (keyword.length() >= 8) {
                        score += 3;
                    } else if (keyword.length() >= 5) {
                        score += 2;
                    } else {
                        score += 1;
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = c;
            }
        }

        // Require minimum score of 3 to avoid false positives
        return bestScore >= 3 ? bestMatch : null;
    }

    private String extractDomain(String feedUrl) {
        try {
            String host = URI.create(feedUrl).getHost();
            if (host == null) return "unknown";
            host = host.replaceAll("^(www\\.|feeds\\.|rss\\.|rssfeeds\\.)", "");
            return host;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractDescription(SyndEntry entry) {
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue()
                    .replaceAll("<[^>]+>", "").trim();
        }
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            return entry.getContents().get(0).getValue()
                    .replaceAll("<[^>]+>", "").trim();
        }
        return null;
    }

    private String extractImageUrl(SyndEntry entry) {
        if (entry.getEnclosures() != null) {
            for (var enc : entry.getEnclosures()) {
                if (enc.getType() != null && enc.getType().startsWith("image/")) {
                    return enc.getUrl();
                }
            }
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    @ConfigurationProperties(prefix = "app.rss")
    @Component
    @Data
    public static class RssFeedProperties {
        private boolean enabled = true;
        private Map<String, String> feeds = new LinkedHashMap<>();
    }
}
