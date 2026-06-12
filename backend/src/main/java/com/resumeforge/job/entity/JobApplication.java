package com.resumeforge.job.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private com.resumeforge.auth.entity.User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_profile_id")
    private com.resumeforge.resume.entity.ResumeProfile resumeProfile;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "job_url", columnDefinition = "TEXT")
    private String jobUrl;

    @Column(name = "job_description", nullable = false, columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "job_source")
    private String jobSource;

    @Column(name = "job_location")
    private String jobLocation;

    @Column(name = "job_type")
    private String jobType;

    @Column
    private String seniority;

    @Column(nullable = false)
    private String status = "saved";

    @Column(name = "status_changed_at", nullable = false)
    private OffsetDateTime statusChangedAt = OffsetDateTime.now();

    @Column(name = "applied_at")
    private OffsetDateTime appliedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "generated_count", nullable = false)
    private Integer generatedCount = 0;

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
        statusChangedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
