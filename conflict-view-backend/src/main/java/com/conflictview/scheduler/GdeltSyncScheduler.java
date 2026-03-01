package com.conflictview.scheduler;

import com.conflictview.service.GdeltService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GdeltSyncScheduler {

    private final GdeltService gdeltService;

    @Scheduled(cron = "${app.scheduler.gdelt-sync-cron}")
    public void syncGdelt() {
        log.info("Scheduled GDELT sync triggered");
        try {
            gdeltService.syncConflictEvents();
        } catch (Exception e) {
            log.error("GDELT sync scheduler error: {}", e.getMessage(), e);
        }
    }
}
