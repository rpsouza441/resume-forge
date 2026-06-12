package com.resumeforge.resume.repository;

import com.resumeforge.resume.entity.ResumeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, UUID>, JpaSpecificationExecutor<ResumeProfile> {

    List<ResumeProfile> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<ResumeProfile> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    long countByUserIdAndDeletedAtIsNull(UUID userId);

    @Query("SELECT rp FROM ResumeProfile rp WHERE rp.user.id = :userId AND rp.isDefault = true AND rp.deletedAt IS NULL")
    Optional<ResumeProfile> findDefaultByUserId(@Param("userId") UUID userId);
}
