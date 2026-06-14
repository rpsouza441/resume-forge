package com.resumeforge.generation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to validate AI-generated resume content for inflated expressions,
 * temporal consistency, and certification accuracy.
 */
@Service
public class ContentValidationService {

    private static final Logger log = LoggerFactory.getLogger(ContentValidationService.class);

    // Patterns for inflated expressions
    private static final List<InflatedExpression> INFLATED_EXPRESSIONS = List.of(
        new InflatedExpression(
            "Especialista em",
            "warning",
            "Considere usar uma linguagem mais específica e verificável",
            Pattern.compile("(?i)especialista\\s+em", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Liderança",
            "warning",
            "Substitua por ação concreta (liderou equipe X, coordenou projeto Y)",
            Pattern.compile("(?i)\\b(liderança|liderou)\\b", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Arquiteto",
            "warning",
            "Verifique se o nível 'Arquiteto' está comprovado pelas experiências",
            Pattern.compile("(?i)\\barquiteto\\b", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Responsável integral",
            "warning",
            "Considere detalhar responsabilidades específicas",
            Pattern.compile("(?i)responsável\\s+integral", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Domínio avançado",
            "warning",
            "Substitua por nível específico (ex: '5 anos de experiência', 'certificação')",
            Pattern.compile("(?i)domínio\\s+avançado", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Sólida experiência",
            "warning",
            "Substitua por dado quantificável quando possível",
            Pattern.compile("(?i)sólida\\s+experiência", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Infraestrutura crítica",
            "warning",
            "Verifique se o termo 'crítica' está adequado ao contexto",
            Pattern.compile("(?i)infraestrutura\\s+crítica", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Administração de AWS/Azure",
            "warning",
            "Verifique se há certificações ou experiência comprovada",
            Pattern.compile("(?i)(administração|gerenciamento)\\s+de\\s+(aws|azure|gcp)", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "Em transição para",
            "warning",
            "Evite mencionar transição de carreira no currículo",
            Pattern.compile("(?i)em\\s+transição\\s+para", Pattern.CASE_INSENSITIVE)
        ),
        new InflatedExpression(
            "14+ anos",
            "warning",
            "Verifique se as experiências sustentam essa claim",
            Pattern.compile("(\\d{2,})\\+\\s*anos", Pattern.CASE_INSENSITIVE)
        )
    );

    // Pattern for extracting years of experience claims
    private static final Pattern YEARS_CLAIM_PATTERN = Pattern.compile(
        "(\\d{1,2})(?:\\+|\\s*a\\s*\\d{2})?\\s*(?:anos|years)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for certification mentions
    private static final Pattern CERTIFICATION_PATTERN = Pattern.compile(
        "(?i)(certificad[oa]|certificação|certificado)\\s+(de|em|em\\s+)?\\s*([A-Z][A-Za-z\\s]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for training/curso mentions
    private static final Pattern TRAINING_PATTERN = Pattern.compile(
        "(?i)(curso|treinamento|workshop|seminário|palestra)\\s+(de|em|sobre)?\\s*([A-Za-z\\s]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern for dates in resumes (YYYY-MM-DD, YYYY/YYYY, etc.)
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
        "(\\d{4})\\s*[-/]\\s*(\\d{4}|presente|atual|now)",
        Pattern.CASE_INSENSITIVE
    );

    // Critical patterns for validation
    private static final Set<String> CRITICAL_PATTERNS = Set.of(
            "[PLACEHOLDER]", "[TEMPLATE]", "[TODO]", "[FILL IN]",
            "{{", "}}", "[Nome", "[Empresa", "[Cargo", "[Data]"
    );

    // Minimum content thresholds
    private static final int MIN_WORD_COUNT = 100;
    private static final int MIN_SECTION_COUNT = 3;
    private static final int MIN_KEYWORD_MATCHES = 3;

    /**
     * Validates AI-generated content for inflated expressions and consistency issues.
     *
     * @param markdown The generated markdown content
     * @param originalResume The original resume data (may contain experience dates, certifications)
     * @param generatedResume The generated resume data
     * @return ValidationResult with warnings and issues
     */
    public ValidationResult validateGeneratedContent(
            String markdown,
            Map<String, Object> originalResume,
            Map<String, Object> generatedResume) {

        List<Warning> warnings = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();

        if (markdown == null || markdown.isBlank()) {
            return new ValidationResult(
                true,
                Collections.emptyList(),
                Collections.emptyList(),
                "No content to validate"
            );
        }

        // 1. Detect inflated expressions
        detectInflatedExpressions(markdown, warnings);

        // 2. Verify temporal consistency
        verifyTemporalConsistency(markdown, originalResume, generatedResume, warnings, issues);

        // 3. Verify certification separations
        verifyCertificationSeparation(markdown, originalResume, warnings);

        // 4. Check for potential fabrications
        checkForFabrications(markdown, originalResume, issues);

        // Generate summary
        String summary = generateSummary(warnings, issues);

        boolean passed = issues.isEmpty();

        return new ValidationResult(passed, warnings, issues, summary);
    }

    /**
     * Detects inflated expressions in the content.
     */
    private void detectInflatedExpressions(String content, List<Warning> warnings) {
        for (InflatedExpression expression : INFLATED_EXPRESSIONS) {
            Matcher matcher = expression.pattern.matcher(content);
            while (matcher.find()) {
                String matchedText = matcher.group();
                String context = extractContext(content, matcher.start(), matcher.end());

                warnings.add(new Warning(
                    expression.expression,
                    context,
                    expression.suggestion
                ));
            }
        }
    }

    /**
     * Verifies temporal consistency between claimed experience and actual experience.
     */
    private void verifyTemporalConsistency(
            String content,
            Map<String, Object> originalResume,
            Map<String, Object> generatedResume,
            List<Warning> warnings,
            List<Issue> issues) {

        // Extract claimed years of experience
        int claimedYears = extractClaimedYears(content);
        if (claimedYears == 0) {
            return;
        }

        // Extract actual years from experience data
        int actualYears = extractActualYears(originalResume);

        // Check if claimed years exceed actual years
        if (claimedYears > actualYears && actualYears > 0) {
            issues.add(new Issue(
                claimedYears + "+ anos",
                "error",
                "Anos reclamados (" + claimedYears + ") excedem experiência verificável (" + actualYears + " anos). " +
                "Ajuste para valor mais preciso ou remova a claim."
            ));
        } else if (claimedYears > actualYears + 2 && actualYears > 0) {
            warnings.add(new Warning(
                claimedYears + "+ anos",
                "Claimed: " + claimedYears + " anos, Verificável: " + actualYears + " anos",
                "Considere ajustar para um valor mais conservador que reflita a experiência real."
            ));
        }

        // Check for experiences that might have been removed
        if (generatedResume != null) {
            verifyExperiencesNotRemoved(originalResume, generatedResume, warnings);
        }
    }

    /**
     * Verifies that certifications are properly separated from trainings.
     */
    private void verifyCertificationSeparation(
            String content,
            Map<String, Object> originalResume,
            List<Warning> warnings) {

        // Get original certifications
        Set<String> originalCertifications = extractOriginalCertifications(originalResume);
        Set<String> originalTrainings = extractOriginalTrainings(originalResume);

        // Check if training mentions are incorrectly labeled as certifications
        Matcher trainingMatcher = TRAINING_PATTERN.matcher(content);
        while (trainingMatcher.find()) {
            String trainingName = trainingMatcher.group(3).trim();
            String fullMatch = trainingMatcher.group();

            // Check if this training appears near certification keywords
            String context = extractContext(content, trainingMatcher.start(), trainingMatcher.end());

            // If near "certificação" or "certificado", flag it
            if (context.toLowerCase().contains("certificação") ||
                context.toLowerCase().contains("certificado")) {

                boolean wasActuallyCertification = originalCertifications.stream()
                    .anyMatch(cert -> cert.toLowerCase().contains(trainingName.toLowerCase()));

                if (!wasActuallyCertification && !isKnownOfficialCertification(trainingName)) {
                    warnings.add(new Warning(
                        fullMatch,
                        context,
                        "Este curso/treinamento não deve ser apresentado como certificação oficial. " +
                        "Use 'Curso de' ou 'Treinamento em' em vez de 'Certificação'."
                    ));
                }
            }
        }
    }

    /**
     * Checks for potential fabrications by comparing with original data.
     */
    private void checkForFabrications(
            String content,
            Map<String, Object> originalResume,
            List<Issue> issues) {

        if (originalResume == null) {
            return;
        }

        // Extract original certifications
        Set<String> originalCertifications = extractOriginalCertifications(originalResume);

        // Check for certifications that weren't in the original
        Set<String> mentionedCertifications = extractMentionedCertifications(content);

        for (String mentioned : mentionedCertifications) {
            boolean found = originalCertifications.stream()
                .anyMatch(orig -> matchesCertification(mentioned, orig));

            if (!found && !isKnownOfficialCertification(mentioned)) {
                issues.add(new Issue(
                    "Certificação não verificada: " + mentioned,
                    "error",
                    "Esta certificação não foi encontrada nos dados originais. " +
                    "Verifique se é uma adição válida ou uma Fabricação."
                ));
            }
        }
    }

    /**
     * Verifies that old experiences weren't improperly removed.
     */
    private void verifyExperiencesNotRemoved(
            Map<String, Object> originalResume,
            Map<String, Object> generatedResume,
            List<Warning> warnings) {

        List<Map<String, Object>> originalExps = extractExperiences(originalResume);
        List<Map<String, Object>> generatedExps = extractExperiences(generatedResume);

        if (originalExps.isEmpty() || generatedExps.isEmpty()) {
            return;
        }

        // Calculate date ranges
        int originalOldestYear = getOldestYear(originalExps);
        int generatedOldestYear = getOldestYear(generatedExps);

        // If generated oldest year is significantly newer than original, warn
        if (originalOldestYear > 0 && generatedOldestYear > 0) {
            int yearsMissing = originalOldestYear - generatedOldestYear;
            if (yearsMissing >= 5) {
                warnings.add(new Warning(
                    "Experiências condensadas",
                    "Experiências de " + yearsMissing + " anos podem estar faltando",
                    "Experiências antigas podem ser condensadas, mas não eliminadas completamente. " +
                    "Considere manter um resumo das experiências mais antigas."
                ));
            }
        }
    }

    /**
     * Extracts claimed years of experience from content.
     */
    private int extractClaimedYears(String content) {
        Matcher matcher = YEARS_CLAIM_PATTERN.matcher(content);
        int maxYears = 0;
        while (matcher.find()) {
            String yearsStr = matcher.group(1);
            int years = Integer.parseInt(yearsStr);
            if (years > maxYears) {
                maxYears = years;
            }
        }
        return maxYears;
    }

    /**
     * Extracts actual years of experience from resume data.
     */
    private int extractActualYears(Map<String, Object> resume) {
        if (resume == null) {
            return 0;
        }

        List<Map<String, Object>> experiences = extractExperiences(resume);
        if (experiences.isEmpty()) {
            return 0;
        }

        int oldestYear = getOldestYear(experiences);
        if (oldestYear == 0) {
            return 0;
        }

        // Assume current year as reference (could be parameterized)
        int currentYear = java.time.Year.now().getValue();
        return currentYear - oldestYear;
    }

    /**
     * Extracts experience entries from resume map.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractExperiences(Map<String, Object> resume) {
        if (resume == null) {
            return Collections.emptyList();
        }

        Object experiences = resume.get("experiences");
        if (experiences instanceof List) {
            return (List<Map<String, Object>>) experiences;
        }

        Object workExperience = resume.get("workExperience");
        if (workExperience instanceof List) {
            return (List<Map<String, Object>>) workExperience;
        }

        return Collections.emptyList();
    }

    /**
     * Gets the oldest start year from experience entries.
     */
    private int getOldestYear(List<Map<String, Object>> experiences) {
        int oldestYear = 0;
        for (Map<String, Object> exp : experiences) {
            String startDate = String.valueOf(exp.getOrDefault("startDate", ""));
            Matcher matcher = DATE_RANGE_PATTERN.matcher(startDate);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                if (oldestYear == 0 || year < oldestYear) {
                    oldestYear = year;
                }
            }
        }
        return oldestYear;
    }

    /**
     * Extracts certifications from original resume.
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractOriginalCertifications(Map<String, Object> resume) {
        Set<String> certs = new HashSet<>();
        if (resume == null) {
            return certs;
        }

        Object certifications = resume.get("certifications");
        if (certifications instanceof List) {
            for (Object cert : (List<?>) certifications) {
                if (cert instanceof String) {
                    certs.add((String) cert);
                } else if (cert instanceof Map) {
                    Map<?, ?> certMap = (Map<?, ?>) cert;
                    Object name = certMap.get("name");
                    if (name != null) {
                        certs.add(name.toString());
                    }
                }
            }
        }

        return certs;
    }

    /**
     * Extracts trainings/courses from original resume.
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractOriginalTrainings(Map<String, Object> resume) {
        Set<String> trainings = new HashSet<>();
        if (resume == null) {
            return trainings;
        }

        Object courses = resume.get("courses");
        if (courses instanceof List) {
            for (Object course : (List<?>) courses) {
                if (course instanceof String) {
                    trainings.add((String) course);
                } else if (course instanceof Map) {
                    Map<?, ?> courseMap = (Map<?, ?>) course;
                    Object name = courseMap.get("name");
                    if (name != null) {
                        trainings.add(name.toString());
                    }
                }
            }
        }

        Object trainingsObj = resume.get("trainings");
        if (trainingsObj instanceof List) {
            for (Object training : (List<?>) trainingsObj) {
                if (training instanceof String) {
                    trainings.add((String) training);
                } else if (training instanceof Map) {
                    Map<?, ?> trainingMap = (Map<?, ?>) training;
                    Object name = trainingMap.get("name");
                    if (name != null) {
                        trainings.add(name.toString());
                    }
                }
            }
        }

        return trainings;
    }

    /**
     * Extracts mentioned certifications from generated content.
     */
    private Set<String> extractMentionedCertifications(String content) {
        Set<String> certs = new HashSet<>();
        Matcher matcher = CERTIFICATION_PATTERN.matcher(content);
        while (matcher.find()) {
            String certName = matcher.group(3).trim();
            if (!certName.isEmpty()) {
                certs.add(certName);
            }
        }
        return certs;
    }

    /**
     * Checks if a certification name matches an original one.
     */
    private boolean matchesCertification(String mentioned, String original) {
        String mentionedLower = mentioned.toLowerCase();
        String originalLower = original.toLowerCase();

        // Direct match
        if (originalLower.contains(mentionedLower) || mentionedLower.contains(originalLower)) {
            return true;
        }

        // Check key words
        String[] mentionedWords = mentionedLower.split("\\s+");
        String[] originalWords = originalLower.split("\\s+");

        int matchCount = 0;
        for (String mw : mentionedWords) {
            if (mw.length() > 3) { // Ignore common words
                for (String ow : originalWords) {
                    if (ow.length() > 3 && ow.equals(mw)) {
                        matchCount++;
                    }
                }
            }
        }

        return matchCount >= Math.min(2, Math.min(mentionedWords.length, originalWords.length));
    }

    /**
     * Checks if a certification is a known official certification.
     */
    private boolean isKnownOfficialCertification(String certName) {
        String lowerCert = certName.toLowerCase();

        // List of common official certifications
        String[] knownCerts = {
            "aws", "azure", "gcp", "google cloud",
            "pmp", "pmi", "scrum", "agile",
            "java", "oracle", "mysql", "postgresql", "mongodb",
            "cisco", "ccna", "ccnp",
            "itil", "cobit",
            "toefl", "ielts", "gre", "gmat",
            "cpa", "cfa",
            "pmp", "prince2"
        };

        for (String known : knownCerts) {
            if (lowerCert.contains(known)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts context around a match.
     */
    private String extractContext(String content, int start, int end) {
        int contextStart = Math.max(0, start - 50);
        int contextEnd = Math.min(content.length(), end + 50);

        String context = content.substring(contextStart, contextEnd);
        context = context.replace("\n", " ").replace("\r", " ").trim();

        if (contextStart > 0) {
            context = "..." + context;
        }
        if (contextEnd < content.length()) {
            context = context + "...";
        }

        return context;
    }

    /**
     * Generates a summary of validation results.
     */
    private String generateSummary(List<Warning> warnings, List<Issue> issues) {
        if (issues.isEmpty() && warnings.isEmpty()) {
            return "Validação passou: Nenhum problema detectado.";
        }

        StringBuilder summary = new StringBuilder();

        if (!issues.isEmpty()) {
            summary.append("Problemas críticos encontrados: ")
                   .append(issues.size());
            if (!warnings.isEmpty()) {
                summary.append(". ");
            }
        }

        if (!warnings.isEmpty()) {
            summary.append("Alertas: ").append(warnings.size());
        }

        return summary.toString();
    }

    /**
     * Record for validation results.
     */
    public record ValidationResult(
        boolean passed,
        List<Warning> warnings,
        List<Issue> issues,
        String summary
    ) {}

    /**
     * Extended validation result for generation service integration.
     */
    public record ExtendedValidationResult(
        boolean valid,
        List<String> warnings,
        List<String> criticalIssues,
        Map<String, Object> metadata
    ) {
        public boolean isValid() {
            return valid;
        }

        public boolean hasCriticalIssues() {
            return criticalIssues != null && !criticalIssues.isEmpty();
        }

        public List<String> warnings() {
            return warnings != null ? warnings : Collections.emptyList();
        }
    }

    /**
     * Validates generated content against job requirements (for GenerationService integration).
     * This method extracts the markdown from parsed response and validates it.
     */
    @SuppressWarnings("unchecked")
    public ExtendedValidationResult validateGeneratedContent(Map<String, Object> parsed,
                                                               Map<String, Object> originalResume,
                                                               String jobDescription) {
        List<String> warnings = new ArrayList<>();
        List<String> criticalIssues = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();

        if (parsed == null) {
            criticalIssues.add("Parsed response is null");
            return new ExtendedValidationResult(false, warnings, criticalIssues, metadata);
        }

        // 1. Validate adherence_analysis structure
        Map<String, Object> adherence = extractMapField(parsed, "adherence_analysis");
        if (adherence == null) {
            criticalIssues.add("adherence_analysis not found in response");
            return new ExtendedValidationResult(false, warnings, criticalIssues, metadata);
        }

        // 2. Validate score range
        if (adherence.containsKey("score")) {
            Object score = adherence.get("score");
            if (score instanceof Number) {
                double scoreValue = ((Number) score).doubleValue();
                if (scoreValue < 0 || scoreValue > 100) {
                    criticalIssues.add("Score out of range (0-100): " + scoreValue);
                }
            } else {
                warnings.add("Score is not a number: " + score);
            }
        } else {
            warnings.add("Score field missing from adherence_analysis");
        }

        // 3. Validate requirements structure
        boolean hasMatched = adherence.containsKey("matched_requirements") ||
                            adherence.containsKey("requirements_matched");
        boolean hasMissing = adherence.containsKey("unmatched_requirements") ||
                            adherence.containsKey("requirements_missing");

        if (!hasMatched && !hasMissing) {
            warnings.add("No requirements lists found (matched/missing)");
        }

        // 4. Validate markdown content
        String markdown = extractMarkdown(parsed);
        if (markdown == null || markdown.isBlank()) {
            criticalIssues.add("Generated markdown is empty");
        } else {
            // Check for critical patterns
            String lowerMarkdown = markdown.toLowerCase();
            for (String pattern : CRITICAL_PATTERNS) {
                if (lowerMarkdown.contains(pattern.toLowerCase())) {
                    criticalIssues.add("Critical placeholder found: " + pattern);
                }
            }

            // Check minimum word count
            int wordCount = markdown.split("\\s+").length;
            metadata.put("wordCount", wordCount);
            if (wordCount < MIN_WORD_COUNT) {
                warnings.add("Generated content is short (" + wordCount + " words, min: " + MIN_WORD_COUNT + ")");
            }

            // Check section count
            int sectionCount = countMarkdownSections(markdown);
            metadata.put("sectionCount", sectionCount);
            if (sectionCount < MIN_SECTION_COUNT) {
                warnings.add("Few sections detected (" + sectionCount + ", min: " + MIN_SECTION_COUNT + ")");
            }
        }

        // 5. Validate keyword matching
        List<String> keywords = extractJobKeywords(jobDescription);
        List<String> matchedKeywords = extractMatchedKeywords(markdown, keywords);
        metadata.put("matchedKeywords", matchedKeywords);
        metadata.put("requiredKeywords", keywords);

        if (keywords.size() >= 5 && matchedKeywords.size() < MIN_KEYWORD_MATCHES) {
            warnings.add("Low keyword match (" + matchedKeywords.size() + "/" + keywords.size() + " matched)");
        }

        // 6. Check for required sections
        List<String> requiredSections = List.of("experiencia", "experience", "formacao", "education",
                                                "habilidades", "skills", "resumo", "summary");
        List<String> missingSections = findMissingSections(markdown, requiredSections);
        if (!missingSections.isEmpty()) {
            warnings.add("Missing recommended sections: " + String.join(", ", missingSections));
        }

        // 7. Validate JSON structure if present
        if (parsed.containsKey("optimized_resume")) {
            Object optimized = parsed.get("optimized_resume");
            if (!(optimized instanceof Map)) {
                warnings.add("optimized_resume is not a valid object");
            }
        }

        boolean isValid = criticalIssues.isEmpty();
        return new ExtendedValidationResult(isValid, warnings, criticalIssues, metadata);
    }

    /**
     * Extracts a Map field from parsed response.
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
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                return objectMapper.readValue((String) field, Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse {} as JSON", fieldName, e);
                return null;
            }
        }
        return null;
    }

    /**
     * Extracts markdown from parsed response.
     */
    private String extractMarkdown(Map<String, Object> parsed) {
        try {
            Map<String, Object> optimized = (Map<String, Object>) parsed.get("optimized_resume");
            if (optimized != null && optimized.get("markdown") instanceof String) {
                return (String) optimized.get("markdown");
            }
        } catch (Exception e) {
            log.warn("Failed to extract markdown", e);
        }
        return null;
    }

    /**
     * Counts markdown sections (headings).
     */
    private int countMarkdownSections(String markdown) {
        if (markdown == null) return 0;
        Pattern pattern = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(markdown);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Extracts keywords from job description.
     */
    private List<String> extractJobKeywords(String jobDescription) {
        List<String> keywords = new ArrayList<>();
        if (jobDescription == null || jobDescription.isBlank()) {
            return keywords;
        }

        // Common technical skill patterns
        Pattern techPattern = Pattern.compile("\\b[A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*\\b");
        Matcher matcher = techPattern.matcher(jobDescription);

        Set<String> seen = new HashSet<>();
        while (matcher.find()) {
            String match = matcher.group().trim();
            // Filter out common words
            if (match.length() > 2 && !isCommonWord(match) && !seen.contains(match)) {
                keywords.add(match);
                seen.add(match);
                if (keywords.size() >= 20) break; // Limit keywords
            }
        }

        return keywords;
    }

    /**
     * Checks if a word is a common word that shouldn't be a keyword.
     */
    private boolean isCommonWord(String word) {
        Set<String> commonWords = Set.of(
            "The", "This", "That", "Will", "Must", "Should", "Could", "Would",
            "Experience", "Requirements", "Position", "Company", "Candidate",
            "Knowledge", "Ability", "Strong", "Excellent", "Good", "Plus"
        );
        return commonWords.contains(word);
    }

    /**
     * Extracts matched keywords from markdown.
     */
    private List<String> extractMatchedKeywords(String markdown, List<String> jobKeywords) {
        List<String> matched = new ArrayList<>();
        if (markdown == null || jobKeywords == null) {
            return matched;
        }

        String lowerMarkdown = markdown.toLowerCase();
        for (String keyword : jobKeywords) {
            if (lowerMarkdown.contains(keyword.toLowerCase())) {
                matched.add(keyword);
            }
        }

        return matched;
    }

    /**
     * Finds missing sections in markdown.
     */
    private List<String> findMissingSections(String markdown, List<String> requiredSections) {
        List<String> missing = new ArrayList<>();
        if (markdown == null) {
            missing.addAll(requiredSections);
            return missing;
        }

        String lowerMarkdown = markdown.toLowerCase();
        for (String section : requiredSections) {
            if (!lowerMarkdown.contains(section.toLowerCase())) {
                missing.add(section);
            }
        }

        return missing;
    }

    /**
     * Record for validation warnings.
     */
    public record Warning(String expression, String context, String suggestion) {}

    /**
     * Record for validation issues.
     */
    public record Issue(String expression, String severity, String action) {}

    /**
     * Internal class for inflated expression definitions.
     */
    private record InflatedExpression(
        String expression,
        String severity,
        String suggestion,
        Pattern pattern
    ) {}
}
