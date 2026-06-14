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

        // 10. Extract markdown and inject header
        String markdown = extractMarkdown(parsed);
        markdown = injectHeader(resumeProfile, markdown);

        // 11. Create generated resume with versioning
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

        // 12. Create analysis report
        AnalysisReport analysisReport = buildAnalysisReport(generatedResume, parsed);
        analysisReport = analysisReportRepository.save(analysisReport);

        // Update match score on generated resume
        BigDecimal matchScore = extractAdherenceScore(parsed);
        generatedResume.setMatchScore(matchScore);
        generatedResumeRepository.save(generatedResume);

        // 13. Update AI run
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

        // 14. Log
        loggingService.logInfo(com.resumeforge.model.enums.LogCategory.AI_REQUEST,
                "AI generation completed",
                Map.of("provider", result.provider(), "model", result.model(),
                        "tokens", result.totalTokens()));

        //13. Build response
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

    @SuppressWarnings("unchecked")
    private AnalysisReport buildAnalysisReport(GeneratedResume generatedResume, Map<String, Object> parsed) {
        log.debug("Building analysis report from parsed response. Keys: {}", parsed.keySet());

        // Try to get adherence_analysis, handling case where it might be a String (JSON string)
        Map<String, Object> adherence = extractMapField(parsed, "adherence_analysis");
        BigDecimal overallScore = BigDecimal.ZERO;
        List<Map<String, Object>> findingsList = new ArrayList<>();
        List<Map<String, Object>> recommendationsList = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (adherence != null) {
            log.debug("adherence_analysis parsed successfully. Keys: {}", adherence.keySet());

            Object score = adherence.get("score");
            if (score instanceof Number) {
                overallScore = BigDecimal.valueOf(((Number) score).doubleValue());
            }

            // Try to extract matched_requirements
            Object matchedReqsObj = adherence.get("matched_requirements");
            if (matchedReqsObj instanceof List) {
                List<?> matchedReqs = (List<?>) matchedReqsObj;
                for (Object req : matchedReqs) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        Object reqStr = reqMap.get("requirement");
                        matched.add(reqStr != null ? reqStr.toString() : "");
                        findingsList.add(reqMap);
                    }
                }
            }

            // Try to extract unmatched_requirements
            Object unmatchedReqsObj = adherence.get("unmatched_requirements");
            if (unmatchedReqsObj instanceof List) {
                List<?> unmatchedReqs = (List<?>) unmatchedReqsObj;
                for (Object req : unmatchedReqs) {
                    if (req instanceof Map) {
                        Map<String, Object> reqMap = (Map<String, Object>) req;
                        Object reqStr = reqMap.get("requirement");
                        missing.add(reqStr != null ? reqStr.toString() : "");
                        recommendationsList.add(reqMap);
                    }
                }
            }
        } else {
            log.warn("adherence_analysis is null or could not be parsed as Map. Raw parsed keys: {}",
                    parsed.keySet());
        }

        Map<String, Object> dimensionScores = Map.of("adherence", overallScore);
        List<String> analyzedFieldsList = new ArrayList<>();
        analyzedFieldsList.addAll(matched);
        analyzedFieldsList.addAll(missing);

        return AnalysisReport.builder()
                .generatedResume(generatedResume)
                .reportType(com.resumeforge.model.enums.ReportType.COMBINED)
                .reportVersion("v1")
                .overallScore(overallScore)
                .atsCompatibilityScore(BigDecimal.valueOf(75))
                .dimensionScores(new java.util.HashMap<>(dimensionScores))
                .findings(new java.util.HashMap<>(Map.of("items", findingsList)))
                .recommendations(new java.util.HashMap<>(Map.of("items", recommendationsList)))
                .analyzedFields(new java.util.HashMap<>(Map.of("matched", matched, "missing", missing)))
                .build();
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

    private GenerationResponse buildGenerationResponse(GeneratedResume gr, AnalysisReport ar, AiRun aiRun) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<Map<String, Object>> gaps = new ArrayList<>();
        String summary = "";

        // Extract from findings Map
        try {
            Map<String, Object> findingsMap = ar.getFindings();
            if (findingsMap != null && findingsMap.containsKey("items")) {
                Object items = findingsMap.get("items");
                if (items instanceof List) {
                    for (Object item : (List<?>) items) {
                        if (item instanceof Map) {
                            Object req = ((Map<?, ?>) item).get("requirement");
                            if (req != null) {
                                String reqStr = req.toString();
                                matched.add(reqStr);
                                gaps.add(Map.of("requirement", reqStr, "type", "matched"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        // Extract from recommendations Map
        try {
            Map<String, Object> recsMap = ar.getRecommendations();
            if (recsMap != null && recsMap.containsKey("items")) {
                Object items = recsMap.get("items");
                if (items instanceof List) {
                    for (Object item : (List<?>) items) {
                        if (item instanceof Map) {
                            Object req = ((Map<?, ?>) item).get("requirement");
                            if (req != null) {
                                String reqStr = req.toString();
                                missing.add(reqStr);
                                gaps.add(Map.of("requirement", reqStr, "type", "missing"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
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
                        .summary(summary)
                        .keywordMap(Map.of("matched", matched, "missing", missing))
                        .gaps(gaps)
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
                        .findings(a.getFindings() != null ? a.getFindings() : new java.util.HashMap<>())
                        .recommendations(a.getRecommendations() != null ? a.getRecommendations() : new java.util.HashMap<>())
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
