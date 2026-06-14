package com.resumeforge.generation.entity;

import com.resumeforge.model.converter.ReportTypeConverter;
import com.resumeforge.model.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analysis_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_resume_id", nullable = false)
    private GeneratedResume generatedResume;

    @Convert(converter = ReportTypeConverter.class)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType = ReportType.COMBINED;

    @Column(name = "report_version", nullable = false)
    private String reportVersion = "v1";

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "ats_compatibility_score", precision = 5, scale = 2)
    private BigDecimal atsCompatibilityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dimension_scores", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> dimensionScores = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> findings = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> recommendations = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analyzed_fields", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> analyzedFields = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
