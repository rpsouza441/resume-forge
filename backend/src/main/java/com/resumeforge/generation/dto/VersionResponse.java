package com.resumeforge.generation.dto;

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
public class VersionResponse {
    private UUID id;
    private Integer versionNumber;
    private Boolean isCurrent;
    private String generationReason;
    private String aiProvider;
    private String aiModel;
    private OffsetDateTime createdAt;
}
