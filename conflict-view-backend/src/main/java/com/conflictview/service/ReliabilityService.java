package com.conflictview.service;

import com.conflictview.model.NewsSource;
import com.conflictview.model.enums.SourceCategory;
import com.conflictview.repository.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReliabilityService {

    private static final int DEFAULT_UNKNOWN_SCORE = 40;

    private final NewsSourceRepository newsSourceRepository;

    public int getScore(String url) {
        String domain = extractDomain(url);
        if (domain == null) return DEFAULT_UNKNOWN_SCORE;
        return getScoreForDomain(domain);
    }

    public int getScoreForDomain(String domain) {
        // Normalize: strip www. prefix
        String normalized = domain.toLowerCase().replaceFirst("^www\\.", "");

        return newsSourceRepository.findByDomain(normalized)
                .map(NewsSource::getReliabilityScore)
                .orElseGet(() -> {
                    // Try parent domain (e.g. news.bbc.co.uk → bbc.co.uk)
                    String parent = getParentDomain(normalized);
                    return newsSourceRepository.findByDomain(parent)
                            .map(NewsSource::getReliabilityScore)
                            .orElseGet(() -> saveUnknownAndReturn(normalized));
                });
    }

    public String getLabel(int score) {
        if (score >= 80) return "High";
        if (score >= 60) return "Medium";
        return "Low";
    }

    public String getColor(int score) {
        if (score >= 80) return "green";
        if (score >= 60) return "yellow";
        return "red";
    }

    private int saveUnknownAndReturn(String domain) {
        if (!newsSourceRepository.existsByDomain(domain)) {
            try {
                newsSourceRepository.save(NewsSource.builder()
                        .domain(domain)
                        .name(domain)
                        .reliabilityScore(DEFAULT_UNKNOWN_SCORE)
                        .category(SourceCategory.UNKNOWN)
                        .build());
            } catch (Exception e) {
                log.debug("Could not save unknown source {}: {}", domain, e.getMessage());
            }
        }
        return DEFAULT_UNKNOWN_SCORE;
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return null;
            return host.toLowerCase().replaceFirst("^www\\.", "");
        } catch (Exception e) {
            return null;
        }
    }

    private String getParentDomain(String domain) {
        String[] parts = domain.split("\\.");
        if (parts.length <= 2) return domain;
        // Return last 2 or 3 parts (handle co.uk etc.)
        if (parts[parts.length - 2].equals("co") || parts[parts.length - 2].equals("com")) {
            return parts[parts.length - 3] + "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }
}
