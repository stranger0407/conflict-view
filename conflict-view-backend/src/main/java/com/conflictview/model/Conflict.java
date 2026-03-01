package com.conflictview.model;

import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ConflictType;
import com.conflictview.model.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conflicts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conflict {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String region;

    @Column(name = "country_codes", length = 50)
    private String countryCodes;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false)
    private ConflictType conflictType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConflictStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "casualty_estimate")
    private Integer casualtyEstimate;

    @Column(name = "displaced_estimate")
    private Integer displacedEstimate;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "involved_parties", length = 500)
    private String involvedParties;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "conflict", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<NewsArticle> newsArticles = new ArrayList<>();

    @OneToMany(mappedBy = "conflict", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConflictEvent> events = new ArrayList<>();
}
