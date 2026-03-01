package com.conflictview.model;

import com.conflictview.model.enums.SentimentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "news_articles",
        uniqueConstraints = @UniqueConstraint(columnNames = "url"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conflict_id", nullable = false)
    private Conflict conflict;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "source_domain", length = 200)
    private String sourceDomain;

    @Column(length = 200)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "reliability_score")
    private Integer reliabilityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SentimentType sentiment = SentimentType.NEUTRAL;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "fetched_at", updatable = false)
    private LocalDateTime fetchedAt;
}
