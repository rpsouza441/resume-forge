package com.resumeforge.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationResponse {
    private UUID id;
    private UUID resumeProfileId;
    private UUID jobApplicationId;
    private Integer versionNumber;
    private Boolean isCurrent;
    private String status;
    private String contentMarkdown;
    private String contentText;
    private Map<String, Object> contentJsonb;
    private AnalysisDto analysis;
    private AiRunDto aiRun;
    private OffsetDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisDto {
        private UUID id;
        private int adherenceScore;
        private String adherenceClassification;
        private String summary;
        private Map<String, List<String>> keywordMap;
        private List<Map<String, Object>> gaps;
        private List<String> warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRunDto {
        private UUID id;
        private String provider;
        private String model;
        private String status;
    }
}
