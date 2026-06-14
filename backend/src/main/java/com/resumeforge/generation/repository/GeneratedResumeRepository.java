package com.resumeforge.generation.repository;

import com.resumeforge.generation.entity.GeneratedResume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedResumeRepository extends JpaRepository<GeneratedResume, UUID>, JpaSpecificationExecutor<GeneratedResume> {

    @Query("SELECT gr FROM GeneratedResume gr WHERE gr.resumeProfile.id = :profileId AND gr.jobApplication.id = :applicationId AND gr.isCurrent = true AND gr.deletedAt IS NULL")
    Optional<GeneratedResume> findCurrentByResumeProfileIdAndJobApplicationId(
            @Param("profileId") UUID profileId,
            @Param("applicationId") UUID applicationId);

    List<GeneratedResume> findByResumeProfileIdAndJobApplicationIdAndDeletedAtIsNull(UUID resumeProfileId, UUID jobApplicationId);

    Optional<GeneratedResume> findByIdAndDeletedAtIsNull(UUID id);

    Page<GeneratedResume> findByResumeProfileIdAndDeletedAtIsNull(UUID resumeProfileId, Pageable pageable);

    Page<GeneratedResume> findByJobApplicationIdAndDeletedAtIsNull(UUID jobApplicationId, Pageable pageable);

    @Query("SELECT gr FROM GeneratedResume gr WHERE gr.resumeProfile.id = :profileId AND gr.isCurrent = true AND gr.deletedAt IS NULL")
    Optional<GeneratedResume> findCurrentByResumeProfileId(@Param("profileId") UUID profileId);

    @Query("SELECT gr FROM GeneratedResume gr WHERE gr.resumeProfile.user.id = :userId AND gr.deletedAt IS NULL")
    Page<GeneratedResume> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT gr FROM GeneratedResume gr " +
           "LEFT JOIN gr.jobApplication ja " +
           "WHERE gr.resumeProfile.user.id = :userId AND gr.deletedAt IS NULL " +
           "AND (:companyName IS NULL OR LOWER(COALESCE(ja.companyName, '')) LIKE LOWER(CONCAT('%', :companyName, '%'))) " +
           "AND (:jobTitle IS NULL OR LOWER(COALESCE(ja.jobTitle, '')) LIKE LOWER(CONCAT('%', :jobTitle, '%'))) " +
           "AND (:resumeProfileId IS NULL OR gr.resumeProfile.id = :resumeProfileId) " +
           "AND (:dateFrom IS NULL OR gr.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR gr.createdAt <= :dateTo) " +
           "AND (:isCurrent IS NULL OR gr.isCurrent = :isCurrent)")
    Page<GeneratedResume> findByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("companyName") String companyName,
            @Param("jobTitle") String jobTitle,
            @Param("resumeProfileId") UUID resumeProfileId,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            @Param("isCurrent") Boolean isCurrent,
            Pageable pageable);
}
