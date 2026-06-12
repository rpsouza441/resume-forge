package com.resumeforge.generation.repository;

import com.resumeforge.generation.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, UUID> {

    Optional<AnalysisReport> findByGeneratedResumeId(UUID generatedResumeId);
}
