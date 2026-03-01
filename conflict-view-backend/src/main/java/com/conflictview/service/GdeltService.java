package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.ConflictEvent;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.repository.ConflictEventRepository;
import com.conflictview.repository.ConflictRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdeltService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final ConflictEventRepository conflictEventRepository;

    @Value("${app.gdelt.base-url}")
    private String gdeltBaseUrl;

    @Value("${app.gdelt.enabled:true}")
    private boolean enabled;

    // GDELT event code to description mapping (simplified)
    private static final Map<String, String> GDELT_CAMEO_MAP = Map.ofEntries(
            Map.entry("14", "Protest"),
            Map.entry("145", "Protest violently"),
            Map.entry("19", "Use unconventional mass violence"),
            Map.entry("190", "Use unconventional mass violence"),
            Map.entry("193", "Carry out suicide bombing"),
            Map.entry("194", "Carry out vehicle bombing"),
            Map.entry("195", "Attempt to assassinate"),
            Map.entry("196", "Assassinate"),
            Map.entry("20", "Use conventional mass violence"),
            Map.entry("200", "Carry out attack"),
            Map.entry("201", "Engage in armed battle"),
            Map.entry("202", "Conduct airstrike"),
            Map.entry("203", "Conduct missile strike"),
            Map.entry("204", "Conduct mortar attack"),
            Map.entry("12", "Appeal for peace"),
            Map.entry("042", "Make visit"),
            Map.entry("036", "Express intent to cooperate")
    );

    public void syncConflictEvents() {
        if (!enabled) {
            log.info("GDELT sync disabled — skipping");
            return;
        }

        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                syncForConflict(conflict);
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("GDELT sync failed for {}: {}", conflict.getName(), e.getMessage());
            }
        }
    }

    private void syncForConflict(Conflict conflict) {
        // Query GDELT v2 geo API for events near conflict centroid
        String url = UriComponentsBuilder.fromHttpUrl(gdeltBaseUrl + "/geo/json")
                .queryParam("query", conflict.getName())
                .queryParam("mode", "PointData")
                .queryParam("maxrecords", 10)
                .queryParam("format", "json")
                .build()
                .toUriString();

        try {
            GdeltGeoResponse response = restTemplate.getForObject(url, GdeltGeoResponse.class);
            if (response == null || response.getFeatures() == null) return;

            for (GdeltGeoResponse.Feature feature : response.getFeatures()) {
                if (feature.getProperties() == null) continue;
                var props = feature.getProperties();
                if (props.getUrl() == null) continue;

                // Don't re-import events we already have
                LocalDate eventDate = parseGdeltDate(props.getDateadded());
                String eventType = resolveEventType(props.getEventcode());

                conflictEventRepository.save(ConflictEvent.builder()
                        .conflict(conflict)
                        .eventDate(eventDate)
                        .eventType(eventType)
                        .description(props.getName())
                        .sourceUrl(props.getUrl())
                        .latitude(feature.getGeometry() != null
                                ? getCoord(feature.getGeometry().getCoordinates(), 1) : null)
                        .longitude(feature.getGeometry() != null
                                ? getCoord(feature.getGeometry().getCoordinates(), 0) : null)
                        .build());
            }
            log.debug("GDELT: synced events for '{}'", conflict.getName());
        } catch (Exception e) {
            log.warn("GDELT API error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private LocalDate parseGdeltDate(String dateStr) {
        if (dateStr == null) return LocalDate.now();
        try {
            // GDELT format: 20240115120000
            return LocalDate.parse(dateStr.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String resolveEventType(String cameoCode) {
        if (cameoCode == null) return "Event";
        return GDELT_CAMEO_MAP.getOrDefault(cameoCode,
                GDELT_CAMEO_MAP.getOrDefault(cameoCode.substring(0, Math.min(2, cameoCode.length())), "Event"));
    }

    private Double getCoord(List<Double> coords, int idx) {
        if (coords == null || coords.size() <= idx) return null;
        return coords.get(idx);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GdeltGeoResponse {
        private List<Feature> features;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Feature {
            private Geometry geometry;
            private Properties properties;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Geometry {
                private List<Double> coordinates;
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Properties {
                private String name;
                private String url;
                private String eventcode;
                private String dateadded;
                private Integer nummentions;
            }
        }
    }
}
