package com.conflictview.model;

import com.conflictview.model.enums.SourceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "news_sources",
        uniqueConstraints = @UniqueConstraint(columnNames = "domain"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String domain;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "reliability_score", nullable = false)
    private Integer reliabilityScore;

    @Column(name = "bias_rating", length = 50)
    private String biasRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SourceCategory category = SourceCategory.UNKNOWN;

    @Column(length = 100)
    private String country;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;
}
