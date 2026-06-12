package com.resumeforge.generation.repository;

import com.resumeforge.generation.entity.AiRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiRunRepository extends JpaRepository<AiRun, UUID> {

    Page<AiRun> findByUserId(UUID userId, Pageable pageable);

    List<AiRun> findByGeneratedResumeId(UUID generatedResumeId);
}
