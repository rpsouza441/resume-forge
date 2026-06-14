package com.resumeforge.generation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for validating, sanitizing, and formatting resume content before sending to AI.
 * Implements fail-closed security: if sanitization cannot be guaranteed, generation is blocked.
 */
@Service
public class ResumeContentService {

    private static final Logger log = LoggerFactory.getLogger(ResumeContentService.class);

    private final ObjectMapper objectMapper;

    // Placeholder patterns that indicate invalid/template content
    private static final List<String> PLACEHOLDER_PATTERNS = Arrays.asList(
            "[Nome do Curso]", "[Instituicao]", "[Idioma]", "[Nivel]",
            "[Empresa]", "[Cargo]", "[Data]", "[Ano]", "[Periodo]",
            "{{", "}}", "[PLACEHOLDER]", "[TEMPLATE]"
    );

    public ResumeContentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Result of resume content validation and processing.
     */
    public record ResumeContentResult(
            boolean valid,
            String format,           // "JSON" or "PLAIN_TEXT"
            String processedContent, // Sanitized content for prompt
            String sourceType,       // "contentJsonb" or "contentMarkdown"
            int sectionCount,        // Number of sections found
            Map<String, Integer> sectionCounts, // Count per section type
            String validationError   // null if valid, error message otherwise
    ) {}

    /**
     * Validates and processes resume content from either JSON or Markdown.
     * Implements fallback logic and fail-closed security.
     */
    public ResumeContentResult processResumeContent(Map<String, Object> contentJsonb, String contentMarkdown) {
        Map<String, Integer> sectionCounts = new HashMap<>();

        // Try JSON first
        if (contentJsonb != null && !contentJsonb.isEmpty()) {
            boolean hasProfessionalContent = checkProfessionalContent(contentJsonb, sectionCounts);

            if (hasProfessionalContent) {
                String sanitized = sanitizeJsonContent(contentJsonb);
                if (sanitized != null) {
                    log.info("Using contentJsonb as source. Sections: {}", sectionCounts);
                    return new ResumeContentResult(
                            true, "JSON", sanitized, "contentJsonb",
                            sectionCounts.values().stream().mapToInt(Integer::intValue).sum(),
                            sectionCounts, null
                    );
                } else {
                    log.warn("JSON content failed sanitization");
                    return new ResumeContentResult(
                            false, "JSON", "", "contentJsonb", 0,
                            sectionCounts, "Falha na sanitizacao do conteudo JSON"
                    );
                }
            }
        }

        // Fallback to Markdown if JSON is empty/invalid
        if (contentMarkdown != null && !contentMarkdown.isBlank()) {
            int markdownLength = contentMarkdown.length();
            sectionCounts.put("contentLength", markdownLength);

            // Check if markdown has professional content
            if (hasProfessionalMarkdown(contentMarkdown)) {
                String sanitized = sanitizeMarkdownContent(contentMarkdown);
                if (sanitized != null) {
                    log.info("Using contentMarkdown as fallback. Length: {}", markdownLength);
                    return new ResumeContentResult(
                            true, "PLAIN_TEXT", sanitized, "contentMarkdown",
                            estimateSectionsFromMarkdown(contentMarkdown),
                            sectionCounts, null
                    );
                } else {
                    log.warn("Markdown content failed sanitization");
                    return new ResumeContentResult(
                            false, "PLAIN_TEXT", "", "contentMarkdown", 0,
                            sectionCounts, "Falha na sanitizacao do conteudo Markdown"
                    );
                }
            }
        }

        // Both sources are invalid or empty
        log.error("No valid resume content available. JSON empty: {}, Markdown empty: {}",
                contentJsonb == null || contentJsonb.isEmpty(),
                contentMarkdown == null || contentMarkdown.isBlank());

        return new ResumeContentResult(
                false, "", "", "none", 0,
                sectionCounts, "Conteudo do curriculo invalido ou vazio"
        );
    }

