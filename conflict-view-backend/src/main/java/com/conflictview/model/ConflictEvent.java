package com.conflictview.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "conflict_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conflict_id", nullable = false)
    private Conflict conflict;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "fatalities_reported")
    private Integer fatalitiesReported;
}
