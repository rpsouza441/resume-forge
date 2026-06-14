package com.resumeforge.generation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PromptBuilderService {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilderService.class);

    private final ObjectMapper objectMapper;
    private final String systemPrompt;

    public PromptBuilderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.systemPrompt = loadSystemPrompt();
    }

    private String loadSystemPrompt() {
        try {
            return new ClassPathResource("ai/system-prompt.txt")
 .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load system prompt from resource", e);
            throw new RuntimeException("Failed to load system prompt", e);
        }
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Builds the user prompt by combining formatted resume data and job description.
     * Resume is converted to readable text format for better AI comprehension.
     *
     * @param resumeContent The processed resume content (JSON string or plain text)
     * @param contentFormat "JSON" or "PLAIN_TEXT" indicating the source format
     */
    public PromptPair buildUserPrompt(String resumeContent, String contentFormat, String jobTitle,
                                      String jobDescription, String language, String toneGuidance,
                                      String extraInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Dados do Candidato (Curriculo Base)\n\n");
        sb.append("As informacoes abaixo foram fornecidas pelo candidato. Use-as EXCLUSIVAMENTE como fonte de fatos.\n");
        sb.append("Candidate input format: ").append(contentFormat).append("\n\n");
        sb.append("<candidate_resume_data>\n");
        sb.append(resumeContent);
        sb.append("\n</candidate_resume_data>\n\n");

        String requirementsList = extractRequirements(jobDescription);
        String preferencesList = extractPreferences(jobDescription);

        sb.append("## Descricao da Vaga\n\n");
        sb.append("```yaml\n");
        sb.append("Titulo da Vaga: ").append(jobTitle).append("\n");
        sb.append("Idioma da Vaga: ").append(language).append("\n");
        sb.append("Tom Preferido: ").append(toneGuidance).append("\n");
        sb.append("```\n\n");
        sb.append("```text\n");
        sb.append(jobDescription);
        sb.append("\n```\n\n");

        sb.append("## Requisitos e Preferencias Extraidos\n\n");
        sb.append("**Requisitos (obrigatorios para score alto):**\n");
        sb.append(requirementsList).append("\n\n");
        sb.append("**Preferencias (diferenciais competitivos):**\n");
        sb.append(preferencesList).append("\n\n");

        if (extraInstructions != null && !extraInstructions.isBlank()) {
            sb.append("## Instrucoes Especificas do Usuario\n\n");
            sb.append(extraInstructions).append("\n\n");
        }

        sb.append("## Instrucoes Especificas\n\n");
        sb.append("1. Gere o curriculo otimizado em `").append(language).append("`.\n");
        sb.append("2. Use o tom `").append(toneGuidance).append("`.\n");
        sb.append("3. Para vagas junior, deu peso maior a projetos academicos e competencias tecnicas basicas.\n");
        sb.append("4. Para vagas senior, destaque lideranca tecnica, arquitetura e impacto nos negocios.\n");
        sb.append("5. Remova experiencias anteriores a 10 anos que nao sejam diretamente relevantes para a vaga.\n");
        sb.append("6. Use o placeholder {HEADER} no lugar da secao de contato (nome, e-mail, telefone, LinkedIn, GitHub) — eles serao injetados localmente.\n\n");

        sb.append("## Schema de Output\n\n");
        sb.append("Gere a resposta no seguinte formato JSON. Todos os campos sao obrigatorios exceto onde indicado como opcional.\n\n");
        sb.append("```json\n");
        sb.append("""
          {
            "adherence_analysis": {
              "score": 0-100,
              "matched_requirements": [...],
              "unmatched_requirements": [...],
              "gaps": [...],
              "strengths": [...]
            },
            "optimized_resume": {
              "markdown": "string markdown completo (sem secao de contato, usar {HEADER})",
              "sections": { ... },
              "metadata": { ... }
            },
            "notes": {
              "inferences": [...],
              "warnings": [...],
              "generation_metadata": { ... }
            }
          }
          """);
        sb.append("\n```\n\n");
        sb.append("## Output\n\n");
        sb.append("Retorne APENAS o objeto JSON. Sem preambulo, sem pos-ambulo, sem markdown.\n");

        log.debug("User prompt built. Resume content length: {}", resumeContent.length());
        return new PromptPair(systemPrompt, sb.toString());
    }

    /**
     * Formats resume JSONB into readable text for better AI comprehension.
     * Removes PII and transforms structured data into human-readable format.
     */
    @SuppressWarnings("unchecked")
    private String formatResumeForPrompt(String resumeJsonb) {
        try {
            JsonNode root = objectMapper.readTree(resumeJsonb);
            StringBuilder sb = new StringBuilder();

            // Summary
            String summary = getTextValue(root, "summary");
            if (summary != null && !summary.isBlank()) {
                sb.append("**Resumo Profissional:**\n").append(summary).append("\n\n");
            }

            // Experience
            JsonNode experience = root.get("experience");
            if (experience != null && experience.isArray() && !experience.isEmpty()) {
                sb.append("**Experiencia Profissional:**\n");
                for (JsonNode exp : experience) {
                    String company = getTextValue(exp, "company");
                    String role = getTextValue(exp, "role");
                    String start = getTextValue(exp, "start");
                    String end = getTextValue(exp, "end", "presente");
                    String location = getTextValue(exp, "location");

                    String period = start != null ? start : "?";
                    if (end != null) period += " - " + end;

                    sb.append("- ").append(role != null ? role : "N/A");
                    sb.append(" @ ").append(company != null ? company : "N/A");
                    sb.append(" (").append(period).append(")");
                    if (location != null) sb.append(" — ").append(location);
                    sb.append("\n");

                    JsonNode highlights = exp.get("highlights");
                    if (highlights != null && highlights.isArray()) {
                        for (JsonNode h : highlights) {
                            String highlight = h.asText();
                            if (highlight != null && !highlight.isBlank()) {
                                sb.append("  • ").append(highlight).append("\n");
                            }
                        }
                    }
                    sb.append("\n");
                }
            }

            // Education
            JsonNode education = root.get("education");
            if (education != null && education.isArray() && !education.isEmpty()) {
                sb.append("**Formacao:**\n");
                for (JsonNode edu : education) {
                    String institution = getTextValue(edu, "institution");
                    String degree = getTextValue(edu, "degree");
                    String start = getTextValue(edu, "start");
                    String end = getTextValue(edu, "end");

                    String period = (start != null ? start : "?") + (end != null ? " - " + end : "");
                    sb.append("- ").append(degree != null ? degree : "Formacao");
                    if (institution != null) sb.append(" — ").append(institution);
                    if (!period.equals("?")) sb.append(" (").append(period).append(")");
                    sb.append("\n");
                }
                sb.append("\n");
            }

            // Skills
            JsonNode skills = root.get("skills");
            if (skills != null && skills.isObject() && !skills.isEmpty()) {
                sb.append("**Skills:**\n");
                skills.fields().forEachRemaining(entry -> {
                    String category = entry.getKey();
                    JsonNode items = entry.getValue();
                    if (items.isArray()) {
                        StringBuilder itemsSb = new StringBuilder();
                        items.forEach(item -> {
                            if (itemsSb.length() > 0) itemsSb.append(", ");
                            itemsSb.append(item.asText());
                        });
                        sb.append("- ").append(category).append(": ").append(itemsSb).append("\n");
                    }
                });
                sb.append("\n");
            }

            // Certifications/Trainings
            JsonNode trainings = root.get("trainings");
            JsonNode certifications = root.get("certifications");
            boolean hasCerts = (trainings != null && trainings.isArray() && !trainings.isEmpty()) ||
                               (certifications != null && certifications.isArray() && !certifications.isEmpty());

            if (hasCerts) {
                sb.append("**Certificacoes/Treinamentos:**\n");
                JsonNode certSource = (trainings != null && trainings.isArray() && !trainings.isEmpty())
                        ? trainings : certifications;
                int count = 0;
                for (JsonNode cert : certSource) {
                    if (count >= 10) {
                        sb.append("- ... e mais ").append(certSource.size() - 10).append(" certificacoes\n");
                        break;
                    }
                    String name = getTextValue(cert, "name");
                    String issuer = getTextValue(cert, "issuer");
                    String year = getTextValue(cert, "year");
                    if (name != null) {
                        sb.append("- ").append(name);
                        if (issuer != null) sb.append(" (").append(issuer).append(")");
                        if (year != null) sb.append(" - ").append(year);
                        sb.append("\n");
                        count++;
                    }
                }
                sb.append("\n");
            }

            // Projects (optional, only featured)
            JsonNode projects = root.get("projects");
            if (projects != null && projects.isArray() && !projects.isEmpty()) {
                sb.append("**Projetos Relevantes:**\n");
                int count = 0;
                for (JsonNode proj : projects) {
                    if (count >= 3) break;
                    boolean featured = proj.has("featured") && proj.get("featured").asBoolean();
                    if (featured || count < 1) {
                        String name = getTextValue(proj, "name");
                        String desc = getTextValue(proj, "description");
                        JsonNode techs = proj.get("technologies");
                        if (name != null) {
                            sb.append("- ").append(name);
                            if (techs != null && techs.isArray()) {
                                StringBuilder techSb = new StringBuilder();
                                int techCount = 0;
                                for (JsonNode tech : techs) {
                                    if (techCount >= 5) break;
                                    if (techSb.length() > 0) techSb.append(", ");
                                    techSb.append(tech.asText());
                                    techCount++;
                                }
                                sb.append(" (").append(techSb).append(")");
                            }
                            sb.append("\n");
                            if (desc != null && desc.length() > 100) {
                                sb.append("  ").append(desc.substring(0, 100)).append("...\n");
                            }
                            count++;
                        }
                    }
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.warn("Failed to format resume for prompt, returning raw JSON", e);
            return "```json\n" + resumeJsonb + "\n```";
        }
    }

    /**
     * Helper to safely get text value from JsonNode.
     */
    private String getTextValue(JsonNode node, String field) {
        return getTextValue(node, field, null);
    }

    private String getTextValue(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field)) return defaultValue;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return defaultValue;
        String text = value.asText();
        return (text != null && !text.isBlank()) ? text : defaultValue;
    }

    /**
     * Extracts requirements from job description using simple keyword patterns.
     */
    private String extractRequirements(String jobDescription) {
        // Simple pattern-based extraction
        Pattern[] patterns = {
            Pattern.compile("(?i)\\b(?:required|obrigat|must have|exigido|necessita)\\b[\\s:\\-]*(.{20,200})"),
            Pattern.compile("(?i)\\b\\d+\\s*(?:anos|year)[^.]*"),
            Pattern.compile("(?i)\\b(?:Python|Java|JavaScript|TypeScript|Go|Rust|SQL|Kubernetes|Docker|AWS|Azure|GCP|Terraform|CI/CD|Agile|Scrum)\\b")
        };

        StringBuilder sb = new StringBuilder();
        for (Pattern p : patterns) {
            var matcher = p.matcher(jobDescription);
            while (matcher.find()) {
                String line = matcher.groupCount() >= 1 ? matcher.group(1).trim() : matcher.group().trim();
                if (!line.isEmpty() && line.length() > 5) {
                    sb.append("- ").append(line).append("\n");
                }
            }
        }

        if (sb.length() == 0) {
            // Fallback: just take first 500 chars of description
            String excerpt = jobDescription.length() > 500 ? jobDescription.substring(0, 500) : jobDescription;
            return excerpt.lines().limit(10).collect(Collectors.joining("\n"));
        }

        return sb.toString();
    }

    /**
     * Extracts preferences/differentials from job description.
     */
    private String extractPreferences(String jobDescription) {
        Pattern[] patterns = {
            Pattern.compile("(?i)\\b(?:nice to have|preferivel|desejavel|differential|preferencia)\\b[\\s:\\-]*(.{20,200})"),
            Pattern.compile("(?i)\\b(?:plus|bonus|diferencial)\\b[\\s:\\-]*(.{10,100})")
        };

        StringBuilder sb = new StringBuilder();
        for (Pattern p : patterns) {
            var matcher = p.matcher(jobDescription);
            while (matcher.find()) {
                String line = matcher.groupCount() >= 1 ? matcher.group(1).trim() : matcher.group().trim();
                if (!line.isEmpty() && line.length() > 5) {
                    sb.append("- ").append(line).append("\n");
                }
            }
        }

        return sb.length() > 0 ? sb.toString() : "- Nenhuma preferencia adicional especificada.\n";
    }

    public record PromptPair(String systemPrompt, String userPrompt) {}
}
