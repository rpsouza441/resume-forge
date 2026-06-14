package com.resumeforge.generation.controller;

import com.resumeforge.auth.security.UserPrincipal;
import com.resumeforge.common.dto.PaginatedResponse;
import com.resumeforge.generation.dto.*;
import com.resumeforge.generation.entity.AnalysisReport;
import com.resumeforge.generation.service.GenerationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    /**
     * POST /api/generate — create new generation
     */
    @PostMapping("/generate")
    public ResponseEntity<GenerationResponse> generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerationRequest request) {
        GenerationResponse response = generationService.generate(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/generated — list generated resumes (with pagination and filters)
     * Returns PaginatedResponse with data, page, size, total, totalPages
     */
    @GetMapping("/generated")
    public ResponseEntity<PaginatedResponse<GeneratedResumeResponse>> listGenerated(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) UUID jobApplicationId,
            @RequestParam(required = false) UUID resumeProfileId,
            @RequestParam(required = false) OffsetDateTime dateFrom,
            @RequestParam(required = false) OffsetDateTime dateTo,
            @RequestParam(required = false) Boolean isCurrent,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<GeneratedResumeResponse> page = generationService.listGenerated(
                principal.getUserId(), companyName, jobTitle, jobApplicationId,
                resumeProfileId, dateFrom, dateTo, isCurrent, pageable);

        PaginatedResponse<GeneratedResumeResponse> response = PaginatedResponse.<GeneratedResumeResponse>builder()
                .data(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/generated/{id} — get one with analysis
     */
    @GetMapping("/generated/{id}")
    public ResponseEntity<GeneratedResumeResponse> getGenerated(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        GeneratedResumeResponse response = generationService.getGenerated(principal.getUserId(), id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/generated/{id}/analysis — get analysis only
     */
    @GetMapping("/generated/{id}/analysis")
    public ResponseEntity<AnalysisReport> getAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        AnalysisReport analysis = generationService.getAnalysis(principal.getUserId(), id);
        return ResponseEntity.ok(analysis);
    }

    /**
     * PUT /api/generated/{id} — save manual edit (creates new version)
     */
    @PutMapping("/generated/{id}")
    public ResponseEntity<GenerationResponse> saveManualEdit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody ManualEditRequest request) {
        GenerationResponse response = generationService.saveManualEdit(principal.getUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/generated/{id}/regenerate — regenerate via AI
     */
    @PostMapping("/generated/{id}/regenerate")
    public ResponseEntity<GenerationResponse> regenerate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        GenerationResponse response = generationService.regenerate(principal.getUserId(), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/generated/{id}/versions — list all versions
     */
    @GetMapping("/generated/{id}/versions")
    public ResponseEntity<List<VersionResponse>> listVersions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        List<VersionResponse> versions = generationService.listVersions(principal.getUserId(), id);
        return ResponseEntity.ok(versions);
    }
}