    /**
     * Check if JSON content has professional sections.
     */
    private boolean checkProfessionalContent(Map<String, Object> contentJsonb, Map<String, Integer> sectionCounts) {
        int totalSections = 0;

        // Check summary
        if (contentJsonb.containsKey("summary") && isNonEmptyString(contentJsonb.get("summary"))) {
            sectionCounts.put("summary", 1);
            totalSections++;
        }

        // Check experience
        if (contentJsonb.containsKey("experience") && contentJsonb.get("experience") instanceof List) {
            List<?> exp = (List<?>) contentJsonb.get("experience");
            if (!exp.isEmpty()) {
                sectionCounts.put("experience", exp.size());
                totalSections += exp.size();
            }
        }

        // Check skills
        if (contentJsonb.containsKey("skills") && contentJsonb.get("skills") instanceof Map) {
            Map<?, ?> skills = (Map<?, ?>) contentJsonb.get("skills");
            if (!skills.isEmpty()) {
                sectionCounts.put("skills", skills.size());
                totalSections++;
            }
        }

        // Check education
        if (contentJsonb.containsKey("education") && contentJsonb.get("education") instanceof List) {
            List<?> edu = (List<?>) contentJsonb.get("education");
            if (!edu.isEmpty()) {
                sectionCounts.put("education", edu.size());
                totalSections += edu.size();
            }
        }

        // Check projects
        if (contentJsonb.containsKey("projects") && contentJsonb.get("projects") instanceof List) {
            List<?> proj = (List<?>) contentJsonb.get("projects");
            if (!proj.isEmpty()) {
                sectionCounts.put("projects", proj.size());
                totalSections += proj.size();
            }
        }

        // Check certifications
        if (contentJsonb.containsKey("certifications") && contentJsonb.get("certifications") instanceof List) {
            List<?> cert = (List<?>) contentJsonb.get("certifications");
            if (!cert.isEmpty()) {
                sectionCounts.put("certifications", cert.size());
                totalSections += cert.size();
            }
        }

        // Check trainings
        if (contentJsonb.containsKey("trainings") && contentJsonb.get("trainings") instanceof List) {
            List<?> train = (List<?>) contentJsonb.get("trainings");
            if (!train.isEmpty()) {
                sectionCounts.put("trainings", train.size());
                totalSections += train.size();
            }
        }

        return totalSections > 0;
    }

    /**
     * Check if markdown has professional content.
     */
    private boolean hasProfessionalMarkdown(String markdown) {
        if (markdown == null || markdown.length() < 50) return false;

        String lower = markdown.toLowerCase();
        int contentWords = lower.split("\\s+").length;

        // Must have at least 50 words to be considered professional content
        return contentWords >= 50;
    }

    /**
     * Estimate number of sections from markdown content.
     */
    private int estimateSectionsFromMarkdown(String markdown) {
        int sections = 0;
        String lower = markdown.toLowerCase();

        if (lower.contains("experiencia")) sections++;
        if (lower.contains("formacao") || lower.contains("educacao")) sections++;
        if (lower.contains("habilidade") || lower.contains("skill")) sections++;
        if (lower.contains("certificacao") || lower.contains("treinamento")) sections++;
        if (lower.contains("projeto")) sections++;
        if (lower.contains("resumo")) sections++;

        return Math.max(sections, 1);
    }

    /**
     * Sanitize JSON content by removing PII.
     * Returns null if sanitization fails.
     */
    @SuppressWarnings("unchecked")
    private String sanitizeJsonContent(Map<String, Object> contentJsonb) {
        try {
            // Deep copy to avoid modifying original
            JsonNode root = objectMapper.valueToTree(contentJsonb);
            ObjectNode sanitized = root.deepCopy();

            // Remove PII from various possible locations
            removePiiFromNode(sanitized);

            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sanitized JSON", e);
            return null;
        }
    }

