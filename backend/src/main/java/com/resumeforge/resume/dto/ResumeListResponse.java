package com.resumeforge.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeListResponse {
    private UUID id;
    private String title;
    private Boolean isDefault;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}