package com.resumeforge.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResumeResponse {
    private UUID id;
    private UUID resumeProfileId;
    private UUID jobApplicationId;
    private String companyName;
    private String jobTitle;
    private Integer versionNumber;
    private Boolean isCurrent;
    private String status;
    private String contentMarkdown;
    private String contentText;
    private Map<String, Object> contentJsonb;
    private Integer adherenceScore;
    private String aiProvider;
    private String aiModel;
    private String generationReason;
    private Integer wordCount;
    private Integer charCount;
    private AnalysisReportDto analysis;
    private OffsetDateTime createdAt;
    private OffsetDateTime docxGeneratedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisReportDto {
        private UUID id;
        private Integer overallScore;
        private String adherenceClassification;
        private java.util.Map<String, Object> findings;
        private java.util.Map<String, Object> recommendations;
        private java.util.Map<String, Object> analyzedFields;
    }
}
