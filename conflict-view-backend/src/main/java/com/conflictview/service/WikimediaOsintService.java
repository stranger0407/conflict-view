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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikimediaOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    private static final String BASE_URL = "https://commons.wikimedia.org/w/api.php";

    public void fetchForAllConflicts() {
        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                fetchForConflict(conflict);
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Wikimedia fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchForConflict(Conflict conflict) {
        String query = buildQuery(conflict);

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("action", "query")
                .queryParam("generator", "search")
                .queryParam("gsrsearch", query)
                .queryParam("gsrnamespace", "6")
                .queryParam("gsrlimit", "30")
                .queryParam("prop", "imageinfo")
                .queryParam("iiprop", "url|extmetadata|size|mime")
                .queryParam("iiurlwidth", "400")
                .queryParam("format", "json")
                .build()
                .toUriString();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return;

            Map<String, Object> queryResult = (Map<String, Object>) response.get("query");
            if (queryResult == null) return;

            Map<String, Map<String, Object>> pages = (Map<String, Map<String, Object>>) queryResult.get("pages");
            if (pages == null) return;

            int saved = 0;
            for (Map<String, Object> page : pages.values()) {
                List<Map<String, Object>> imageInfo = (List<Map<String, Object>>) page.get("imageinfo");
                if (imageInfo == null || imageInfo.isEmpty()) continue;

                Map<String, Object> info = imageInfo.get(0);
                String mime = (String) info.get("mime");
                if (mime == null || (!mime.contains("jpeg") && !mime.contains("png"))) continue;

                Number width = (Number) info.get("width");
                if (width != null && width.intValue() < 200) continue;

                String title = (String) page.get("title");
                if (title != null) title = title.replaceFirst("^File:", "").replaceAll("\\.[^.]+$", "").replace("_", " ");

                String fullUrl = (String) info.get("descriptionurl");
                if (fullUrl == null) fullUrl = (String) info.get("url");
                if (fullUrl == null || osintResourceRepository.existsByUrl(fullUrl)) continue;

                String thumbUrl = (String) info.get("thumburl");
                if (thumbUrl == null) thumbUrl = (String) info.get("url");

                String description = extractDescription(info);

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(ResourceType.IMAGE)
                        .title(truncate(title, 500))
                        .url(fullUrl)
                        .thumbnailUrl(thumbUrl)
                        .description(truncate(description, 1000))
                        .sourcePlatform("Wikimedia")
                        .author(extractAuthor(info))
                        .build());
                saved++;
            }
            log.debug("Wikimedia: saved {} images for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("Wikimedia error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String buildQuery(Conflict conflict) {
        String[] words = conflict.getName().split("\\s+");
        return String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(3, words.length)));
    }

    @SuppressWarnings("unchecked")
    private String extractDescription(Map<String, Object> info) {
        Map<String, Object> extmetadata = (Map<String, Object>) info.get("extmetadata");
        if (extmetadata == null) return null;
        Map<String, Object> desc = (Map<String, Object>) extmetadata.get("ImageDescription");
        if (desc == null) return null;
        String val = (String) desc.get("value");
        if (val == null) return null;
        return val.replaceAll("<[^>]+>", "").trim();
    }

    @SuppressWarnings("unchecked")
    private String extractAuthor(Map<String, Object> info) {
        Map<String, Object> extmetadata = (Map<String, Object>) info.get("extmetadata");
        if (extmetadata == null) return null;
        Map<String, Object> artist = (Map<String, Object>) extmetadata.get("Artist");
        if (artist == null) return null;
        String val = (String) artist.get("value");
        if (val == null) return null;
        return truncate(val.replaceAll("<[^>]+>", "").trim(), 200);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
