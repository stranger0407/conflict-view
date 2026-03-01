package com.conflictview.controller;

import com.conflictview.dto.*;
import com.conflictview.service.ConflictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conflicts")
@RequiredArgsConstructor
@Tag(name = "Conflicts", description = "Conflict tracking endpoints")
public class ConflictController {

    private final ConflictService conflictService;

    @GetMapping
    @Operation(summary = "Get all active conflicts for map display")
    public ResponseEntity<List<ConflictMapDTO>> getAllConflicts() {
        return ResponseEntity.ok(conflictService.getAllForMap());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full conflict detail")
    public ResponseEntity<ConflictDetailDTO> getConflict(@PathVariable UUID id) {
        return ResponseEntity.ok(conflictService.getDetail(id));
    }

    @GetMapping("/{id}/news")
    @Operation(summary = "Get paginated news articles for a conflict")
    public ResponseEntity<Page<NewsArticleDTO>> getConflictNews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String source) {
        return ResponseEntity.ok(conflictService.getNews(id, page, size, sentiment, source));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get timeline events for a conflict")
    public ResponseEntity<List<ConflictEventDTO>> getConflictEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(conflictService.getEvents(id));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get statistics and chart data for a conflict")
    public ResponseEntity<ConflictStatsDTO> getConflictStats(@PathVariable UUID id) {
        return ResponseEntity.ok(conflictService.getStats(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter conflicts")
    public ResponseEntity<List<ConflictMapDTO>> searchConflicts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(conflictService.search(q, region, severity, type, status));
    }
}
