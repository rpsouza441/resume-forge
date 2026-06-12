package com.resumeforge.generation.controller;

import com.resumeforge.auth.security.UserPrincipal;
import com.resumeforge.generation.entity.AiRun;
import com.resumeforge.generation.repository.AiRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai-runs")
public class AiRunController {

    private final AiRunRepository aiRunRepository;

    public AiRunController(AiRunRepository aiRunRepository) {
        this.aiRunRepository = aiRunRepository;
    }

    /**
     * GET /api/ai-runs — list AI runs for the current user
     */
    @GetMapping
    public ResponseEntity<Page<AiRunResponse>> listAiRuns(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID generatedResumeId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) OffsetDateTime dateFrom,
            @RequestParam(required = false) OffsetDateTime dateTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AiRun> page = aiRunRepository.findByUserId(principal.getUserId(), pageable);
        return ResponseEntity.ok(page.map(this::toResponse));
    }

    /**
     * GET /api/ai-runs/{id} — get AI run details
     */
    @GetMapping("/{id}")
    public ResponseEntity<AiRunResponse> getAiRun(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        AiRun aiRun = aiRunRepository.findById(id)
                .orElseThrow(() -> new com.resumeforge.exception.ResourceNotFoundException("AI run not found"));
        if (!aiRun.getUser().getId().equals(principal.getUserId())) {
            throw new com.resumeforge.exception.ForbiddenException("Voce nao tem permissao para acessar este AI run");
        }
        return ResponseEntity.ok(toResponse(aiRun));
    }

    private AiRunResponse toResponse(AiRun aiRun) {
        return AiRunResponse.builder()
                .id(aiRun.getId())
                .generatedResumeId(aiRun.getGeneratedResume() != null ? aiRun.getGeneratedResume().getId() : null)
                .runType(aiRun.getRunType().getValue())
                .promptVersion(aiRun.getPromptVersion())
                .aiModel(aiRun.getAiModel())
                .aiProvider(aiRun.getAiProvider())
                .inputTokens(aiRun.getInputTokens())
                .outputTokens(aiRun.getOutputTokens())
                .latencyMs(aiRun.getLatencyMs())
                .estimatedCostUsd(aiRun.getEstimatedCostUsd())
                .status(aiRun.getStatus().getValue())
                .success(aiRun.getSuccess())
                .errorCode(aiRun.getErrorCode())
                .errorMessage(aiRun.getErrorMessage())
                .createdAt(aiRun.getCreatedAt())
                .completedAt(aiRun.getCompletedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AiRunResponse {
        private UUID id;
        private UUID generatedResumeId;
        private String runType;
        private String promptVersion;
        private String aiModel;
        private String aiProvider;
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer latencyMs;
        private java.math.BigDecimal estimatedCostUsd;
        private String status;
        private Boolean success;
        private String errorCode;
        private String errorMessage;
        private OffsetDateTime createdAt;
        private OffsetDateTime completedAt;
    }
}