    /**
     * Recursively remove PII from JSON node.
     */
    @SuppressWarnings("unchecked")
    private void removePiiFromNode(ObjectNode node) {
        // Remove from root level
        node.remove("email");
        node.remove("phone");
        node.remove("telefone");
        node.remove("linkedin");
        node.remove("github");
        node.remove("portfolio");
        node.remove("website");
        node.remove("name");
        node.remove("fullName");
        node.remove("full_name");

        // Remove from profile
        if (node.has("profile") && node.get("profile").isObject()) {
            ObjectNode profile = (ObjectNode) node.get("profile");
            profile.remove("name");
            profile.remove("fullName");
            profile.remove("full_name");
            profile.remove("email");
            profile.remove("phone");
            profile.remove("telefone");
            profile.remove("linkedin");
            profile.remove("github");
            profile.remove("portfolio");
            profile.remove("website");

            // Remove from profile.contacts
            if (profile.has("contacts") && profile.get("contacts").isObject()) {
                ObjectNode contacts = (ObjectNode) profile.get("contacts");
                contacts.remove("email");
                contacts.remove("phone");
                contacts.remove("telefone");
                contacts.remove("linkedin");
                contacts.remove("github");
                contacts.remove("portfolio");
                contacts.remove("website");
            }
        }

        // Remove from personalInfo (legacy support)
        if (node.has("personalInfo") && node.get("personalInfo").isObject()) {
            ObjectNode personalInfo = (ObjectNode) node.get("personalInfo");
            personalInfo.remove("email");
            personalInfo.remove("phone");
            personalInfo.remove("telefone");
            personalInfo.remove("linkedin");
            personalInfo.remove("github");
            personalInfo.remove("portfolio");
            personalInfo.remove("website");
            personalInfo.remove("fullName");
            personalInfo.remove("full_name");
        }

        // Recursively process nested objects and arrays
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isObject()) {
                removePiiFromNode((ObjectNode) value);
            } else if (value.isArray()) {
                for (JsonNode item : value) {
                    if (item.isObject()) {
                        removePiiFromNode((ObjectNode) item);
                    }
                }
            }
        });
    }

    /**
     * Sanitize markdown content by removing PII patterns.
     * Returns null if sanitization fails.
     */
    private String sanitizeMarkdownContent(String markdown) {
        if (markdown == null) return null;

        String sanitized = markdown;

        // Remove email patterns
        sanitized = sanitized.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[EMAIL]");

        // Remove phone patterns (various formats)
        sanitized = sanitized.replaceAll("\\(?\\d{2}\\)?[\\s.-]?\\d{4,5}[\\s.-]?\\d{4}", "[TELEFONE]");
        sanitized = sanitized.replaceAll("\\+?\\d{1,3}[\\s.-]?\\d{2,3}[\\s.-]?\\d{3,4}[\\s.-]?\\d{3,4}", "[TELEFONE]");

        // Remove LinkedIn URLs
        sanitized = sanitized.replaceAll("linkedin\\.com/in/[a-zA-Z0-9-]+", "[LINKEDIN]");
        sanitized = sanitized.replaceAll("https?://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-]+", "[LINKEDIN]");

        // Remove GitHub URLs
        sanitized = sanitized.replaceAll("github\\.com/[a-zA-Z0-9-]+", "[GITHUB]");
        sanitized = sanitized.replaceAll("https?://(www\\.)?github\\.com/[a-zA-Z0-9-]+", "[GITHUB]");

        // Remove website URLs (but keep other URLs that might be relevant)
        sanitized = sanitized.replaceAll("https?://(?!www\\.)[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}(?![a-zA-Z0-9-])", "[SITE]");

        return sanitized;
    }

    /**
     * Check if a value is a non-empty string.
     */
    private boolean isNonEmptyString(Object value) {
        return value instanceof String && !((String) value).isBlank();
    }

    /**
     * Check generated content for invalid placeholders.
     */
    public boolean hasInvalidPlaceholders(String content) {
        if (content == null || content.isBlank()) return true;

        String lower = content.toLowerCase();

        // Check for placeholder patterns
        for (String pattern : PLACEHOLDER_PATTERNS) {
            if (lower.contains(pattern.toLowerCase())) {
                return true;
            }
        }

        // Check for template-style placeholders
        if (lower.contains("[") && lower.contains("]")) {
            // Check for bracketed content that looks like a placeholder
            if (lower.matches(".*\\[\\w+\\].*")) {
                return true;
            }
        }

        // Check for empty sections
        if (lower.contains("## ") && lower.matches(".*##\\s*\\w+\\s*\\n\\s*##.*")) {
            return true;
        }

        return false;
    }

    /**
     * Validate AI response has required structure.
     */
    public String validateAiResponse(Map<String, Object> parsed, Map<String, Object> originalContent) {
        if (parsed == null) {
            return "Resposta da IA e nula";
        }

        // Check for optimized_resume
        if (!parsed.containsKey("adherence_analysis")) {
            return "Resposta da IA nao contem 'adherence_analysis'";
        }

        // Check for markdown content
        try {
            Map<String, Object> optimized = (Map<String, Object>) parsed.get("optimized_resume");
            if (optimized == null) {
                return "Resposta da IA nao contem 'optimized_resume'";
            }

            String markdown = (String) optimized.get("markdown");
            if (markdown == null || markdown.isBlank()) {
                return "Curriculo gerado esta vazio";
            }

            // Check for invalid placeholders
            if (hasInvalidPlaceholders(markdown)) {
                return "Curriculo gerado contem placeholders invalidos";
            }

            // Check for minimum content
            if (markdown.length() < 200) {
                return "Curriculo gerado e muito curto";
            }

        } catch (ClassCastException e) {
            return "Estrutura da resposta da IA e invalida";
        }

        // Check adherence score is valid
        try {
            Map<String, Object> adherence = (Map<String, Object>) parsed.get("adherence_analysis");
            if (adherence != null && adherence.containsKey("score")) {
                Object score = adherence.get("score");
                if (score instanceof Number) {
                    double scoreValue = ((Number) score).doubleValue();
                    if (scoreValue < 0 || scoreValue > 100) {
                        return "Score de aderencia invalido: " + scoreValue;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not validate adherence score", e);
        }

        return null; // Valid
    }
}
