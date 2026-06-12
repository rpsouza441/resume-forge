package com.resumeforge.generation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualEditRequest {
    private String contentMarkdown;
    private String contentJsonb;
}
