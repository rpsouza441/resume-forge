package com.resumeforge.export.service;

import com.resumeforge.exception.ConflictException;
import com.resumeforge.exception.ForbiddenException;
import com.resumeforge.exception.ResourceNotFoundException;
import com.resumeforge.export.converter.MarkdownToDocxConverter;
import com.resumeforge.generation.entity.GeneratedResume;
import com.resumeforge.generation.repository.GeneratedResumeRepository;
import com.resumeforge.job.entity.JobApplication;
import com.resumeforge.logging.entity.ProcessingLog;
import com.resumeforge.logging.repository.ProcessingLogRepository;
import com.resumeforge.model.enums.LogCategory;
import com.resumeforge.model.enums.LogLevel;
import com.resumeforge.resume.entity.ResumeProfile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for on-demand DOCX generation from generated resumes.
 *
 * Per SPEC-08:
 * - Generates DOCX from content_markdown using Apache POI
 * - Does NOT save files to disk, database, or object storage
 * - Updates docx_generated_at timestamp after successful generation
 * - File name pattern: [Nome]-otimizado-[Empresa]-[YYYY-MM-DD].docx
 */
@Service
public class DocxGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DocxGenerationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MarkdownToDocxConverter converter;
    private final GeneratedResumeRepository generatedResumeRepository;
    private final ProcessingLogRepository processingLogRepository;

    public DocxGenerationService(
            MarkdownToDocxConverter converter,
            GeneratedResumeRepository generatedResumeRepository,
            ProcessingLogRepository processingLogRepository) {
        this.converter = converter;
        this.generatedResumeRepository = generatedResumeRepository;
        this.processingLogRepository = processingLogRepository;
    }

    /**
     * Result class holding both the DOCX bytes and the computed filename.
     */
    public record DocxResult(byte[] bytes, String filename) {}

    /**
     * Generates a DOCX file from the given generated resume.
     *
     * @param generatedResumeId the ID of the generated resume
     * @param userId            the authenticated user's ID (for ownership validation)
     * @return DocxResult containing the DOCX bytes and the filename
     * @throws ResourceNotFoundException if the resume does not exist
     * @throws ForbiddenException       if the resume does not belong to the user
     * @throws ConflictException        if the resume is not in an exportable state
     */
    @Transactional
    public DocxResult generateDocx(UUID generatedResumeId, UUID userId) {
        // 1. Load the generated resume
        GeneratedResume generatedResume = generatedResumeRepository.findByIdAndDeletedAtIsNull(generatedResumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Curriculo gerado nao encontrado: " + generatedResumeId));

        // 2. Validate ownership via resumeProfile -> user
        ResumeProfile resumeProfile = generatedResume.getResumeProfile();
        if (resumeProfile == null || !resumeProfile.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Voce nao tem permissao para exportar este curriculo.");
        }

        // 3. Validate exportable state
        if (!"completed".equals(generatedResume.getStatus())) {
            throw new ConflictException(
                    "Curriculo ainda nao esta pronto para exportacao. Status atual: " + generatedResume.getStatus());
        }

        // 4. Validate content_markdown is not empty
        String markdown = generatedResume.getContentMarkdown();
        if (markdown == null || markdown.isBlank()) {
            throw new ConflictException("Conteudo markdown esta vazio. Exportacao nao possivel.");
        }

        // 5. Load resume profile for candidate name
        String candidateName = extractCandidateName(resumeProfile, markdown);

        // 6. Load job application for company name
        String companyName = "vaga";
        JobApplication jobApplication = generatedResume.getJobApplication();
        if (jobApplication != null && jobApplication.getCompanyName() != null) {
            companyName = sanitizeFileNameComponent(jobApplication.getCompanyName());
        }

        // 7. Generate filename: [Nome]-otimizado-[Empresa]-[YYYY-MM-DD].docx
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String filename = String.format("%s-otimizado-%s-%s.docx", candidateName, companyName, dateStr);

        // 8. Convert markdown to DOCX
        XWPFDocument document;
        try {
            document = converter.convert(markdown);
        } catch (Exception e) {
            log.error("DOCX conversion failed for generatedResumeId={}", generatedResumeId, e);
            logProcessingError(generatedResumeId, e);
            throw new ConflictException("Falha na geracao do DOCX. Tente novamente.");
        }

        // 9. Write document to byte array
        byte[] docxBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            document.write(baos);
            docxBytes = baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to serialize DOCX for generatedResumeId={}", generatedResumeId, e);
            throw new ConflictException("Falha ao serializar o DOCX. Tente novamente.");
        }

        // 10. Update docx_generated_at timestamp
        generatedResume.setDocxGeneratedAt(OffsetDateTime.now());
        generatedResumeRepository.save(generatedResume);

        log.info("DOCX generated successfully: generatedResumeId={}, filename={}, size={} bytes",
                generatedResumeId, filename, docxBytes.length);

        return new DocxResult(docxBytes, filename);
    }

    /**
     * Extracts the candidate name from the resume profile or markdown content.
     * Prioritizes: resumeProfile.contentJsonb.personalInfo.fullName > markdown header
     */
    private String extractCandidateName(ResumeProfile resumeProfile, String markdown) {
        String name = extractNameFromJsonb(resumeProfile.getContentJsonb());
        if (name != null && !name.isBlank()) {
            return sanitizeFileNameComponent(name);
        }
        name = extractNameFromMarkdown(markdown);
        if (name != null && !name.isBlank()) {
            return sanitizeFileNameComponent(name);
        }
        return "Candidato";
    }

    private String extractNameFromJsonb(String jsonb) {
        if (jsonb == null || jsonb.isBlank()) return null;
        try {
            int idx = jsonb.indexOf("\"fullName\"");
            if (idx == -1) idx = jsonb.indexOf("\"full_name\"");
            if (idx == -1) return null;
            int colonIdx = jsonb.indexOf(":", idx);
            if (colonIdx == -1) return null;
            int startQuote = jsonb.indexOf("\"", colonIdx);
            if (startQuote == -1) return null;
            int endQuote = jsonb.indexOf("\"", startQuote + 1);
            if (endQuote == -1) return null;
            return jsonb.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractNameFromMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) return null;
        String[] lines = markdown.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) return trimmed.substring(2).trim();
            if (trimmed.startsWith("#")) {
                String cleaned = trimmed.replaceFirst("^#+\\s*", "");
                if (!cleaned.isBlank()) return cleaned.trim();
            }
        }
        return null;
    }

    private String sanitizeFileNameComponent(String input) {
        if (input == null || input.isBlank()) return "unknown";
        return input.trim()
                .replaceAll("[^\\p{L}\\p{N}\\s\\-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private void logProcessingError(UUID generatedResumeId, Exception e) {
        try {
            ProcessingLog processingLog = ProcessingLog.builder()
                    .logLevel(LogLevel.ERROR)
                    .category(LogCategory.EXPORT)
                    .message("DOCX generation failed: " + e.getMessage())
                    .contextData(Map.of("generatedResumeId", generatedResumeId.toString()))
                    .sourceService("DocxGenerationService")
                    .build();
            processingLogRepository.save(processingLog);
        } catch (Exception logEx) {
            log.warn("Failed to log processing error", logEx);
        }
    }
}