package com.conflictview.dto;

import com.conflictview.model.enums.ResourceType;
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
public class OsintResourceDTO {
    private UUID id;
    private UUID conflictId;
    private ResourceType resourceType;
    private String title;
    private String url;
    private String thumbnailUrl;
    private String description;
    private String sourcePlatform;
    private String author;
    private LocalDateTime publishedAt;
    private String duration;
}
