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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcledOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    @Value("${app.acled.key:}")
    private String apiKey;

    @Value("${app.acled.email:}")
    private String email;

    @Value("${app.acled.enabled:true}")
    private boolean enabled;

    private static final String BASE_URL = "https://api.acleddata.com/acled/read";

    public void fetchForAllConflicts() {
        if (!enabled || apiKey == null || apiKey.isBlank() || email == null || email.isBlank()) {
            log.info("ACLED OSINT disabled or API key/email not configured — skipping");
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
                log.warn("ACLED fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    private void fetchForConflict(Conflict conflict) {
        String country = extractCountry(conflict);
        if (country == null) return;

        String sixMonthsAgo = LocalDate.now().minusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE);

        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("key", apiKey)
                .queryParam("email", email)
                .queryParam("country", country)
                .queryParam("event_date", sixMonthsAgo)
                .queryParam("event_date_where", ">=")
                .queryParam("limit", 100)
                .queryParam("fields", "event_id_cnty|event_date|event_type|sub_event_type|actor1|actor2|admin1|admin2|latitude|longitude|fatalities|source|notes")
                .build()
                .toUriString();

        try {
            AcledResponse response = restTemplate.getForObject(url, AcledResponse.class);
            if (response == null || response.getData() == null) return;

            int saved = 0;
            for (AcledEvent event : response.getData()) {
                String eventUrl = "https://acleddata.com/data-export-tool/#event=" + event.getEventIdCnty();
                if (osintResourceRepository.existsByUrl(eventUrl)) continue;

                String title = buildTitle(event);
                String actors = buildActors(event);
                String description = event.getNotes() != null ? event.getNotes() : "";

                Double lat = parseDouble(event.getLatitude());
                Double lon = parseDouble(event.getLongitude());
                Integer fatalities = parseInt(event.getFatalities());

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(ResourceType.EVENT_DATA)
                        .title(truncate(title, 500))
                        .url(eventUrl)
                        .description(truncate(description, 2000))
                        .sourcePlatform("ACLED")
                        .author(truncate(actors, 200))
                        .publishedAt(parseDate(event.getEventDate()))
                        .latitude(lat)
                        .longitude(lon)
                        .fatalities(fatalities)
                        .eventType(truncate(event.getEventType(), 100))
                        .build());
                saved++;
            }
            log.debug("ACLED: saved {} events for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("ACLED error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private String extractCountry(Conflict conflict) {
        // Try to extract country from region or name
        String region = conflict.getRegion();
        if (region != null && !region.isBlank()) {
            // Region might be "Middle East", "East Africa" etc. — use conflict name instead
            // ACLED expects country names like "Ukraine", "Syria", "Sudan"
            String name = conflict.getName();
            // Common patterns: "Ukraine War", "Syrian Civil War", "Sudan Crisis"
            // Take the first word if it looks like a country name
            String[] words = name.split("\\s+");
            if (words.length > 0) {
                String firstWord = words[0];
                // Handle adjectival forms
                if (firstWord.endsWith("ian")) return firstWord.replace("ian", "ia"); // Ukrainian -> Ukrainia? No...
                if (firstWord.endsWith("n") && firstWord.length() > 4) {
                    // "Syrian" -> "Syria"
                    String base = firstWord.substring(0, firstWord.length() - 1);
                    if (base.endsWith("ia") || base.endsWith("a")) return base;
                }
                return firstWord;
            }
        }
        return conflict.getName().split("\\s+")[0];
    }

    private String buildTitle(AcledEvent event) {
        StringBuilder sb = new StringBuilder();
        if (event.getEventType() != null) sb.append(event.getEventType());
        if (event.getSubEventType() != null) sb.append(": ").append(event.getSubEventType());
        if (event.getAdmin1() != null) sb.append(" in ").append(event.getAdmin1());
        if (event.getAdmin2() != null) sb.append(", ").append(event.getAdmin2());
        return sb.toString();
    }

    private String buildActors(AcledEvent event) {
        StringBuilder sb = new StringBuilder();
        if (event.getActor1() != null) sb.append(event.getActor1());
        if (event.getActor2() != null && !event.getActor2().isBlank()) {
            sb.append(" vs ").append(event.getActor2());
        }
        return sb.toString();
    }

    private LocalDateTime parseDate(String date) {
        if (date == null) return LocalDateTime.now();
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AcledResponse {
        private boolean success;
        private List<AcledEvent> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AcledEvent {
        @JsonProperty("event_id_cnty")
        private String eventIdCnty;
        @JsonProperty("event_date")
        private String eventDate;
        @JsonProperty("event_type")
        private String eventType;
        @JsonProperty("sub_event_type")
        private String subEventType;
        private String actor1;
        private String actor2;
        private String admin1;
        private String admin2;
        private String latitude;
        private String longitude;
        private String fatalities;
        private String source;
        private String notes;
    }
}
