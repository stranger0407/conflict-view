package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.OsintResource;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ResourceType;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.OsintResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fetches intelligence reports from professional humanitarian/conflict RSS feeds.
 * Sources: ReliefWeb updates, OCHA reports, conflict analysis feeds.
 * Creates REPORT and INFOGRAPHIC type OSINT resources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrisisIntelOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    private static final String[] INTEL_FEEDS = {
            "https://reliefweb.int/updates/rss.xml",
            "https://news.un.org/feed/subscribe/en/news/topic/peace-and-security/feed/rss.xml",
            "https://www.msf.org/rss/all",
    };

    private static final DateTimeFormatter RSS_DATE = DateTimeFormatter.ofPattern(
            "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    private static final DateTimeFormatter RSS_DATE_ALT = DateTimeFormatter.ofPattern(
            "EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    public void fetchForAllConflicts() {
        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);

        for (String feedUrl : INTEL_FEEDS) {
            try {
                List<RssItem> items = parseFeed(feedUrl);
                matchAndSave(items, conflicts);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Crisis intel feed error for '{}': {}", feedUrl, e.getMessage());
            }
        }
    }

    private List<RssItem> parseFeed(String feedUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "ConflictView/1.0 (https://github.com/stranger0407/conflict-view)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(feedUrl, HttpMethod.GET, entity, String.class);
        String xml = response.getBody();
        if (xml == null || xml.isBlank()) return List.of();

        List<RssItem> items = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < Math.min(itemNodes.getLength(), 100); i++) {
                Element item = (Element) itemNodes.item(i);
                RssItem rssItem = new RssItem();
                rssItem.title = getElementText(item, "title");
                rssItem.link = getElementText(item, "link");
                rssItem.description = getElementText(item, "description");
                rssItem.pubDate = getElementText(item, "pubDate");
                if (feedUrl.contains("reliefweb")) {
                    rssItem.source = "ReliefWeb";
                } else if (feedUrl.contains("news.un.org")) {
                    rssItem.source = "UN News";
                } else if (feedUrl.contains("msf.org")) {
                    rssItem.source = "MSF";
                } else {
                    rssItem.source = "Crisis Intel";
                }

                // Extract media thumbnail if available
                NodeList mediaNodes = item.getElementsByTagName("media:content");
                if (mediaNodes.getLength() > 0) {
                    rssItem.thumbnailUrl = ((Element) mediaNodes.item(0)).getAttribute("url");
                }

                if (rssItem.title != null && rssItem.link != null) {
                    items.add(rssItem);
                }
            }
        } catch (Exception e) {
            log.warn("RSS parse error: {}", e.getMessage());
        }
        return items;
    }

    private void matchAndSave(List<RssItem> items, List<Conflict> conflicts) {
        for (RssItem item : items) {
            if (osintResourceRepository.existsByUrl(item.link)) continue;

            String titleLower = (item.title + " " + (item.description != null ? item.description : "")).toLowerCase();

            for (Conflict conflict : conflicts) {
                if (matchesConflict(titleLower, conflict)) {
                    // Determine resource type
                    ResourceType type = ResourceType.REPORT;
                    if (titleLower.contains("map") || titleLower.contains("infographic")
                            || titleLower.contains("dashboard") || titleLower.contains("snapshot")) {
                        type = ResourceType.INFOGRAPHIC;
                    }

                    String cleanDesc = item.description;
                    if (cleanDesc != null) {
                        cleanDesc = cleanDesc.replaceAll("<[^>]+>", "").trim();
                        if (cleanDesc.length() > 500) cleanDesc = cleanDesc.substring(0, 500);
                    }

                    osintResourceRepository.save(OsintResource.builder()
                            .conflict(conflict)
                            .resourceType(type)
                            .title("[OSINT] " + item.title)
                            .url(item.link)
                            .thumbnailUrl(item.thumbnailUrl)
                            .description(cleanDesc)
                            .sourcePlatform(item.source)
                            .author(item.source + " Intelligence")
                            .publishedAt(parseDate(item.pubDate))
                            .build());
                    break; // Only match to first conflict
                }
            }
        }
    }

    private boolean matchesConflict(String text, Conflict conflict) {
        // Match by country name, conflict name fragments, or keywords
        String name = conflict.getName().toLowerCase();

        // Extract key terms from conflict name
        String[] nameFragments = name.split("[\\s\\-/&]+");
        int matches = 0;
        for (String fragment : nameFragments) {
            if (fragment.length() >= 3 && text.contains(fragment)) {
                matches++;
            }
        }
        if (matches >= 2) return true;

        // Match by keywords
        if (conflict.getKeywords() != null) {
            String[] keywords = conflict.getKeywords().split(",");
            for (String kw : keywords) {
                String trimmed = kw.trim().toLowerCase();
                if (trimmed.length() >= 3 && text.contains(trimmed)) {
                    return true;
                }
            }
        }

        // Match by country codes
        if (conflict.getCountryCodes() != null) {
            String[] codes = conflict.getCountryCodes().split(",");
            for (String code : codes) {
                String country = codeToCountry(code.trim());
                if (country != null && text.contains(country.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String codeToCountry(String code) {
        return switch (code.toUpperCase()) {
            case "UA" -> "Ukraine";
            case "RU" -> "Russia";
            case "IL" -> "Israel";
            case "PS" -> "Palestine";
            case "SY" -> "Syria";
            case "SD" -> "Sudan";
            case "YE" -> "Yemen";
            case "IQ" -> "Iraq";
            case "AF" -> "Afghanistan";
            case "CD" -> "Congo";
            case "ML", "BF", "NE" -> "Sahel";
            case "SO" -> "Somalia";
            case "MM" -> "Myanmar";
            case "ET" -> "Ethiopia";
            case "HT" -> "Haiti";
            case "MX" -> "Mexico";
            case "PK" -> "Pakistan";
            case "CO" -> "Colombia";
            case "MZ" -> "Mozambique";
            case "PH" -> "Philippines";
            case "LB" -> "Lebanon";
            case "IR" -> "Iran";
            default -> null;
        };
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        try {
            return ZonedDateTime.parse(dateStr, RSS_DATE).toLocalDateTime();
        } catch (Exception e1) {
            try {
                return ZonedDateTime.parse(dateStr, RSS_DATE_ALT).toLocalDateTime();
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0).getTextContent() != null) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private static class RssItem {
        String title;
        String link;
        String description;
        String pubDate;
        String source;
        String thumbnailUrl;
    }
}
