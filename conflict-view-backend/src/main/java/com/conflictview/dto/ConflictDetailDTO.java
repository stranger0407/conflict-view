package com.conflictview.dto;

import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ConflictType;
import com.conflictview.model.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDetailDTO {
    private UUID id;
    private String name;
    private String region;
    private String countryCodes;
    private Double latitude;
    private Double longitude;
    private Severity severity;
    private ConflictType conflictType;
    private ConflictStatus status;
    private LocalDate startDate;
    private String summary;
    private Integer casualtyEstimate;
    private Integer displacedEstimate;
    private String involvedParties;
    private String thumbnailUrl;
    private long articleCount;
    private long eventCount;
    private LocalDateTime updatedAt;
}
