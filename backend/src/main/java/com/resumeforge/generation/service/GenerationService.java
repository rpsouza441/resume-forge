package com.resumeforge.generation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.ai.provider.GenerationOptions;
import com.resumeforge.ai.provider.GenerationResult;
import com.resumeforge.ai.service.AiOrchestrationService;
import com.resumeforge.auth.entity.User;
import com.resumeforge.auth.repository.UserRepository;
import com.resumeforge.exception.AiGenerationException;
import com.resumeforge.exception.ForbiddenException;
import com.resumeforge.exception.ResourceNotFoundException;
import com.resumeforge.exception.ValidationException;
import com.resumeforge.generation.dto.*;
import com.resumeforge.generation.entity.AiRun;
import com.resumeforge.generation.specification.GeneratedResumeSpecification;
import com.resumeforge.model.enums.AiRunStatus;
import com.resumeforge.model.enums.AiRunType;
import com.resumeforge.generation.entity.AnalysisReport;
import com.resumeforge.generation.entity.GeneratedResume;
import com.resumeforge.generation.repository.AiRunRepository;
import com.resumeforge.generation.repository.AnalysisReportRepository;
import com.resumeforge.generation.repository.GeneratedResumeRepository;
import com.resumeforge.job.entity.JobApplication;
import com.resumeforge.job.repository.JobApplicationRepository;
import com.resumeforge.logging.service.LoggingService;
import com.resumeforge.resume.entity.ResumeProfile;
import com.resumeforge.resume.repository.ResumeProfileRepository;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private final GeneratedResumeRepository generatedResumeRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final AiRunRepository aiRunRepository;
    private final ResumeProfileRepository resumeProfileRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final AiOrchestrationService aiOrchestrationService;
    private final PromptBuilderService promptBuilderService;
    private final LoggingService loggingService;
    private final ResumeContentService resumeContentService;
    private final ContentValidationService contentValidationService;
    private final ObjectMapper objectMapper;

    @Value("${ai.temperature:0.3}")
    private double defaultTemperature;

    @Value("${ai.max-tokens:8192}")
    private int defaultMaxTokens;

    @Value("${ai.timeout-seconds:90}")
    private int defaultTimeoutSeconds;

    public GenerationService(
            GeneratedResumeRepository generatedResumeRepository,
            AnalysisReportRepository analysisReportRepository,
            AiRunRepository aiRunRepository,
            ResumeProfileRepository resumeProfileRepository,
            JobApplicationRepository jobApplicationRepository,
            UserRepository userRepository,
            AiOrchestrationService aiOrchestrationService,
            PromptBuilderService promptBuilderService,
            LoggingService loggingService,
            ResumeContentService resumeContentService,
            ContentValidationService contentValidationService,
            ObjectMapper objectMapper) {
        this.generatedResumeRepository = generatedResumeRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.aiRunRepository = aiRunRepository;
        this.resumeProfileRepository = resumeProfileRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
        this.aiOrchestrationService = aiOrchestrationService;
        this.promptBuilderService = promptBuilderService;
        this.loggingService = loggingService;
        this.resumeContentService = resumeContentService;
        this.contentValidationService = contentValidationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GenerationResponse generate(UUID userId, GenerationRequest request) {
        //1. Load and validate resume profile
        ResumeProfile resumeProfile = resumeProfileRepository.findById(request.getResumeProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Curriculo base nao encontrado"));
        if (!resumeProfile.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para usar este curriculo");
        }

        // 2. Load and validate job application
        JobApplication jobApplication = jobApplicationRepository.findById(request.getJobApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Vaga nao encontrada"));
        if (!jobApplication.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para usar esta vaga");
        }

        // 3. Process and validate resume content
        ResumeContentService.ResumeContentResult contentResult = resumeContentService.processResumeContent(
                resumeProfile.getContentJsonb(),
                resumeProfile.getContentMarkdown()
        );

        if (!contentResult.valid()) {
            log.error("Invalid resume content for generation. Source: {}, Error: {}",
                    contentResult.sourceType(), contentResult.validationError());
            throw new ValidationException("Conteudo do curriculo invalido: " + contentResult.validationError());
        }

        log.info("Resume content validated. Format: {}, Source: {}, Sections: {}, SectionCounts: {}",
                contentResult.format(), contentResult.sourceType(),
                contentResult.sectionCount(), contentResult.sectionCounts());

        // 4. Build prompts with validated content
        String language = detectLanguage(jobApplication.getJobDescription());
        String toneGuidance = "professional";
        PromptBuilderService.PromptPair prompts = promptBuilderService.buildUserPrompt(
                contentResult.processedContent(),
                contentResult.format(),
                jobApplication.getJobTitle(),
                jobApplication.getJobDescription(),
                language,
                toneGuidance,
                request.getExtraInstructions()
        );

        // 5. Create AI run record
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        AiRun aiRun = AiRun.builder()
                .user(user)
                .runType(AiRunType.RESUME_GENERATION)
                .promptVersion("v1")
                .aiModel("gemini-2.0-flash")
                .aiProvider("gemini")
                .promptText(prompts.systemPrompt() + "\n\n---\n\n" + prompts.userPrompt())
                .context(new java.util.HashMap<>())
                .status(AiRunStatus.STARTED)
                .build();
        aiRun = aiRunRepository.save(aiRun);

        // 6. Call AI provider
        GenerationOptions options = GenerationOptions.builder()
                .temperature(defaultTemperature)
                .maxTokens(defaultMaxTokens)
                .timeoutMs(defaultTimeoutSeconds * 1000)
                .retryEnabled(true)
                .build();

        GenerationResult result = aiOrchestrationService.generate(
                prompts.systemPrompt(), prompts.userPrompt(), options);

        // 7. Validate response
        if (!result.isSuccess()) {
            aiRun.setStatus(AiRunStatus.FAILED);
            aiRun.setSuccess(false);
            aiRun.setErrorCode(result.errorCode());
            aiRun.setErrorMessage(result.errorMessage());
            aiRun.setCompletedAt(OffsetDateTime.now());
            aiRunRepository.save(aiRun);
            throw new AiGenerationException("AI generation failed: " + result.errorMessage());
        }

        // 8. Parse and validate output
        Map<String, Object> parsed = result.parsedResponse();
        if (parsed == null) {
            log.error("AI response could not be parsed as JSON. Raw response (first 500 chars): {}",
                    result.rawResponse() != null ? result.rawResponse().substring(0, Math.min(500, result.rawResponse().length())) : "null");
            aiRun.setStatus(AiRunStatus.FAILED);
            aiRun.setSuccess(false);
            aiRun.setErrorCode("INVALID_OUTPUT");
            aiRun.setErrorMessage("Failed to parse AI response as JSON. Check server logs for raw response.");
            aiRun.setRawResponse(result.rawResponse());
            aiRun.setCompletedAt(OffsetDateTime.now());
            aiRunRepository.save(aiRun);
            throw new ValidationException("Resposta da IA invalida: formato inesperado. Tente novamente.", null);
        }

        log.debug("AI response parsed successfully. Keys: {}", parsed.keySet());

        // 9. Validate AI response structure and content
        String responseValidation = resumeContentService.validateAiResponse(parsed, resumeProfile.getContentJsonb());
        if (responseValidation != null) {
            log.error("AI response validation failed: {}", responseValidation);
            aiRun.setStatus(AiRunStatus.FAILED);
            aiRun.setSuccess(false);
            aiRun.setErrorCode("INVALID_RESPONSE");
            aiRun.setErrorMessage(responseValidation);
            aiRun.setRawResponse(result.rawResponse());
            aiRun.setCompletedAt(OffsetDateTime.now());
            aiRunRepository.save(aiRun);
            throw new ValidationException(responseValidation);
        }

        // 10. Call ContentValidationService to validate generated content
        ContentValidationService.ExtendedValidationResult validationResult = contentValidationService.validateGeneratedContent(
                parsed, resumeProfile.getContentJsonb(), jobApplication.getJobDescription());

        if (!validationResult.isValid()) {
            log.warn("Content validation warnings: {}", validationResult.warnings());
            // Log warnings but don't fail the generation unless critical
            if (validationResult.hasCriticalIssues()) {
                aiRun.setStatus(AiRunStatus.FAILED);
                aiRun.setSuccess(false);
                aiRun.setErrorCode("CONTENT_VALIDATION_FAILED");
                aiRun.setErrorMessage("Critical content validation issues: " + String.join(", ", validationResult.warnings()));
                aiRun.setRawResponse(result.rawResponse());
                aiRun.setCompletedAt(OffsetDateTime.now());
                aiRunRepository.save(aiRun);
                throw new ValidationException("Conteudo gerado nao atende aos requisitos: " + String.join(", ", validationResult.warnings()));
            }
        }

        // 11. Extract markdown and inject header
        String markdown = extractMarkdown(parsed);
        markdown = injectHeader(resumeProfile, markdown);

        // 12. Create generated resume with versioning
        GeneratedResume previousCurrent = generatedResumeRepository
                .findCurrentByResumeProfileIdAndJobApplicationId(
                        request.getResumeProfileId(), request.getJobApplicationId())
                .orElse(null);

        int versionNumber = (previousCurrent != null) ? previousCurrent.getVersionNumber() + 1 : 1;

        if (previousCurrent != null) {
            previousCurrent.setIsCurrent(false);
            generatedResumeRepository.save(previousCurrent);
        }

        String contentText = stripMarkdown(markdown);
        Map<String, Object> contentJsonbMap = new HashMap<>(parsed);

        int wordCount = contentText.split("\\s+").length;
        int charCount = contentText.length();

        GeneratedResume generatedResume = GeneratedResume.builder()
                .resumeProfile(resumeProfile)
                .jobApplication(jobApplication)
                .versionNumber(versionNumber)
                .parentVersion(previousCurrent)
                .isCurrent(true)
                .status("completed")
                .contentText(contentText)
                .contentMarkdown(markdown)
                .contentJsonb(contentJsonbMap)
                .aiModel(result.model())
                .aiProvider(result.provider())
                .promptVersion("v1")
                .generationReason("ai_generation")
                .wordCount(wordCount)
                .charCount(charCount)
                .build();
        generatedResume = generatedResumeRepository.save(generatedResume);

        // 13. Create analysis report with enhanced validation
        AnalysisReport analysisReport = buildAnalysisReport(generatedResume, parsed, validationResult);
        analysisReport = analysisReportRepository.save(analysisReport);

        // Update match score on generated resume
        BigDecimal matchScore = extractAdherenceScore(parsed);
        generatedResume.setMatchScore(matchScore);
        generatedResumeRepository.save(generatedResume);

        // 14. Update AI run
        aiRun.setGeneratedResume(generatedResume);
        aiRun.setStatus(AiRunStatus.SUCCEEDED);
        aiRun.setSuccess(true);
        aiRun.setInputTokens(result.inputTokens());
        aiRun.setOutputTokens(result.outputTokens());
        aiRun.setLatencyMs((int) result.durationMs());
        aiRun.setEstimatedCostUsd(BigDecimal.valueOf(result.costEstimate()));
        aiRun.setRawResponse(result.rawResponse());
        aiRun.setParsedResponse(parsed);
        aiRun.setCompletedAt(OffsetDateTime.now());
        aiRunRepository.save(aiRun);

        // 15. Log
        loggingService.logInfo(com.resumeforge.model.enums.LogCategory.AI_REQUEST,
                "AI generation completed",
                Map.of("provider", result.provider(), "model", result.model(),
                        "tokens", result.totalTokens()));

        // 16. Build response
        return buildGenerationResponse(generatedResume, analysisReport, aiRun);
    }

    @Transactional(readOnly = true)
    public Page<GeneratedResumeResponse> listGenerated(UUID userId, String companyName, String jobTitle,
                                                        UUID jobApplicationId, UUID resumeProfileId,
                                                        OffsetDateTime dateFrom, OffsetDateTime dateTo,
                                                        Boolean isCurrent, Pageable pageable) {
        Specification<GeneratedResume> spec = GeneratedResumeSpecification.withFilters(
                userId, companyName, jobTitle, jobApplicationId,
                resumeProfileId, dateFrom, dateTo, isCurrent);
        Page<GeneratedResume> page = generatedResumeRepository.findAll(spec, pageable);
        return page.map(this::toGeneratedResumeResponse);
    }

    @Transactional(readOnly = true)
    public GeneratedResumeResponse getGenerated(UUID userId, UUID generatedResumeId) {
        GeneratedResume gr = generatedResumeRepository.findByIdAndDeletedAtIsNull(generatedResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculo gerado nao encontrado"));
        if (!gr.getResumeProfile().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para acessar este curriculo");
        }
        return toGeneratedResumeResponse(gr);
    }

    @Transactional(readOnly = true)
    public AnalysisReport getAnalysis(UUID userId, UUID generatedResumeId) {
        GeneratedResume gr = generatedResumeRepository.findByIdAndDeletedAtIsNull(generatedResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculo gerado nao encontrado"));
        if (!gr.getResumeProfile().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para acessar este curriculo");
        }
        return analysisReportRepository.findByGeneratedResumeId(gr.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Analise nao encontrada"));
    }

    @Transactional
    public GenerationResponse saveManualEdit(UUID userId, UUID generatedResumeId, ManualEditRequest request) {
        GeneratedResume current = generatedResumeRepository.findByIdAndDeletedAtIsNull(generatedResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculo gerado nao encontrado"));
        if (!current.getResumeProfile().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para editar este curriculo");
        }

        current.setIsCurrent(false);
        generatedResumeRepository.save(current);

        String contentText = stripMarkdown(request.getContentMarkdown());
        String contentJsonbRaw = request.getContentJsonb() != null ? request.getContentJsonb() : "{}";
        Map<String, Object> contentJsonb = parseJsonb(contentJsonbRaw);
        int wordCount = contentText.split("\\s+").length;

        GeneratedResume newVersion = GeneratedResume.builder()
                .resumeProfile(current.getResumeProfile())
                .jobApplication(current.getJobApplication())
                .versionNumber(current.getVersionNumber() + 1)
                .parentVersion(current)
                .isCurrent(true)
                .status("completed")
                .contentText(contentText)
                .contentMarkdown(request.getContentMarkdown())
                .contentJsonb(contentJsonb)
                .aiModel(current.getAiModel())
                .aiProvider(current.getAiProvider())
                .promptVersion(current.getPromptVersion())
                .generationReason("manual_edit")
                .wordCount(wordCount)
                .charCount(contentText.length())
                .build();
        newVersion = generatedResumeRepository.save(newVersion);

        // Copy analysis report to new version
        Optional<AnalysisReport> existingAnalysis = analysisReportRepository.findByGeneratedResumeId(current.getId());
        if (existingAnalysis.isPresent()) {
            AnalysisReport newReport = AnalysisReport.builder()
                    .generatedResume(newVersion)
                    .reportType(existingAnalysis.get().getReportType())
                    .reportVersion(existingAnalysis.get().getReportVersion())
                    .overallScore(existingAnalysis.get().getOverallScore())
                    .atsCompatibilityScore(existingAnalysis.get().getAtsCompatibilityScore())
                    .dimensionScores(existingAnalysis.get().getDimensionScores())
                    .findings(existingAnalysis.get().getFindings())
                    .recommendations(existingAnalysis.get().getRecommendations())
                    .analyzedFields(existingAnalysis.get().getAnalyzedFields())
                    .build();
            analysisReportRepository.save(newReport);
        }

        return GenerationResponse.builder()
                .id(newVersion.getId())
                .resumeProfileId(newVersion.getResumeProfile().getId())
                .jobApplicationId(newVersion.getJobApplication() != null ? newVersion.getJobApplication().getId() : null)
                .versionNumber(newVersion.getVersionNumber())
                .isCurrent(true)
                .status(newVersion.getStatus())
                .contentMarkdown(newVersion.getContentMarkdown())
                .contentText(newVersion.getContentText())
                .contentJsonb(newVersion.getContentJsonb())
                .createdAt(newVersion.getCreatedAt())
                .build();
    }

    @Transactional
    public GenerationResponse regenerate(UUID userId, UUID generatedResumeId) {
        GeneratedResume original = generatedResumeRepository.findByIdAndDeletedAtIsNull(generatedResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculo gerado nao encontrado"));
        if (!original.getResumeProfile().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para regenerar este curriculo");
        }

        GenerationRequest request = GenerationRequest.builder()
                .resumeProfileId(original.getResumeProfile().getId())
                .jobApplicationId(original.getJobApplication() != null ? original.getJobApplication().getId() : null)
                .build();

        return generate(userId, request);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> listVersions(UUID userId, UUID generatedResumeId) {
        GeneratedResume gr = generatedResumeRepository.findByIdAndDeletedAtIsNull(generatedResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Curriculo gerado nao encontrado"));
        if (!gr.getResumeProfile().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para acessar este curriculo");
        }

        // Follow parent chain to get all versions
        List<VersionResponse> versions = new ArrayList<>();
        GeneratedResume current = gr;
        while (current != null) {
            versions.add(VersionResponse.builder()
                    .id(current.getId())
                    .versionNumber(current.getVersionNumber())
                    .isCurrent(current.getIsCurrent())
                    .generationReason(current.getGenerationReason())
                    .aiProvider(current.getAiProvider())
                    .aiModel(current.getAiModel())
                    .createdAt(current.getCreatedAt())
                    .build());
            current = current.getParentVersion();
        }

        // Also find any children not in the parent chain
        List<GeneratedResume> allVersions = generatedResumeRepository
                .findByResumeProfileIdAndJobApplicationIdAndDeletedAtIsNull(
                        gr.getResumeProfile().getId(),
                        gr.getJobApplication() != null ? gr.getJobApplication().getId() : null);

        Set<UUID> existingIds = versions.stream().map(VersionResponse::getId).collect(Collectors.toSet());
        for (GeneratedResume v : allVersions) {
            if (!existingIds.contains(v.getId())) {
                versions.add(VersionResponse.builder()
                        .id(v.getId())
                        .versionNumber(v.getVersionNumber())
                        .isCurrent(v.getIsCurrent())
                        .generationReason(v.getGenerationReason())
                        .aiProvider(v.getAiProvider())
                        .aiModel(v.getAiModel())
                        .createdAt(v.getCreatedAt())
                        .build());
            }
        }

        versions.sort(Comparator.comparing(VersionResponse::getVersionNumber));
        return versions;
    }

    // --- Private helpers ---

    @SuppressWarnings("unchecked")
    private String extractMarkdown(Map<String, Object> parsed) {
        try {
            Map<String, Object> optimized = (Map<String, Object>) parsed.get("optimized_resume");
            if (optimized != null && optimized.get("markdown") != null) {
                return (String) optimized.get("markdown");
            }
        } catch (Exception e) {
            log.warn("Failed to extract markdown from parsed response", e);
        }
        throw new ValidationException("Resposta da IA nao contem markdown valido", null);
    }

    private String injectHeader(ResumeProfile resumeProfile, String markdown) {
        try {
            JsonNode jsonb = objectMapper.valueToTree(resumeProfile.getContentJsonb());
            String fullName = "";
            String email = "";
            String phone = "";
            String location = "";
            String linkedin = "";
            String github = "";

            // Try new schema: profile.contacts
            if (jsonb.has("profile") && jsonb.get("profile").isObject()) {
                JsonNode profile = jsonb.get("profile");
                fullName = getNodeText(profile, "name");
                location = getNodeText(profile, "location");

                if (profile.has("contacts") && profile.get("contacts").isObject()) {
                    JsonNode contacts = profile.get("contacts");
                    email = getNodeText(contacts, "email");
                    phone = getNodeText(contacts, "phone");
                    linkedin = getNodeText(contacts, "linkedin");
                    github = getNodeText(contacts, "github");
                }
            }

            // Fallback to legacy schema: personalInfo
            if (fullName.isEmpty() && jsonb.has("personalInfo") && jsonb.get("personalInfo").isObject()) {
                JsonNode pi = jsonb.get("personalInfo");
                fullName = getNodeText(pi, "fullName");
                email = getNodeText(pi, "email");
                phone = getNodeText(pi, "phone");
                location = getNodeText(pi, "location");
                linkedin = getNodeText(pi, "linkedin");
                github = getNodeText(pi, "github");
            }

            StringBuilder header = new StringBuilder();
            header.append("# ").append(fullName).append("\n\n");
            if (!phone.isEmpty()) header.append(phone).append(" | ");
            if (!email.isEmpty()) header.append(email).append(" | ");
            if (!location.isEmpty()) header.append(location);
            header.append("\n");
            if (!linkedin.isEmpty()) header.append(linkedin).append(" | ");
            if (!github.isEmpty()) header.append(github);
            header.append("\n\n");

            String headerStr = header.toString().trim() + "\n\n";

            if (markdown.contains("{HEADER}")) {
                return markdown.replace("{HEADER}", headerStr);
            } else {
                return headerStr + markdown;
            }
        } catch (Exception e) {
            log.warn("Failed to inject header, returning original markdown", e);
            return markdown;
        }
    }

    private String getNodeText(JsonNode node, String field) {
        if (node == null || !node.has(field)) return "";
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return "";
        String text = value.asText();
        return (text != null && !text.isBlank()) ? text : "";
    }

    /**
     * Enhanced analysis report builder with full validation and detailed requirements tracking.
     */
    @SuppressWarnings("unchecked")
    private AnalysisReport buildAnalysisReport(GeneratedResume generatedResume, Map<String, Object> parsed,
                                                 ContentValidationService.ValidationResult validationResult) {
        log.debug("Building enhanced analysis report from parsed response. Keys: {}", parsed.keySet());

        // Try to get adherence_analysis, handling case where it might be a String (JSON string)
        Map<String, Object> adherence = extractMapField(parsed, "adherence_analysis");
        BigDecimal overallScore = BigDecimal.ZERO;
        List<Map<String, Object>> findingsList = new ArrayList<>();
        List<Map<String, Object>> recommendationsList = new ArrayList<>();
        List<Map<String, Object>> requirementsMatched = new ArrayList<>();
        List<Map<String, Object>> requirementsPartial = new ArrayList<>();
        List<Map<String, Object>> requirementsMissing = new ArrayList<>();
        List<Map<String, Object>> gaps = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Add validation warnings if any
        if (validationResult != null && !validationResult.warnings().isEmpty()) {
            warnings.addAll(validationResult.warnings());
        }

        if (adherence != null) {
            log.debug("adherence_analysis parsed successfully. Keys: {}", adherence.keySet());

            // Validate adherence_analysis structure
            List<String> validationErrors = validateAdherenceAnalysis(adherence);
            if (!validationErrors.isEmpty()) {
                warnings.addAll(validationErrors);
                log.warn("Adherence analysis validation issues: {}", validationErrors);
            }

            // Extract and validate score
            Object score = adherence.get("score");
            if (score instanceof Number) {
                double scoreValue = ((Number) score).doubleValue();
                // Ensure score is within valid range (0-100)
                if (scoreValue < 0) {
                    scoreValue = 0;
                    warnings.add("Score negativo corrigido para 0");
                } else if (scoreValue > 100) {
                    scoreValue = 100;
                    warnings.add("Score maior que 100 corrigido para 100");
                }
                overallScore = BigDecimal.valueOf(scoreValue);
            } else {
                warnings.add("Score nao encontrado ou invalido na analise de aderencia");
            }

            // Extract matched_requirements
            Object matchedReqsObj = adherence.get("matched_requirements");
            if (matchedReqsObj instanceof List) {
                List<?> matchedReqs = (List<?>) matchedReqsObj;
                for (Object req : matchedReqs) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        requirementsMatched.add(reqMap);
                        findingsList.add(reqMap);
                        // Extract keywords from matched requirements
                        extractKeywords(reqMap, keywords);
                    }
                }
            }

            // Extract partial_requirements (if present)
            Object partialReqsObj = adherence.get("partial_requirements");
            if (partialReqsObj instanceof List) {
                List<?> partialReqs = (List<?>) partialReqsObj;
                for (Object req : partialReqs) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        requirementsPartial.add(reqMap);
                        recommendationsList.add(reqMap);
                        // Add gap with partial severity
                        addGap(reqMap, "partial", gaps);
                        extractKeywords(reqMap, keywords);
                    }
                }
            }

            // Extract unmatched_requirements (treated as missing)
            Object unmatchedReqsObj = adherence.get("unmatched_requirements");
            if (unmatchedReqsObj instanceof List) {
                List<?> unmatchedReqs = (List<?>) unmatchedReqsObj;
                for (Object req : unmatchedReqs) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        requirementsMissing.add(reqMap);
                        recommendationsList.add(reqMap);
                        // Add gap with missing severity
                        addGap(reqMap, "missing", gaps);
                        extractKeywords(reqMap, keywords);
                    }
                }
            }

            // Also check for requirements by priority if available
            extractRequirementsByPriority(adherence, requirementsMatched, requirementsPartial, requirementsMissing, gaps, warnings);

        } else {
            log.warn("adherence_analysis is null or could not be parsed as Map. Raw parsed keys: {}",
                    parsed.keySet());
            warnings.add("adherence_analysis nao encontrado na resposta da IA");
        }

        // Extract keywords from optimized_resume if not already populated
        if (keywords.isEmpty()) {
            extractKeywordsFromResume(parsed, keywords);
        }

        // Build dimension scores with new classification
        Map<String, Object> dimensionScores = new HashMap<>();
        dimensionScores.put("adherence", overallScore);
        dimensionScores.put("adherenceClassification", classifyScore(overallScore));

        // Build analyzed fields with complete requirements tracking
        Map<String, Object> analyzedFields = new HashMap<>();
        analyzedFields.put("matched", requirementsMatched.stream()
                .map(r -> r.getOrDefault("requirement", r.toString()))
                .collect(Collectors.toList()));
        analyzedFields.put("partial", requirementsPartial.stream()
                .map(r -> r.getOrDefault("requirement", r.toString()))
                .collect(Collectors.toList()));
        analyzedFields.put("missing", requirementsMissing.stream()
                .map(r -> r.getOrDefault("requirement", r.toString()))
                .collect(Collectors.toList()));
        analyzedFields.put("keywords", keywords);
        analyzedFields.put("warnings", warnings);

        // Calculate ATS compatibility based on content analysis
        BigDecimal atsScore = calculateAtsCompatibility(parsed, keywords);

        return AnalysisReport.builder()
                .generatedResume(generatedResume)
                .reportType(com.resumeforge.model.enums.ReportType.COMBINED)
                .reportVersion("v2")
                .overallScore(overallScore)
                .atsCompatibilityScore(atsScore)
                .dimensionScores(dimensionScores)
                .findings(new HashMap<>(Map.of("items", findingsList, "keywords", keywords)))
                .recommendations(new HashMap<>(Map.of("items", recommendationsList, "gaps", gaps)))
                .analyzedFields(analyzedFields)
                .build();
    }

    /**
     * Validates adherence_analysis has all expected fields.
     */
    private List<String> validateAdherenceAnalysis(Map<String, Object> adherence) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (!adherence.containsKey("score")) {
            errors.add("Campo 'score' ausente em adherence_analysis");
        }

        // Check for at least one requirements list
        boolean hasMatched = adherence.containsKey("matched_requirements") ||
                             adherence.containsKey("requirements_matched");
        boolean hasPartial = adherence.containsKey("partial_requirements") ||
                            adherence.containsKey("requirements_partial");
        boolean hasMissing = adherence.containsKey("unmatched_requirements") ||
                            adherence.containsKey("requirements_missing");

        if (!hasMatched && !hasPartial && !hasMissing) {
            errors.add("Nenhuma lista de requirements encontrada (matched/partial/missing)");
        }

        return errors;
    }

    /**
     * Classifies score into adherence category (new classification).
     * 0-30: "Baixa aderência"
     * 31-50: "Aderência moderada"
     * 51-70: "Boa aderência"
     * 71-100: "Alta aderência"
     */
    private String classifyScore(BigDecimal score) {
        if (score == null) return "Sem classificacao";
        int scoreInt = score.intValue();

        if (scoreInt <= 30) {
            return "Baixa aderencia";
        } else if (scoreInt <= 50) {
            return "Aderencia moderada";
        } else if (scoreInt <= 70) {
            return "Boa aderencia";
        } else {
            return "Alta aderencia";
        }
    }

    /**
     * Extracts requirements by priority (obrigatorios, importantes, desejaveis).
     */
    @SuppressWarnings("unchecked")
    private void extractRequirementsByPriority(Map<String, Object> adherence,
                                               List<Map<String, Object>> requirementsMatched,
                                               List<Map<String, Object>> requirementsPartial,
                                               List<Map<String, Object>> requirementsMissing,
                                               List<Map<String, Object>> gaps,
                                               List<String> warnings) {
        // Check for priority-based requirements
        String[] priorities = {"obrigatorios", "importantes", "desejaveis", "required", "preferred", "optional"};
        for (String priority : priorities) {
            Object matchedPriority = adherence.get("matched_" + priority);
            if (matchedPriority instanceof List) {
                for (Object req : (List<?>) matchedPriority) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        reqMap.put("priority", priority);
                        requirementsMatched.add(reqMap);
                    }
                }
            }

            Object partialPriority = adherence.get("partial_" + priority);
            if (partialPriority instanceof List) {
                for (Object req : (List<?>) partialPriority) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        reqMap.put("priority", priority);
                        requirementsPartial.add(reqMap);
                        addGap(reqMap, "partial", gaps);
                    }
                }
            }

            Object missingPriority = adherence.get("missing_" + priority);
            if (missingPriority instanceof List) {
                for (Object req : (List<?>) missingPriority) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        reqMap.put("priority", priority);
                        requirementsMissing.add(reqMap);
                        addGap(reqMap, "missing", gaps);
                    }
                }
            }
        }
    }

    /**
     * Adds a gap entry with severity.
     */
    private void addGap(Map<String, Object> requirement, String severity, List<Map<String, Object>> gaps) {
        String requirementText = requirement.getOrDefault("requirement", "").toString();
        String justification = requirement.getOrDefault("justification", "").toString();

        Map<String, Object> gap = new HashMap<>();
        gap.put("requirement", requirementText);
        gap.put("severity", severity);
        gap.put("justification", justification);

        // Add priority if present
        if (requirement.containsKey("priority")) {
            gap.put("priority", requirement.get("priority"));
        }

        gaps.add(gap);
    }

    /**
     * Extracts keywords from a requirement map.
     */
    private void extractKeywords(Map<String, Object> reqMap, List<String> keywords) {
        // Extract from 'keywords' field
        Object keywordsObj = reqMap.get("keywords");
        if (keywordsObj instanceof List) {
            for (Object kw : (List<?>) keywordsObj) {
                if (kw != null && !keywords.contains(kw.toString())) {
                    keywords.add(kw.toString());
                }
            }
        }

        // Extract from 'requirement' text
        Object requirement = reqMap.get("requirement");
        if (requirement instanceof String) {
            extractKeywordsFromText((String) requirement, keywords);
        }
    }

    /**
     * Extracts keywords from text using common patterns.
     */
    private void extractKeywordsFromText(String text, List<String> keywords) {
        if (text == null || text.isBlank()) return;

        // Common technical keywords patterns
        Pattern techPattern = Pattern.compile("\\b[A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*\\b");
        Matcher matcher = techPattern.matcher(text);

        while (matcher.find()) {
            String match = matcher.group();
            if (match.length() > 2 && !keywords.contains(match)) {
                keywords.add(match);
            }
        }
    }

    /**
     * Extracts keywords from the resume content.
     */
    @SuppressWarnings("unchecked")
    private void extractKeywordsFromResume(Map<String, Object> parsed, List<String> keywords) {
        try {
            Map<String, Object> optimized = (Map<String, Object>) parsed.get("optimized_resume");
            if (optimized != null) {
                Object skills = optimized.get("skills");
                if (skills instanceof List) {
                    for (Object skill : (List<?>) skills) {
                        if (skill instanceof String && !keywords.contains(skill.toString())) {
                            keywords.add(skill.toString());
                        } else if (skill instanceof Map) {
                            Object skillName = ((Map<?, ?>) skill).get("name");
                            if (skillName != null && !keywords.contains(skillName.toString())) {
                                keywords.add(skillName.toString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract keywords from resume", e);
        }
    }

    /**
     * Calculates ATS compatibility score based on content analysis.
     */
    private BigDecimal calculateAtsCompatibility(Map<String, Object> parsed, List<String> keywords) {
        int score = 75; // Base score

        try {
            String markdown = "";
            Map<String, Object> optimized = (Map<String, Object>) parsed.get("optimized_resume");
            if (optimized != null && optimized.get("markdown") instanceof String) {
                markdown = (String) optimized.get("markdown");
            }

            // Check for common ATS-friendly elements
            if (markdown.contains("#")) score += 5; // Has headings
            if (markdown.contains("- ") || markdown.contains("* ")) score += 5; // Has bullet points
            if (keywords.size() >= 5) score += 5; // Has enough keywords
            if (markdown.length() > 500) score += 5; // Has sufficient content
            if (!markdown.contains("|")) score += 5; // No complex tables (ATS-friendly)

            // Check for potential ATS issues
            if (markdown.contains("***") || markdown.contains("___")) score -= 5; // Decorative lines
            if (markdown.contains("  \n")) score -= 5; // Multiple spaces (potential formatting issues)

        } catch (Exception e) {
            log.warn("Failed to calculate ATS compatibility", e);
        }

        return BigDecimal.valueOf(Math.min(100, Math.max(0, score)));
    }

    /**
     * Extracts a Map field from a parsed response, handling the case where the field
     * might be a JSON string instead of a Map object.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMapField(Map<String, Object> parsed, String fieldName) {
        Object field = parsed.get(fieldName);
        if (field == null) {
            return null;
        }
        if (field instanceof Map) {
            return (Map<String, Object>) field;
        }
        if (field instanceof String) {
            // Try to parse the string as JSON
            try {
                return objectMapper.readValue((String) field, Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse {} as JSON: {}", fieldName, e.getMessage());
                return null;
            }
        }
        log.warn("Field {} has unexpected type: {}", fieldName, field.getClass().getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractAdherenceScore(Map<String, Object> parsed) {
        try {
            Map<String, Object> adherence = extractMapField(parsed, "adherence_analysis");
            if (adherence != null && adherence.get("score") instanceof Number) {
                return BigDecimal.valueOf(((Number) adherence.get("score")).doubleValue());
            }
        } catch (Exception e) {
            log.warn("Failed to extract adherence score", e);
        }
        return BigDecimal.ZERO;
    }

    private String stripMarkdown(String markdown) {
        if (markdown == null) return "";
        return markdown
                .replaceAll("#{1,6}\\s+", "") // headings
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1") // bold
                .replaceAll("\\*(.+?)\\*", "$1") // italic
                .replaceAll("`(.+?)`", "$1")           // inline code
                .replaceAll("```[\\s\\S]*?```", "") // code blocks
                .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1") // links
                .replaceAll("!\\[.+?\\]\\(.+?\\)", "") // images
                .replaceAll("^\\s*[-*+]\\s+", "") // bullet points
                .replaceAll("^\\s*\\d+\\.\\s+", "")     // numbered lists
                .replaceAll("^\\s*>\\s+", "") // blockquotes
                .replaceAll("\\n{3,}", "\n\n")         // extra newlines
                .trim();
    }

    private String detectLanguage(String text) {
        if (text == null) return "PT-BR";
        String lower = text.toLowerCase();
        if (lower.contains("experience") && lower.contains("requirements")) return "EN-US";
        if (lower.contains("experiencia") || lower.contains("requisitos")) return "PT-BR";
        if (lower.contains("experiencia") && lower.contains("requisitos")) return "ES";
        return "PT-BR";
    }

    /**
     * Extracts sections from markdown content if AI didn't return structured sections.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> extractSectionsFromMarkdown(String markdown) {
        Map<String, String> sections = new LinkedHashMap<>();

        if (markdown == null || markdown.isBlank()) {
            return sections;
        }

        // Pattern to match markdown headings
        Pattern headingPattern = Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = headingPattern.matcher(markdown);

        int lastEnd = 0;
        String currentHeading = "header";
        StringBuilder currentContent = new StringBuilder();

        while (matcher.find()) {
            String heading = matcher.group(1).trim();
            int headingStart = matcher.start();

            // Save previous section content
            if (currentContent.length() > 0) {
                sections.put(currentHeading, currentContent.toString().trim());
            }

            currentHeading = heading;
            currentContent = new StringBuilder();

            // Append content between this heading and next
            if (headingStart > lastEnd) {
                String contentBetween = markdown.substring(lastEnd, headingStart);
                currentContent.append(contentBetween);
            }

            lastEnd = matcher.end();
        }

        // Save last section
        if (lastEnd < markdown.length()) {
            currentContent.append(markdown.substring(lastEnd));
        }
        if (currentContent.length() > 0) {
            sections.put(currentHeading, currentContent.toString().trim());
        }

        return sections;
    }

    private GenerationResponse buildGenerationResponse(GeneratedResume gr, AnalysisReport ar, AiRun aiRun) {
        List<String> matched = new ArrayList<>();
        List<String> partial = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<Map<String, Object>> gaps = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String summary = "";
        String adherenceClassification = "Sem classificacao";

        // Extract from analyzed fields
        try {
            Map<String, Object> analyzedFields = ar.getAnalyzedFields();
            if (analyzedFields != null) {
                Object matchedObj = analyzedFields.get("matched");
                if (matchedObj instanceof List) {
                    matched.addAll((List<String>) matchedObj);
                }
                Object partialObj = analyzedFields.get("partial");
                if (partialObj instanceof List) {
                    partial.addAll((List<String>) partialObj);
                }
                Object missingObj = analyzedFields.get("missing");
                if (missingObj instanceof List) {
                    missing.addAll((List<String>) missingObj);
                }
                Object keywordsObj = analyzedFields.get("keywords");
                if (keywordsObj instanceof List) {
                    keywords.addAll((List<String>) keywordsObj);
                }
                Object warningsObj = analyzedFields.get("warnings");
                if (warningsObj instanceof List) {
                    warnings.addAll((List<String>) warningsObj);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract analyzed fields", e);
        }

        // Extract from dimension scores
        try {
            Map<String, Object> dimensionScores = ar.getDimensionScores();
            if (dimensionScores != null) {
                Object classification = dimensionScores.get("adherenceClassification");
                if (classification != null) {
                    adherenceClassification = classification.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract dimension scores", e);
        }

        // Build summary
        int totalRequirements = matched.size() + partial.size() + missing.size();
        if (totalRequirements > 0) {
            summary = String.format("%s (%d/%d requirements cobertos)",
                    adherenceClassification, matched.size(), totalRequirements);
        }

        // Build gaps from recommendations
        try {
            Map<String, Object> recsMap = ar.getRecommendations();
            if (recsMap != null && recsMap.containsKey("gaps")) {
                Object gapsObj = recsMap.get("gaps");
                if (gapsObj instanceof List) {
                    gaps.addAll((List<Map<String, Object>>) gapsObj);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract gaps", e);
        }

        return GenerationResponse.builder()
                .id(gr.getId())
                .resumeProfileId(gr.getResumeProfile().getId())
                .jobApplicationId(gr.getJobApplication() != null ? gr.getJobApplication().getId() : null)
                .versionNumber(gr.getVersionNumber())
                .isCurrent(gr.getIsCurrent())
                .status(gr.getStatus())
                .contentMarkdown(gr.getContentMarkdown())
                .contentText(gr.getContentText())
                .contentJsonb(gr.getContentJsonb())
                .analysis(GenerationResponse.AnalysisDto.builder()
                        .id(ar.getId())
                        .adherenceScore(ar.getOverallScore() != null ? ar.getOverallScore().intValue() : 0)
                        .adherenceClassification(adherenceClassification)
                        .summary(summary)
                        .keywordMap(Map.of("matched", matched, "partial", partial, "missing", missing, "all", keywords))
                        .gaps(gaps)
                        .warnings(warnings)
                        .build())
                .aiRun(GenerationResponse.AiRunDto.builder()
                        .id(aiRun.getId())
                        .provider(aiRun.getAiProvider())
                        .model(aiRun.getAiModel())
                        .status(aiRun.getStatus().getValue())
                        .build())
                .createdAt(gr.getCreatedAt())
                .build();
    }

    private GeneratedResumeResponse toGeneratedResumeResponse(GeneratedResume gr) {
        Optional<AnalysisReport> ar = analysisReportRepository.findByGeneratedResumeId(gr.getId());

        return GeneratedResumeResponse.builder()
                .id(gr.getId())
                .resumeProfileId(gr.getResumeProfile() != null ? gr.getResumeProfile().getId() : null)
                .jobApplicationId(gr.getJobApplication() != null ? gr.getJobApplication().getId() : null)
                .companyName(gr.getJobApplication() != null ? gr.getJobApplication().getCompanyName() : null)
                .jobTitle(gr.getJobApplication() != null ? gr.getJobApplication().getJobTitle() : null)
                .versionNumber(gr.getVersionNumber())
                .isCurrent(gr.getIsCurrent())
                .status(gr.getStatus())
                .contentMarkdown(gr.getContentMarkdown())
                .contentText(gr.getContentText())
                .contentJsonb(gr.getContentJsonb())
                .adherenceScore(gr.getMatchScore() != null ? gr.getMatchScore().intValue() : null)
                .aiProvider(gr.getAiProvider())
                .aiModel(gr.getAiModel())
                .generationReason(gr.getGenerationReason())
                .wordCount(gr.getWordCount())
                .charCount(gr.getCharCount())
                .analysis(ar.map(a -> GeneratedResumeResponse.AnalysisReportDto.builder()
                        .id(a.getId())
                        .overallScore(a.getOverallScore() != null ? a.getOverallScore().intValue() : null)
                        .adherenceClassification(a.getDimensionScores() != null ?
                                a.getDimensionScores().getOrDefault("adherenceClassification", "").toString() : "")
                        .findings(a.getFindings() != null ? a.getFindings() : new java.util.HashMap<>())
                        .recommendations(a.getRecommendations() != null ? a.getRecommendations() : new java.util.HashMap<>())
                        .analyzedFields(a.getAnalyzedFields() != null ? a.getAnalyzedFields() : new java.util.HashMap<>())
                        .build()).orElse(null))
                .createdAt(gr.getCreatedAt())
                .docxGeneratedAt(gr.getDocxGeneratedAt())
                .build();
    }

    /**
     * Parses a JSON string into a Map. Returns an empty map on failure.
     */
    private Map<String, Object> parseJsonb(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
