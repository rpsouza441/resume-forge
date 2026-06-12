package com.resumeforge.job.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateJobRequest {

    private UUID resumeProfileId; // optional

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @NotBlank(message = "Job title is required")
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    @NotBlank(message = "Job description is required")
    @Size(min = 100, message = "Job description must be at least 100 characters")
    private String jobDescription;

    private String jobUrl;

    private String jobLocation;

    private String jobType; // fulltime, parttime, contract, internship, remote

    private String seniority; // junior, mid, senior, lead, staff
}