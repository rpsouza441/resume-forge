package com.resumeforge.job.controller;

import com.resumeforge.common.dto.PaginatedResponse;
import com.resumeforge.common.util.SecurityUtils;
import com.resumeforge.job.dto.*;
import com.resumeforge.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    /**
     * GET /api/jobs — list jobs with optional filters and pagination.
     * Query params: companyName, jobTitle, status, dateFrom, dateTo, page, size
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<JobListResponse>> listJobs(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<JobListResponse> result = jobService.getJobs(
                userId, pageable, companyName, jobTitle, status, dateFrom, dateTo);

        return ResponseEntity.ok(toPaginatedResponse(result));
    }

    /**
     * GET /api/jobs/{id} — get a single job.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        JobResponse response = jobService.getJob(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/jobs — create a new job.
     * Returns 201 Created.
     */
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        JobResponse response = jobService.createJob(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/jobs/{id} — update an existing job.
     */
    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        JobResponse response = jobService.updateJob(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/jobs/{id} — soft-delete a job.
     * Returns 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        jobService.deleteJob(userId, id);
        return ResponseEntity.noContent().build();
    }

    private PaginatedResponse<JobListResponse> toPaginatedResponse(Page<JobListResponse> page) {
        return PaginatedResponse.<JobListResponse>builder()
                .data(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}