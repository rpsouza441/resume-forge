package com.resumeforge.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.exception.ConflictException;
import com.resumeforge.exception.ResourceNotFoundException;
import com.resumeforge.generation.repository.GeneratedResumeRepository;
import com.resumeforge.resume.dto.*;
import com.resumeforge.resume.entity.ResumeProfile;
import com.resumeforge.resume.repository.ResumeProfileRepository;
import com.resumeforge.auth.entity.User;
import com.resumeforge.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeProfileRepository resumeRepository;
    private final GeneratedResumeRepository generatedResumeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new resume profile for the given user.
     * - Generates contentText from markdown.
     * - Sets contentJsonb from request.
     * - If first resume for user, sets isDefault to true.
     * - If isDefault=true, clears other defaults in same transaction.
     */
    @Transactional
    public ResumeResponse createResume(UUID userId, CreateResumeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String contentText = stripMarkdown(request.getContentMarkdown());
        String contentJsonb = serializeJsonb(request.getContentJsonb());

        boolean isFirst = resumeRepository.countByUserIdAndDeletedAtIsNull(userId) == 0;
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault()) || isFirst;

        // If this resume will be default, clear other defaults first
        if (isDefault) {
            resumeRepository.findByUserIdAndDeletedAtIsNull(userId)
                    .forEach(existing -> {
                        existing.setIsDefault(false);
                        existing.setUpdatedAt(OffsetDateTime.now());
                    });
        }

        ResumeProfile resume = ResumeProfile.builder()
                .user(user)
                .title(request.getTitle())
                .contentText(contentText)
                .contentMarkdown(request.getContentMarkdown())
                .contentJsonb(contentJsonb)
                .isDefault(isDefault)
                .build();

        ResumeProfile saved = resumeRepository.save(resume);
        return toResponse(saved);
    }

    /**
     * Retrieves a single resume by ID, validating ownership.
     */
    @Transactional(readOnly = true)
    public ResumeResponse getResume(UUID userId, UUID resumeId) {
        ResumeProfile resume = findAndValidateOwnership(userId, resumeId);
        return toResponse(resume);
    }

    /**
     * Lists resumes with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<ResumeListResponse> getResumes(UUID userId, Pageable pageable,
                                               String title, Boolean isDefault) {
        Page<ResumeProfile> page = resumeRepository.findAll(
                buildResumeSpecification(userId, title, isDefault), pageable);
        return page.map(this::toListResponse);
    }

    /**
     * Updates an existing resume.
     */
    @Transactional
    public ResumeResponse updateResume(UUID userId, UUID resumeId, UpdateResumeRequest request) {
        ResumeProfile resume = findAndValidateOwnership(userId, resumeId);

        if (request.getTitle() != null) {
            resume.setTitle(request.getTitle());
        }
        if (request.getContentMarkdown() != null) {
            resume.setContentMarkdown(request.getContentMarkdown());
            resume.setContentText(stripMarkdown(request.getContentMarkdown()));
        }
        if (request.getContentJsonb() != null) {
            resume.setContentJsonb(serializeJsonb(request.getContentJsonb()));
        }
        if (request.getIsDefault() != null && request.getIsDefault()) {
            // Clear other defaults
            resumeRepository.findByUserIdAndDeletedAtIsNull(userId)
                    .forEach(other -> {
                        if (!other.getId().equals(resumeId)) {
                            other.setIsDefault(false);
                            other.setUpdatedAt(OffsetDateTime.now());
                        }
                    });
            resume.setIsDefault(true);
        }

        ResumeProfile saved = resumeRepository.save(resume);
        return toResponse(saved);
    }

    /**
     * Soft-deletes a resume. Returns 409 Conflict if active generated resumes exist.
     */
    @Transactional
    public void deleteResume(UUID userId, UUID resumeId) {
        ResumeProfile resume = findAndValidateOwnership(userId, resumeId);

        boolean hasActiveGenerated = !generatedResumeRepository
                .findByResumeProfileIdAndDeletedAtIsNull(resumeId, Pageable.unpaged())
                .getContent().isEmpty();

        if (hasActiveGenerated) {
            throw new ConflictException(
                    "Cannot delete resume: there are generated resumes linked to it. " +
                    "Delete the generated resumes first or keep the base resume to preserve history.");
        }

        resume.setDeletedAt(OffsetDateTime.now());
        resume.setUpdatedAt(OffsetDateTime.now());
        resumeRepository.save(resume);
    }

    /**
     * Sets a resume as the default, clearing other defaults for the user.
     */
    @Transactional
    public void setDefaultResume(UUID userId, UUID resumeId) {
        ResumeProfile resume = findAndValidateOwnership(userId, resumeId);

        resumeRepository.findByUserIdAndDeletedAtIsNull(userId)
                .forEach(other -> {
                    if (!other.getId().equals(resumeId)) {
                        other.setIsDefault(false);
                        other.setUpdatedAt(OffsetDateTime.now());
                    }
                });

        resume.setIsDefault(true);
        resume.setUpdatedAt(OffsetDateTime.now());
        resumeRepository.save(resume);
    }

    // --- private helpers ---

    private ResumeProfile findAndValidateOwnership(UUID userId, UUID resumeId) {
        ResumeProfile resume = resumeRepository.findByIdAndUserIdAndDeletedAtIsNull(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
        return resume;
    }

    private ResumeResponse toResponse(ResumeProfile resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .contentText(resume.getContentText())
                .contentMarkdown(resume.getContentMarkdown())
                .contentJsonb(deserializeJsonb(resume.getContentJsonb()))
                .isDefault(resume.getIsDefault())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    private ResumeListResponse toListResponse(ResumeProfile resume) {
        return ResumeListResponse.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .isDefault(resume.getIsDefault())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    /**
     * Strips markdown syntax to produce plain text.
     */
    private String stripMarkdown(String markdown) {
        if (markdown == null) return "";
        String text = markdown;
        // Remove ATX-style headings (# )
        text = Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE).matcher(text).replaceAll("");
        // Remove setext-style headings (==== / ----)
        text = Pattern.compile("^[=-]{3,}\\s*$", Pattern.MULTILINE).matcher(text).replaceAll("");
        // Remove bold (**text**)
        text = Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(text).replaceAll("$1");
        // Remove italic (*text*)
        text = Pattern.compile("(?<!\\*)\\*([^*]+?)\\*(?!\\*)").matcher(text).replaceAll("$1");
        // Remove code fences and inline code
        text = Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE).matcher(text).replaceAll("");
        text = Pattern.compile("`([^`]+)`").matcher(text).replaceAll("$1");
        // Remove links [text](url)
        text = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)").matcher(text).replaceAll("$1");
        // Remove images ![text](url)
        text = Pattern.compile("!\\[([^\\]]*)]\\([^)]+\\)").matcher(text).replaceAll("");
        // Remove blockquote >
        text = Pattern.compile("^>\\s*", Pattern.MULTILINE).matcher(text).replaceAll("");
        // Remove unordered list markers
        text = Pattern.compile("^[-*+]\\s+", Pattern.MULTILINE).matcher(text).replaceAll("");
        // Remove ordered list markers
        text = Pattern.compile("^\\d+\\.\\s+", Pattern.MULTILINE).matcher(text).replaceAll("");
        // Collapse multiple blank lines
        text = Pattern.compile("\\n{3,}").matcher(text).replaceAll("\n\n");
        return text.trim();
    }

    private String serializeJsonb(Map<String, Object> contentJsonb) {
        if (contentJsonb == null || contentJsonb.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(contentJsonb);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> deserializeJsonb(String jsonb) {
        if (jsonb == null || jsonb.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(jsonb, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private org.springframework.data.jpa.domain.Specification<ResumeProfile> buildResumeSpecification(
            UUID userId, String title, Boolean isDefault) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = cb.and(predicates,
                    cb.equal(root.get("user").get("id"), userId));
            if (title != null && !title.isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (isDefault != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("isDefault"), isDefault));
            }
            return predicates;
        };
    }
}