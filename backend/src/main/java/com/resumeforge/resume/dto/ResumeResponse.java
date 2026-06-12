package com.resumeforge.resume.dto;

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
public class ResumeResponse {
    private UUID id;
    private String title;
    private String contentText;
    private String contentMarkdown;
    private Map<String, Object> contentJsonb;
    private Boolean isDefault;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}