package com.resumeforge.job.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobRequest {

    private UUID resumeProfileId;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    @Size(min = 100, message = "Job description must be at least 100 characters")
    private String jobDescription;

    private String jobUrl;

    private String jobLocation;

    private String jobType;

    private String seniority;
}