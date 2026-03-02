package com.conflictview.scheduler;

import com.conflictview.service.OsintAggregatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OsintRefreshScheduler {

    private final OsintAggregatorService osintAggregatorService;

    @Value("${app.osint.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${app.scheduler.osint-refresh-cron}")
    public void refreshOsint() {
        if (!enabled) {
            log.info("OSINT refresh disabled — skipping");
            return;
        }
        log.info("Scheduled OSINT refresh triggered");
        try {
            osintAggregatorService.refreshAllOsint();
        } catch (Exception e) {
            log.error("OSINT refresh scheduler error: {}", e.getMessage(), e);
        }
    }
}
