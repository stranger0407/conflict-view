package com.conflictview.model;

import com.conflictview.model.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "osint_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OsintResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conflict_id", nullable = false)
    private Conflict conflict;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000, unique = true)
    private String url;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_platform", nullable = false, length = 100)
    private String sourcePlatform;

    @Column(length = 200)
    private String author;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(length = 20)
    private String duration;

    @CreationTimestamp
    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
