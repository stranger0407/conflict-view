package com.conflictview.dto;

import com.conflictview.model.enums.SourceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSourceDTO {
    private UUID id;
    private String domain;
    private String name;
    private Integer reliabilityScore;
    private String reliabilityLabel;
    private String reliabilityColor;
    private String biasRating;
    private SourceCategory category;
    private String country;
    private String logoUrl;
}
