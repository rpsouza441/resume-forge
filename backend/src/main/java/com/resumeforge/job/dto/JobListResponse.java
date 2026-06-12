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
public class JobListResponse {
    private UUID id;
    private String companyName;
    private String jobTitle;
    private String jobType;
    private String seniority;
    private String status;
    private OffsetDateTime createdAt;
}