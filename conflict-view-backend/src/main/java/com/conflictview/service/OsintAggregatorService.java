package com.conflictview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OsintAggregatorService {

    private final YouTubeOsintService youTubeOsintService;
    private final ReliefWebOsintService reliefWebOsintService;
    private final WikimediaOsintService wikimediaOsintService;
    private final InternetArchiveOsintService internetArchiveOsintService;
    private final GdeltImageOsintService gdeltImageOsintService;
    private final GdeltGeoOsintService gdeltGeoOsintService;
    private final AcledOsintService acledOsintService;
    private final NasaFirmsOsintService nasaFirmsOsintService;

    @CacheEvict(value = {"osintSummary"}, allEntries = true)
    public void refreshAllOsint() {
        log.info("Starting OSINT aggregation cycle...");
        long start = System.currentTimeMillis();

        try {
            youTubeOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("YouTube OSINT fetch failed: {}", e.getMessage());
        }

        try {
            reliefWebOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("ReliefWeb OSINT fetch failed: {}", e.getMessage());
        }

        try {
            wikimediaOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("Wikimedia OSINT fetch failed: {}", e.getMessage());
        }

        try {
            internetArchiveOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("Internet Archive OSINT fetch failed: {}", e.getMessage());
        }

        try {
            gdeltImageOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("GDELT Image OSINT fetch failed: {}", e.getMessage());
        }

        try {
            gdeltGeoOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("GDELT Geo OSINT fetch failed: {}", e.getMessage());
        }

        try {
            acledOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("ACLED OSINT fetch failed: {}", e.getMessage());
        }

        try {
            nasaFirmsOsintService.fetchForAllConflicts();
        } catch (Exception e) {
            log.error("NASA FIRMS OSINT fetch failed: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("OSINT aggregation cycle completed in {}ms", elapsed);
    }
}
