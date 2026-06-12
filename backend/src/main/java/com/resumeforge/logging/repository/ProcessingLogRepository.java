package com.resumeforge.logging.repository;

import com.resumeforge.logging.entity.ProcessingLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, UUID> {

    Page<ProcessingLog> findByUserId(UUID userId, Pageable pageable);

    Page<ProcessingLog> findByAiRunId(UUID aiRunId, Pageable pageable);

    Page<ProcessingLog> findByGeneratedResumeId(UUID generatedResumeId, Pageable pageable);
}
