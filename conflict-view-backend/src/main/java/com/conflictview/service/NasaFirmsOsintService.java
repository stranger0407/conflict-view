package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.OsintResource;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ResourceType;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.OsintResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NasaFirmsOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;

    @Value("${app.firms.key:}")
    private String mapKey;

    @Value("${app.firms.enabled:true}")
    private boolean enabled;

    private static final String BASE_URL = "https://firms.modaps.eosdis.nasa.gov/api/area/csv";

    public void fetchForAllConflicts() {
        if (!enabled || mapKey == null || mapKey.isBlank()) {
            log.info("NASA FIRMS OSINT disabled or MAP_KEY not configured — skipping");
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
                log.warn("NASA FIRMS fetch failed for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    private void fetchForConflict(Conflict conflict) {
        if (conflict.getLatitude() == null || conflict.getLongitude() == null) return;

        double lat = conflict.getLatitude();
        double lon = conflict.getLongitude();
        double delta = 2.0; // ±2 degrees bounding box

        String bbox = String.format(Locale.US, "%.4f,%.4f,%.4f,%.4f",
                lon - delta, lat - delta, lon + delta, lat + delta);

        String url = String.format("%s/%s/VIIRS_SNPP_NRT/%s/5", BASE_URL, mapKey, bbox);

        try {
            String csv = restTemplate.getForObject(url, String.class);
            if (csv == null || csv.isBlank()) return;

            BufferedReader reader = new BufferedReader(new StringReader(csv));
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) return;

            // Parse header to find column indices
            String[] headers = headerLine.split(",");
            int latIdx = findIndex(headers, "latitude");
            int lonIdx = findIndex(headers, "longitude");
            int brightIdx = findIndex(headers, "bright_ti4");
            int confIdx = findIndex(headers, "confidence");
            int dateIdx = findIndex(headers, "acq_date");
            int timeIdx = findIndex(headers, "acq_time");
            int dayNightIdx = findIndex(headers, "daynight");

            int saved = 0;
            String line;
            while ((line = reader.readLine()) != null && saved < 50) {
                try {
                    String[] cols = line.split(",");
                    if (cols.length < Math.max(Math.max(latIdx, lonIdx), confIdx) + 1) continue;

                    double fireLat = Double.parseDouble(cols[latIdx]);
                    double fireLon = Double.parseDouble(cols[lonIdx]);
                    String confStr = confIdx >= 0 && confIdx < cols.length ? cols[confIdx] : "nominal";
                    String brightness = brightIdx >= 0 && brightIdx < cols.length ? cols[brightIdx] : "N/A";
                    String acqDate = dateIdx >= 0 && dateIdx < cols.length ? cols[dateIdx] : "";
                    String acqTime = timeIdx >= 0 && timeIdx < cols.length ? cols[timeIdx] : "";
                    String dayNight = dayNightIdx >= 0 && dayNightIdx < cols.length ? cols[dayNightIdx] : "";

                    int confidence = parseConfidence(confStr);
                    if (confidence < 50) continue; // Skip low-confidence detections

                    // Build unique URL
                    String resourceUrl = String.format(Locale.US,
                            "https://firms.modaps.eosdis.nasa.gov/map/#d:%s;l:fires_viirs_snpp;@%.4f,%.4f,10z",
                            acqDate, fireLon, fireLat);

                    if (osintResourceRepository.existsByUrl(resourceUrl)) continue;

                    String title = String.format("Thermal anomaly detected (%d%% confidence)", confidence);
                    String period = dayNight.equals("D") ? "daytime" : dayNight.equals("N") ? "nighttime" : "";
                    String description = String.format(Locale.US,
                            "Fire/heat signature at %.4f, %.4f on %s at %s (%s). Brightness temperature: %sK. Near %s.",
                            fireLat, fireLon, acqDate, acqTime, period, brightness, conflict.getName());

                    osintResourceRepository.save(OsintResource.builder()
                            .conflict(conflict)
                            .resourceType(ResourceType.SATELLITE)
                            .title(title)
                            .url(resourceUrl)
                            .description(description)
                            .sourcePlatform("NASA FIRMS")
                            .author("VIIRS/SNPP Satellite")
                            .publishedAt(parseDate(acqDate))
                            .latitude(fireLat)
                            .longitude(fireLon)
                            .confidence(confidence)
                            .build());
                    saved++;
                } catch (Exception e) {
                    log.trace("Skipping FIRMS row: {}", e.getMessage());
                }
            }
            log.debug("NASA FIRMS: saved {} hotspots for '{}'", saved, conflict.getName());
        } catch (Exception e) {
            log.warn("NASA FIRMS error for '{}': {}", conflict.getName(), e.getMessage());
        }
    }

    private int findIndex(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private int parseConfidence(String s) {
        if (s == null) return 0;
        // FIRMS confidence can be "nominal", "low", "high" or a numeric value
        switch (s.trim().toLowerCase()) {
            case "high": return 90;
            case "nominal": return 70;
            case "low": return 30;
            default:
                try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 50; }
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDateTime.now();
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
