package com.resumeforge.job.repository;

import com.resumeforge.job.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID>, JpaSpecificationExecutor<JobApplication> {

    List<JobApplication> findByUserIdAndDeletedAtIsNull(UUID userId);

    Page<JobApplication> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Optional<JobApplication> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
