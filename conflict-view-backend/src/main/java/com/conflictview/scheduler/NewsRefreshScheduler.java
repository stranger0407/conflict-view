package com.conflictview.scheduler;

import com.conflictview.service.NewsAggregatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsRefreshScheduler {

    private final NewsAggregatorService newsAggregatorService;

    @Scheduled(cron = "${app.scheduler.news-refresh-cron}")
    public void refreshNews() {
        log.info("Scheduled news refresh triggered");
        try {
            newsAggregatorService.refreshAllNews();
        } catch (Exception e) {
            log.error("News refresh scheduler error: {}", e.getMessage(), e);
        }
    }
}
