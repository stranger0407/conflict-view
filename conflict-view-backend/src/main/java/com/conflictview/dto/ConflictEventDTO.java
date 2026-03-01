package com.conflictview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictEventDTO {
    private UUID id;
    private UUID conflictId;
    private LocalDate eventDate;
    private String eventType;
    private String description;
    private String sourceUrl;
    private Double latitude;
    private Double longitude;
    private Integer fatalitiesReported;
}
