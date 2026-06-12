package com.resumeforge.generation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "generated_resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class GeneratedResume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_profile_id", nullable = false)
    private com.resumeforge.resume.entity.ResumeProfile resumeProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id")
    private com.resumeforge.job.entity.JobApplication jobApplication;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_version_id")
    private GeneratedResume parentVersion;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    private String contentMarkdown;

    @JdbcType(SqlTypes.JSON)
    @Column(name = "content_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> contentJsonb = new HashMap<>();

    @Column(name = "ai_model", nullable = false)
    private String aiModel;

    @Column(name = "ai_provider", nullable = false)
    private String aiProvider;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion = "v1";

    @Column(name = "generation_reason", columnDefinition = "TEXT")
    private String generationReason;

    @Column(nullable = false)
    private String status = "completed";

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "char_count")
    private Integer charCount;

    @Column(name = "match_score", precision = 5, scale = 2)
    private java.math.BigDecimal matchScore;

    @Column(name = "readability_score", precision = 5, scale = 2)
    private java.math.BigDecimal readabilityScore;

    @Column(name = "docx_generated_at")
    private OffsetDateTime docxGeneratedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
