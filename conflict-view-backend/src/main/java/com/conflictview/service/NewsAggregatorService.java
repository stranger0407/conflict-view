package com.conflictview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsAggregatorService {

    private final NewsApiService newsApiService;
    private final RssFeedService rssFeedService;

    @CacheEvict(value = {"conflicts", "conflictDetail", "dashboardStats"}, allEntries = true)
    public void refreshAllNews() {
        log.info("Starting news aggregation cycle...");
        long start = System.currentTimeMillis();

        rssFeedService.fetchAllFeeds();
        newsApiService.fetchForAllConflicts();

        long elapsed = System.currentTimeMillis() - start;
        log.info("News aggregation cycle completed in {}ms", elapsed);
    }
}
