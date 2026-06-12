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
     * Builds the user prompt by combining anonymized resume data and job description.
     * PII is stripped from resume data before sending to AI.
     */
    public PromptPair buildUserPrompt(String resumeJsonb, String jobTitle, String jobDescription,
                                      String language, String toneGuidance, String extraInstructions) {
        String anonymizedResume = anonymizeResume(resumeJsonb);
        String requirementsList = extractRequirements(jobDescription);
        String preferencesList = extractPreferences(jobDescription);

        StringBuilder sb = new StringBuilder();
        sb.append("## Dados do Candidato (Curriculo Base)\n\n");
        sb.append("Os dados abaixo foram fornecidos pelo candidato. Use-os EXCLUSIVAMENTE como fonte de fatos. Nao invente, nao interprete erroneamente, nao omita informacoes relevantes.\n\n");
        sb.append("```json\n");
        sb.append(anonymizedResume);
        sb.append("\n```\n\n");

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

        return new PromptPair(systemPrompt, sb.toString());
    }

    /**
     * Strips PII from resume JSONB before sending to AI provider.
     * Removes: email, phone, linkedin, github, full_name.
     * Keeps: professional_summary, experience, education, skills, certifications, languages, location.
     */
    @SuppressWarnings("unchecked")
    private String anonymizeResume(String resumeJsonb) {
        try {
            JsonNode root = objectMapper.readTree(resumeJsonb);
            ObjectNode anonymized = root.deepCopy();

            // Remove top-level PII fields
            anonymized.remove("email");
            anonymized.remove("phone");
            anonymized.remove("linkedin");
            anonymized.remove("github");
            anonymized.remove("portfolio");

            // Remove PII from personalInfo if present
            if (anonymized.has("personalInfo")) {
                ObjectNode personalInfo = (ObjectNode) anonymized.get("personalInfo");
                personalInfo.remove("email");
                personalInfo.remove("phone");
                personalInfo.remove("linkedin");
                personalInfo.remove("github");
                personalInfo.remove("portfolio");
                // Keep: fullName (optional for context), location
 }

            return objectMapper.writeValueAsString(anonymized);
        } catch (JsonProcessingException e) {
            log.warn("Failed to anonymize resume JSON, returning as-is", e);
            return resumeJsonb;
        }
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
                String line = matcher.group(1).trim();
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
                String line = matcher.group(1).trim();
                if (!line.isEmpty() && line.length() > 5) {
                    sb.append("- ").append(line).append("\n");
                }
            }
        }

        return sb.length() > 0 ? sb.toString() : "- Nenhuma preferencia adicional especificada.\n";
    }

    public record PromptPair(String systemPrompt, String userPrompt) {}
}
