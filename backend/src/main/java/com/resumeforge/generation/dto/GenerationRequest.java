package com.resumeforge.generation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequest {
    @NotNull(message = "resumeProfileId e obrigatorio")
    private UUID resumeProfileId;

    @NotNull(message = "jobApplicationId e obrigatorio")
    private UUID jobApplicationId;

    private String extraInstructions;
}
