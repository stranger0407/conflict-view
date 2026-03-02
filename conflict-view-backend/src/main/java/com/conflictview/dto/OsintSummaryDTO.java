package com.conflictview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OsintSummaryDTO {
    private long videoCount;
    private long imageCount;
    private long mapCount;
    private long infographicCount;
    private long reportCount;
    private long satelliteCount;
    private long eventDataCount;
    private long totalCount;
}
