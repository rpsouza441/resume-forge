package com.resumeforge.resume.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResumeRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(min = 50, message = "Content must be at least 50 characters")
    private String contentMarkdown;

    private Map<String, Object> contentJsonb;

    private Boolean isDefault;
}