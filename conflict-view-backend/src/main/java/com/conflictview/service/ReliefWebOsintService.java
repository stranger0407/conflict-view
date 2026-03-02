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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReliefWebOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    @Value("${app.reliefweb.appname:}")
    private String appname;

    @Value("${app.reliefweb.enabled:true}")
    private boolean enabled;

    private static final String BASE_URL = "https://api.reliefweb.int/v1/reports";

    public void fetchForAllConflicts() {
        if (!enabled || appname == null || appname.isBlank()) {
            log.info("ReliefWeb OSINT disabled or appname not configured — skipping");
            return;
        }
        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                fetchForConflict(conflict);
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("ReliefWeb fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    private void fetchForConflict(Conflict conflict) {
        String searchTerm = extractCountryName(conflict);

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("appname", appname)
                .queryParam("query[value]", searchTerm)
                .queryParam("query[operator]", "AND")
                .queryParam("fields[include][]", "title")
                .queryParam("fields[include][]", "url")
                .queryParam("fields[include][]", "date.created")
                .queryParam("fields[include][]", "source")
                .queryParam("fields[include][]", "format")
                .queryParam("fields[include][]", "file")
                .queryParam("limit", 50)
                .queryParam("sort[]", "date.created:desc")
                .build()
                .toUriString();

        try {
            ReliefWebResponse response = restTemplate.getForObject(url, ReliefWebResponse.class);
            if (response == null || response.getData() == null) return;

            int saved = 0;
            for (ReliefWebResponse.ReportItem item : response.getData()) {
                var fields = item.getFields();
                if (fields == null || fields.getUrl() == null) continue;
                if (osintResourceRepository.existsByUrl(fields.getUrl())) continue;

                ResourceType type = classifyFormat(fields.getFormat());
                if (type == null) continue;

                String thumbnail = extractThumbnail(fields.getFile());
                String sourceName = extractSourceName(fields.getSource());

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(type)
                        .title(truncate(fields.getTitle(), 500))
                        .url(fields.getUrl())
                        .thumbnailUrl(thumbnail)
                        .description(null)
                        .sourcePlatform("ReliefWeb")
                        .author(truncate(sourceName, 200))
                        .publishedAt(parseDate(fields.getDate()))
                        .build());
                saved++;
            }
            log.debug("ReliefWeb: saved {} resources for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("ReliefWeb error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String extractCountryName(Conflict conflict) {
        // Use region or first part of conflict name for search
        String name = conflict.getName();
        if (name.contains("-")) {
            return name.split("-")[0].trim();
        }
        String[] words = name.split("\\s+");
        return String.join(" ", java.util.Arrays.copyOfRange(words, 0, Math.min(3, words.length)));
    }

    private ResourceType classifyFormat(List<ReliefWebResponse.NamedItem> formats) {
        if (formats == null || formats.isEmpty()) return ResourceType.REPORT;
        for (var fmt : formats) {
            String name = fmt.getName() != null ? fmt.getName().toLowerCase() : "";
            if (name.contains("map")) return ResourceType.MAP;
            if (name.contains("infographic")) return ResourceType.INFOGRAPHIC;
            if (name.contains("interactive")) return ResourceType.MAP;
        }
        return ResourceType.REPORT;
    }

    private String extractThumbnail(List<ReliefWebResponse.FileItem> files) {
        if (files == null || files.isEmpty()) return null;
        for (var file : files) {
            if (file.getPreview() != null && file.getPreview().getUrl() != null) {
                return file.getPreview().getUrl();
            }
            if (file.getUrl() != null && isImageUrl(file.getUrl())) {
                return file.getUrl();
            }
        }
        return null;
    }

    private boolean isImageUrl(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".webp") || lower.endsWith(".gif");
    }

    private String extractSourceName(List<ReliefWebResponse.NamedItem> sources) {
        if (sources == null || sources.isEmpty()) return "ReliefWeb";
        return sources.get(0).getName();
    }

    private LocalDateTime parseDate(ReliefWebResponse.DateField date) {
        if (date == null || date.getCreated() == null) return LocalDateTime.now();
        try {
            return OffsetDateTime.parse(date.getCreated()).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    // Response DTOs
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReliefWebResponse {
        private List<ReportItem> data;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ReportItem {
            private String id;
            private ReportFields fields;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ReportFields {
            private String title;
            private String url;
            @JsonProperty("date")
            private DateField date;
            private List<NamedItem> source;
            private List<NamedItem> format;
            private List<FileItem> file;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class DateField {
            private String created;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class NamedItem {
            private String name;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FileItem {
            private String url;
            private String mimetype;
            private FilePreview preview;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FilePreview {
            @JsonProperty("url-thumb")
            private String url;
        }
    }
}
