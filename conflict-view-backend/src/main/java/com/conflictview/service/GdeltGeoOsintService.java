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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdeltGeoOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://api.gdeltproject.org/api/v2/geo/geo";

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
                log.warn("GDELT Geo fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchForConflict(Conflict conflict) {
        String[] words = conflict.getName().split("\\s+");
        String name = String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
        // Replace dashes and special chars for GDELT compatibility
        name = name.replace("-", " ").replace("/", " ").replaceAll("\\s+", " ").trim();

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("query", "\"" + name + "\"")
                .queryParam("format", "GeoJSON")
                .queryParam("maxpoints", 40)
                .queryParam("timespan", "1month")
                .build()
                .toUriString();

        try {
            // Fetch as String to handle application/vnd.geo+json content type
            String responseStr = restTemplate.getForObject(url, String.class);
            if (responseStr == null || responseStr.isBlank()) return;

            // Skip non-JSON responses (error pages, rate limit text)
            responseStr = responseStr.trim();
            if (!responseStr.startsWith("{") && !responseStr.startsWith("[")) {
                log.debug("GDELT Geo returned non-JSON for '{}', skipping", conflict.getName());
                return;
            }

            Map<String, Object> geoJson = objectMapper.readValue(responseStr, Map.class);

            List<Map<String, Object>> features = (List<Map<String, Object>>) geoJson.get("features");
            if (features == null) return;

            int saved = 0;
            for (Map<String, Object> feature : features) {
                try {
                    Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                    Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                    if (properties == null || geometry == null) continue;

                    String locationName = (String) properties.get("name");
                    if (locationName == null || locationName.isBlank()) continue;

                    List<Number> coords = (List<Number>) geometry.get("coordinates");
                    if (coords == null || coords.size() < 2) continue;

                    double lon = coords.get(0).doubleValue();
                    double lat = coords.get(1).doubleValue();

                    Number countNum = (Number) properties.get("count");
                    int count = countNum != null ? countNum.intValue() : 0;

                    String shareImage = (String) properties.get("shareimage");
                    String html = (String) properties.get("html");

                    // Build a unique URL using conflict name + location + coordinates
                    String resourceUrl = String.format(
                            "https://api.gdeltproject.org/api/v2/geo/geo?query=%s&format=GeoJSON&maxpoints=10#%s_%.4f_%.4f",
                            name.replace(" ", "+"), locationName.replace(" ", "_"), lat, lon);

                    if (osintResourceRepository.existsByUrl(resourceUrl)) continue;

                    // Extract first article URL from html if available
                    String description = String.format("Location: %s — %d news mentions in the past month", locationName, count);
                    if (html != null) {
                        // Extract first article title from html
                        String firstArticle = extractFirstLink(html);
                        if (firstArticle != null) {
                            description += ". Recent: " + firstArticle;
                        }
                    }

                    osintResourceRepository.save(OsintResource.builder()
                            .conflict(conflict)
                            .resourceType(ResourceType.MAP)
                            .title(truncate("Conflict activity: " + locationName + " (" + count + " mentions)", 500))
                            .url(resourceUrl)
                            .thumbnailUrl(shareImage)
                            .description(truncate(description, 2000))
                            .sourcePlatform("GDELT Geo")
                            .author("GDELT Project")
                            .latitude(lat)
                            .longitude(lon)
                            .publishedAt(LocalDateTime.now())
                            .build());
                    saved++;
                } catch (Exception e) {
                    log.trace("Skipping GDELT geo feature: {}", e.getMessage());
                }
            }
            log.debug("GDELT Geo: saved {} map points for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("GDELT Geo error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String extractFirstLink(String html) {
        if (html == null) return null;
        // html contains <a href="...">Title</a> entries
        int startTag = html.indexOf(">");
        int endTag = html.indexOf("</a>");
        if (startTag >= 0 && endTag > startTag) {
            return html.substring(startTag + 1, Math.min(endTag, startTag + 200));
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
