package com.resumeforge.job.dto;

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
public class JobResponse {
    private UUID id;
    private UUID resumeProfileId;
    private String companyName;
    private String jobTitle;
    private String jobDescription;
    private String jobUrl;
    private String jobLocation;
    private String jobType;
    private String seniority;
    private String status;
    private OffsetDateTime statusChangedAt;
    private OffsetDateTime appliedAt;
    private String notes;
    private String contactName;
    private String contactEmail;
    private Integer generatedCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}