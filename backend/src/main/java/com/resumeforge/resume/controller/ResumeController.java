package com.resumeforge.resume.controller;

import com.resumeforge.common.dto.PaginatedResponse;
import com.resumeforge.common.util.SecurityUtils;
import com.resumeforge.resume.dto.*;
import com.resumeforge.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * GET /api/resumes — list resumes with optional filters and pagination.
     * Query params: page, size, sort, title, isDefault
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ResumeListResponse>> listResumes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean isDefault) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = buildPageable(page, size, sort);
        Page<ResumeListResponse> result = resumeService.getResumes(userId, pageable, title, isDefault);

        return ResponseEntity.ok(toPaginatedResponse(result));
    }

    /**
     * GET /api/resumes/{id} — get a single resume.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResume(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ResumeResponse response = resumeService.getResume(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/resumes — create a new resume.
     * Returns 201 Created.
     */
    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(@Valid @RequestBody CreateResumeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ResumeResponse response = resumeService.createResume(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/resumes/{id} — update an existing resume.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResume(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResumeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ResumeResponse response = resumeService.updateResume(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/resumes/{id} — soft-delete a resume.
     * Returns 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        resumeService.deleteResume(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/resumes/{id}/default — set resume as the user's default.
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefaultResume(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        resumeService.setDefaultResume(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Pageable buildPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String property = parts[0];
        org.springframework.data.domain.Sort.Direction direction =
                parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                        ? org.springframework.data.domain.Sort.Direction.ASC
                        : org.springframework.data.domain.Sort.Direction.DESC;
        return PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, property));
    }

    private PaginatedResponse<ResumeListResponse> toPaginatedResponse(Page<ResumeListResponse> page) {
        return PaginatedResponse.<ResumeListResponse>builder()
                .data(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}