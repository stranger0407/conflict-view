package com.conflictview.dto;

import com.conflictview.model.enums.SentimentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleDTO {
    private UUID id;
    private UUID conflictId;
    private String title;
    private String url;
    private String sourceName;
    private String sourceDomain;
    private String author;
    private String description;
    private LocalDateTime publishedAt;
    private Integer reliabilityScore;
    private String reliabilityLabel;   // HIGH / MEDIUM / LOW
    private String reliabilityColor;   // green / yellow / red
    private SentimentType sentiment;
    private String imageUrl;
    private LocalDateTime fetchedAt;
}
