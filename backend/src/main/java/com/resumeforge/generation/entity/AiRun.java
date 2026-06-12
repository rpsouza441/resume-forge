package com.resumeforge.generation.entity;

import com.resumeforge.model.converter.AiRunStatusConverter;
import com.resumeforge.model.converter.AiRunTypeConverter;
import com.resumeforge.model.enums.AiRunStatus;
import com.resumeforge.model.enums.AiRunType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private com.resumeforge.auth.entity.User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_resume_id")
    private GeneratedResume generatedResume;

    @Convert(converter = AiRunTypeConverter.class)
    @Column(name = "run_type", nullable = false, length = 50)
    private AiRunType runType;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "ai_model", nullable = false)
    private String aiModel;

    @Column(name = "ai_provider", nullable = false)
    private String aiProvider;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "estimated_cost_usd", precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @JdbcType(SqlTypes.JSON)
    @Column(name = "parsed_response", columnDefinition = "jsonb")
    private Map<String, Object> parsedResponse = new HashMap<>();

    @JdbcType(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> context = new HashMap<>();

    @Convert(converter = AiRunStatusConverter.class)
    @Column(nullable = false, length = 30)
    private AiRunStatus status = AiRunStatus.STARTED;

    @Column
    private Boolean success;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
