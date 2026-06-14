package com.resumeforge.job.service;

import com.resumeforge.exception.ConflictException;
import com.resumeforge.exception.ResourceNotFoundException;
import com.resumeforge.generation.repository.GeneratedResumeRepository;
import com.resumeforge.job.dto.*;
import com.resumeforge.job.entity.JobApplication;
import com.resumeforge.job.repository.JobApplicationRepository;
import com.resumeforge.resume.entity.ResumeProfile;
import com.resumeforge.resume.repository.ResumeProfileRepository;
import com.resumeforge.auth.entity.User;
import com.resumeforge.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobApplicationRepository jobRepository;
    private final ResumeProfileRepository resumeRepository;
    private final GeneratedResumeRepository generatedResumeRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new job application.
     * Validates resumeProfileId belongs to user if provided.
     */
    @Transactional
    public JobResponse createJob(UUID userId, CreateJobRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ResumeProfile resumeProfile = null;
        if (request.getResumeProfileId() != null) {
            resumeProfile = resumeRepository.findByIdAndUserIdAndDeletedAtIsNull(
                    request.getResumeProfileId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Resume profile", "id", request.getResumeProfileId()));
        }

        JobApplication job = JobApplication.builder()
                .user(user)
                .resumeProfile(resumeProfile)
                .companyName(request.getCompanyName())
                .jobTitle(request.getJobTitle())
                .jobDescription(request.getJobDescription())
                .jobUrl(request.getJobUrl())
                .jobLocation(request.getJobLocation())
                .jobType(request.getJobType())
                .seniority(request.getSeniority())
                .status("saved")
                .build();

        JobApplication saved = jobRepository.save(job);
        return toResponse(saved);
    }

    /**
     * Retrieves a single job by ID, validating ownership.
     */
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID userId, UUID jobId) {
        JobApplication job = findAndValidateOwnership(userId, jobId);
        return toResponse(job);
    }

    /**
     * Lists jobs with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<JobListResponse> getJobs(UUID userId, Pageable pageable,
                                        String companyName, String jobTitle,
                                        String status, OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        Page<JobApplication> page = jobRepository.findAll(
                buildJobSpecification(userId, companyName, jobTitle, status, dateFrom, dateTo), pageable);
        return page.map(this::toListResponse);
    }

    /**
     * Updates an existing job.
     */
    @Transactional
    public JobResponse updateJob(UUID userId, UUID jobId, UpdateJobRequest request) {
        JobApplication job = findAndValidateOwnership(userId, jobId);

        if (request.getResumeProfileId() != null) {
            ResumeProfile resumeProfile = resumeRepository.findByIdAndUserIdAndDeletedAtIsNull(
                    request.getResumeProfileId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Resume profile", "id", request.getResumeProfileId()));
            job.setResumeProfile(resumeProfile);
        }

        if (request.getCompanyName() != null) {
            job.setCompanyName(request.getCompanyName());
        }
        if (request.getJobTitle() != null) {
            job.setJobTitle(request.getJobTitle());
        }
        if (request.getJobDescription() != null) {
            job.setJobDescription(request.getJobDescription());
        }
        if (request.getJobUrl() != null) {
            job.setJobUrl(request.getJobUrl());
        }
        if (request.getJobLocation() != null) {
            job.setJobLocation(request.getJobLocation());
        }
        if (request.getJobType() != null) {
            job.setJobType(request.getJobType());
        }
        if (request.getSeniority() != null) {
            job.setSeniority(request.getSeniority());
        }

        JobApplication saved = jobRepository.save(job);
        return toResponse(saved);
    }

    /**
     * Soft-deletes a job. Returns 409 Conflict if active generated resumes are linked.
     */
    @Transactional
    public void deleteJob(UUID userId, UUID jobId) {
        JobApplication job = findAndValidateOwnership(userId, jobId);

        boolean hasActiveGenerated = !generatedResumeRepository
                .findByJobApplicationIdAndDeletedAtIsNull(jobId, Pageable.unpaged())
                .getContent().isEmpty();

        if (hasActiveGenerated) {
            throw new ConflictException(
                    "Cannot delete job: there are generated resumes linked to it. " +
                    "Delete the generated resumes first or keep the job to preserve history.");
        }

        job.setDeletedAt(OffsetDateTime.now());
        job.setUpdatedAt(OffsetDateTime.now());
        jobRepository.save(job);
    }

    /**
     * Archives a job by setting its status to 'archived'.
     */
    @Transactional
    public void archiveJob(UUID userId, UUID jobId) {
        JobApplication job = findAndValidateOwnership(userId, jobId);
        job.setStatus("archived");
        job.setStatusChangedAt(OffsetDateTime.now());
        job.setUpdatedAt(OffsetDateTime.now());
        jobRepository.save(job);
    }

    // --- private helpers ---

    private JobApplication findAndValidateOwnership(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserIdAndDeletedAtIsNull(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));
    }

    private JobResponse toResponse(JobApplication job) {
        return JobResponse.builder()
                .id(job.getId())
                .resumeProfileId(job.getResumeProfile() != null ? job.getResumeProfile().getId() : null)
                .companyName(job.getCompanyName())
                .jobTitle(job.getJobTitle())
                .jobDescription(job.getJobDescription())
                .jobUrl(job.getJobUrl())
                .jobLocation(job.getJobLocation())
                .jobType(job.getJobType())
                .seniority(job.getSeniority())
                .status(job.getStatus())
                .statusChangedAt(job.getStatusChangedAt())
                .appliedAt(job.getAppliedAt())
                .notes(job.getNotes())
                .contactName(job.getContactName())
                .contactEmail(job.getContactEmail())
                .generatedCount(job.getGeneratedCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private JobListResponse toListResponse(JobApplication job) {
        return JobListResponse.builder()
                .id(job.getId())
                .companyName(job.getCompanyName())
                .jobTitle(job.getJobTitle())
                .jobType(job.getJobType())
                .seniority(job.getSeniority())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private Specification<JobApplication> buildJobSpecification(
            UUID userId, String companyName, String jobTitle,
            String status, OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = cb.and(predicates,
                    cb.equal(root.get("user").get("id"), userId));

            if (companyName != null && !companyName.isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.function("lower", String.class,
                                cb.coalesce(cb.function("text", String.class, root.get("companyName")),
                                        cb.literal(""))),
                                "%" + companyName.toLowerCase() + "%"));
            }
            if (jobTitle != null && !jobTitle.isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.function("lower", String.class,
                                cb.coalesce(cb.function("text", String.class, root.get("jobTitle")),
                                        cb.literal(""))),
                                "%" + jobTitle.toLowerCase() + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("status"), status));
            }
            if (dateFrom != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            return predicates;
        };
    }
}