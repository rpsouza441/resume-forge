package com.resumeforge.logging.entity;

import com.resumeforge.model.enums.LogCategory;
import com.resumeforge.model.enums.LogLevel;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "processing_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private com.resumeforge.auth.entity.User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_run_id")
    private com.resumeforge.generation.entity.AiRun aiRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_resume_id")
    private com.resumeforge.generation.entity.GeneratedResume generatedResume;

    @Convert(converter = LogLevel.LogLevelConverter.class)
    @Column(name = "log_level", nullable = false, length = 20)
    private LogLevel logLevel;

    @Convert(converter = LogCategory.LogCategoryConverter.class)
    @Column(nullable = false, length = 50)
    private LogCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> contextData = new HashMap<>();

    @Column(name = "source_service")
    private String sourceService;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
