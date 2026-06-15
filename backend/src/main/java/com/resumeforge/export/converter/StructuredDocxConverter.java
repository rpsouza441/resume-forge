package com.resumeforge.export.converter;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.Borders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLevelText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumFmt;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * DOCX converter that uses structured data instead of Markdown.
 * Creates a professionally formatted Word document with proper styling.
 */
@Component
public class StructuredDocxConverter {

    // Use static logger to avoid creating a new logger instance for each conversion
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructuredDocxConverter.class);

    // Font settings (all in half-points for Apache POI)
    private static final String DEFAULT_FONT = "Arial";

    // Numbering for bullets
    private XWPFNumbering numbering;
    private BigInteger bulletNumId;

    // Font sizes (half-points: 21 = 10.5pt, 22 = 11pt, 24 = 12pt, 32 = 16pt)
    private static final String NAME_FONT_SIZE = "32";          // 16pt - nome em destaque
    private static final String TITLE_FONT_SIZE = "22";         // 11pt - título profissional
    private static final String CONTACT_FONT_SIZE = "18";       // 9pt - contatos
    private static final String SECTION_FONT_SIZE = "26";       // 13pt - títulos de seção (MAIOR que corpo)
    private static final String SUMMARY_FONT_SIZE = "20";       // 10pt - resumo
    private static final String EXPERIENCE_HEADER_FONT_SIZE = "22"; // 11pt - cargo/empresa (negrito)
    private static final String PERIOD_FONT_SIZE = "18";        // 9pt - período (itálico, menor)
    private static final String BODY_FONT_SIZE = "20";          // 10pt - corpo principal
    private static final String PROJECT_FONT_SIZE = "20";       // 10pt - projetos
    private static final String SKILL_CATEGORY_FONT_SIZE = "20"; // 10pt - categorias de skill
    private static final String SKILL_ITEM_FONT_SIZE = "20";    // 10pt - itens de skill
    private static final String COMPACT_FONT_SIZE = "19";       // 9.5pt - itens compactos

    // Margins in twips (1cm = 567 twips)
    // 1.5cm = 850 twips, 1.8cm = 1020 twips, 2.0cm = 1134 twips
    private static final BigInteger MARGIN_TOP = new BigInteger("1020");    // 1.8cm
    private static final BigInteger MARGIN_BOTTOM = new BigInteger("1020"); // 1.8cm
    private static final BigInteger MARGIN_LEFT = new BigInteger("1134");   // 2.0cm
    private static final BigInteger MARGIN_RIGHT = new BigInteger("1134");  // 2.0cm

    // Spacing (twips)
    private static final int LINE_SPACING = 240;              // 1.0 line spacing
    private static final int SECTION_SPACING_BEFORE = 160;    // ~8pt antes de seções
    private static final int SECTION_SPACING_AFTER = 80;      // ~4pt após seções
    private static final int BODY_SPACING_AFTER = 60;          // ~3pt após parágrafos de corpo
    private static final int BULLET_INDENT = 720;              // 0.5 inch indent for bullets

    // Records for structured data
    public record ResumeStructure(
            String professionalTitle,
            String professionalSummary,
            List<SkillCategory> skills,
            List<Experience> experience,
            List<String> previousExperienceSummary,
            List<Project> projects,
            List<Education> education,
            List<Certification> certifications,
            List<Training> trainings,
            List<Language> languages
    ) {}

    public record SkillCategory(String category, List<String> items) {}
    public record Experience(String company, String officialRole, String location, String startDate, String endDate, List<String> highlights) {}
    public record Project(String name, String description, List<String> technologies) {}
    public record Education(String institution, String degree, String period) {}
    public record Certification(String name, String issuer, String date) {}
    public record Training(String name, String issuer, String date) {}
    public record Language(String language, String level) {}

    public record ResumeHeader(
            String name,
            String title,
            String location,
            String email,
            String phone,
            String linkedin,
            String github
    ) {}

    /**
     * Creates a structured DOCX document from header and resume structure.
     *
     * @param header The resume header information
     * @param structure The resume structured data
     * @return XWPFDocument ready for export
     * @throws IllegalArgumentException if document would be empty
     */
    public XWPFDocument createDocument(ResumeHeader header, ResumeStructure structure) {
        log.info("DOCX_CONVERTER_START: header.name=[{}], structure.sections={}",
                header.name(),
                countStructureSections(structure));
        validateInput(header, structure);

        XWPFDocument document = new XWPFDocument();
        this.numbering = document.createNumbering();
        initBulletNumbering();
        setDocumentMargins(document);

        // Header section
        log.info("DOCX_CONVERTER_HEADER: creating header paragraphs for [{}]", header.name());
        createNameParagraph(document, header.name());
        createTitleParagraph(document, header.title());
        createContactParagraph(document, header);
        createSeparatorLine(document);

        // Professional Summary
        if (structure.professionalSummary() != null && !structure.professionalSummary().isEmpty()) {
            createSectionHeading(document, "Professional Summary");
            createSummaryParagraph(document, structure.professionalSummary());
        }

        // Skills
        if (structure.skills() != null && !structure.skills().isEmpty()) {
            createSectionHeading(document, "Skills");
            createSkillsSection(document, structure.skills());
        }

        // Experience
        if (structure.experience() != null && !structure.experience().isEmpty()) {
            createSectionHeading(document, "Professional Experience");
            createExperienceSection(document, structure.experience());
        }

        // Previous Experience Summary
        if (structure.previousExperienceSummary() != null && !structure.previousExperienceSummary().isEmpty()) {
            createSectionHeading(document, "Previous Experience");
            for (String summary : structure.previousExperienceSummary()) {
                if (summary != null && !summary.isEmpty()) {
                    createBulletItem(document, summary);
                }
            }
        }

        // Projects
        if (structure.projects() != null && !structure.projects().isEmpty()) {
            createSectionHeading(document, "Projects");
            createProjectsSection(document, structure.projects());
        }

        // Education
        if (structure.education() != null && !structure.education().isEmpty()) {
            createSectionHeading(document, "Education");
            createEducationSection(document, structure.education());
        }

        // Certifications
        if (structure.certifications() != null && !structure.certifications().isEmpty()) {
            createSectionHeading(document, "Certifications");
            createCertificationsSection(document, structure.certifications());
        }

        // Trainings
        if (structure.trainings() != null && !structure.trainings().isEmpty()) {
            createSectionHeading(document, "Training");
            createTrainingsSection(document, structure.trainings());
        }

        // Languages
        if (structure.languages() != null && !structure.languages().isEmpty()) {
            createSectionHeading(document, "Languages");
            createLanguagesSection(document, structure.languages());
        }

        return document;
    }

    /**
     * Validates that the document would not be empty.
     */
    private void validateInput(ResumeHeader header, ResumeStructure structure) {
        if (header == null || header.name() == null || header.name().isEmpty()) {
            throw new IllegalArgumentException("Header with name is required");
        }

        boolean hasAnySection = hasContent(structure.professionalSummary())
                || hasContent(structure.skills())
                || hasContent(structure.experience())
                || hasContent(structure.previousExperienceSummary())
                || hasContent(structure.projects())
                || hasContent(structure.education())
                || hasContent(structure.certifications())
                || hasContent(structure.trainings())
                || hasContent(structure.languages());

        if (!hasAnySection) {
            throw new IllegalArgumentException("At least one section is required in the resume structure");
        }
    }

    private boolean hasContent(Object obj) {
        if (obj == null) return false;
        if (obj instanceof String) return !((String) obj).isEmpty();
        if (obj instanceof List) return !((List<?>) obj).isEmpty();
        return true;
    }

    private int countStructureSections(ResumeStructure structure) {
        int count = 0;
        if (hasContent(structure.professionalSummary())) count++;
        if (hasContent(structure.skills())) count++;
        if (hasContent(structure.experience())) count++;
        if (hasContent(structure.previousExperienceSummary())) count++;
        if (hasContent(structure.projects())) count++;
        if (hasContent(structure.education())) count++;
        if (hasContent(structure.certifications())) count++;
        if (hasContent(structure.trainings())) count++;
        if (hasContent(structure.languages())) count++;
        return count;
    }

    /**
     * Sets document margins to 1.8cm top/bottom and 2.0cm left/right.
     */
    private void setDocumentMargins(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        // 1cm = 567 twips, 1.8cm = 1020 twips, 2.0cm = 1134 twips
        pageMar.setTop(MARGIN_TOP);       // 1020 = 1.8cm per D-04
        pageMar.setBottom(MARGIN_BOTTOM); // 1020 = 1.8cm per D-04
        pageMar.setLeft(MARGIN_LEFT);     // 1134 = 2.0cm per D-04
        pageMar.setRight(MARGIN_RIGHT);   // 1134 = 2.0cm per D-04
    }

    /**
     * Initializes OOXML bullet numbering definition.
     * Creates a bullet list style with proper indentation.
     */
    private void initBulletNumbering() {
        // Create abstract numbering for bullets
        CTAbstractNum ctAbstractNum = CTAbstractNum.Factory.newInstance();
        ctAbstractNum.setAbstractNumId(BigInteger.valueOf(1));

        // Create level 0 (first level bullets)
        CTLvl lvl = ctAbstractNum.addNewLvl();
        lvl.setIlvl(BigInteger.valueOf(0));

        // Start value for the bullet list
        lvl.addNewStart().setVal(BigInteger.ONE);

        // Level text (empty means use bullet character from font)
        CTLevelText levelText = lvl.addNewLvlText();
        levelText.setVal("");

        // Bullet format
        CTNumFmt numFmt = lvl.addNewNumFmt();
        numFmt.setVal(STNumberFormat.BULLET);

        // Left alignment
        lvl.addNewLvlJc().setVal(STJc.LEFT);

        // Indentation settings
        CTInd ind = lvl.addNewPPr().addNewInd();
        ind.setLeft(BigInteger.valueOf(BULLET_INDENT));      // 720 twips
        ind.setHanging(BigInteger.valueOf(360));              // Hanging indent

        // Add abstract num to numbering
        XWPFAbstractNum abstractNum = new XWPFAbstractNum(ctAbstractNum);
        BigInteger abstractNumId = numbering.addAbstractNum(abstractNum);
        numbering.addNum(abstractNumId);
        this.bulletNumId = BigInteger.valueOf(1);
    }

    /**
     * Creates a separator line after header.
     */
    private void createSeparatorLine(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        // Use OOXML border instead of underscore characters
        paragraph.setBorderBottom(Borders.SINGLE);
        paragraph.setSpacingBefore(80);   // ~4pt before
        paragraph.setSpacingAfter(160);    // ~8pt after
    }

    /**
     * Creates the name paragraph (16pt, bold).
     */
    private void createNameParagraph(XWPFDocument document, String name) {
        log.info("Creating name paragraph: [{}]", name);
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(name);
        run.setBold(true);
        run.setFontSize(Integer.parseInt(NAME_FONT_SIZE));
        run.setFontFamily(DEFAULT_FONT);
        paragraph.setSpacingBetween(0.0);
        setParagraphSpacing(paragraph, 0, 0);
        setLeftAlignment(paragraph);
    }

    /**
     * Creates the professional title paragraph (11pt).
     */
    private void createTitleParagraph(XWPFDocument document, String title) {
        log.info("Creating title paragraph: [{}]", title);
        if (title == null || title.isEmpty()) return;

        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setFontSize(Integer.parseInt(TITLE_FONT_SIZE));
        run.setFontFamily(DEFAULT_FONT);
        setParagraphSpacing(paragraph, 0, 60);
        setLeftAlignment(paragraph);
    }

    /**
     * Creates the contact information paragraph (9pt).
     */
    private void createContactParagraph(XWPFDocument document, ResumeHeader header) {
        log.info("Creating contact paragraph: location=[{}], email=[{}], phone=[{}], linkedin=[{}], github=[{}]",
                header.location(), header.email(), header.phone(), header.linkedin(), header.github());
        StringBuilder contactLine = new StringBuilder();

        if (header.location() != null && !header.location().isEmpty()) {
            contactLine.append(header.location());
        }
        if (header.email() != null && !header.email().isEmpty()) {
            if (contactLine.length() > 0) contactLine.append(" | ");
            contactLine.append(header.email());
        }
        if (header.phone() != null && !header.phone().isEmpty()) {
            if (contactLine.length() > 0) contactLine.append(" | ");
            contactLine.append(header.phone());
        }
        if (header.linkedin() != null && !header.linkedin().isEmpty()) {
            if (contactLine.length() > 0) contactLine.append(" | ");
            contactLine.append(header.linkedin());
        }
        if (header.github() != null && !header.github().isEmpty()) {
            if (contactLine.length() > 0) contactLine.append(" | ");
            contactLine.append(header.github());
        }

        if (contactLine.length() > 0) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(contactLine.toString());
            run.setFontSize(Integer.parseInt(CONTACT_FONT_SIZE));
            run.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(paragraph, 0, 200);
            setLeftAlignment(paragraph);
        }
    }

    /**
     * Creates a section heading paragraph (12pt, bold).
     */
    private void createSectionHeading(XWPFDocument document, String heading) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(heading);
        run.setBold(true);
        run.setFontSize(Integer.parseInt(SECTION_FONT_SIZE));
        run.setFontFamily(DEFAULT_FONT);
        setParagraphSpacing(paragraph, SECTION_SPACING_BEFORE, SECTION_SPACING_AFTER);
        setLeftAlignment(paragraph);
        setKeepWithNext(paragraph, true);
    }

    /**
     * Creates the professional summary paragraph.
     */
    private void createSummaryParagraph(XWPFDocument document, String summary) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(sanitizeMarkdown(summary));
        run.setFontSize(Integer.parseInt(SUMMARY_FONT_SIZE));
        run.setFontFamily(DEFAULT_FONT);
        setParagraphSpacing(paragraph, 0, BODY_SPACING_AFTER);
        setLeftAlignment(paragraph);
        setKeepTogether(paragraph, true);
    }

    /**
     * Creates the skills section with category headings and bullet items.
     */
    private void createSkillsSection(XWPFDocument document, List<SkillCategory> skills) {
        for (SkillCategory category : skills) {
            // Category name as bold text (sanitize markdown from category name)
            XWPFParagraph categoryPara = document.createParagraph();
            XWPFRun categoryRun = categoryPara.createRun();
            categoryRun.setBold(true);
            categoryRun.setText(sanitizeMarkdown(category.category()) + ":");
            categoryRun.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
            categoryRun.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(categoryPara, 0, 0);
            setLeftAlignment(categoryPara);

            // Skills as bullet items (already sanitized in createBulletItem)
            if (category.items() != null) {
                for (String skill : category.items()) {
                    if (skill != null && !skill.isEmpty()) {
                        createBulletItem(document, skill);
                    }
                }
            }
        }
    }

    /**
     * Creates the experience section.
     */
    private void createExperienceSection(XWPFDocument document, List<Experience> experiences) {
        for (Experience exp : experiences) {
            createExperienceEntry(document, exp);
        }
    }

    /**
     * Creates a single experience entry with proper structure:
     * - Cargo | Empresa (uma linha, negrito)
     * - Período (linha separada, itálico, menor)
     * - Highlights (bullets reais)
     */
    private void createExperienceEntry(XWPFDocument document, Experience exp) {
        // Company and Role on same line (bold, sanitize markdown from role)
        StringBuilder roleCompanyLine = new StringBuilder();
        if (exp.officialRole() != null && !exp.officialRole().isEmpty()) {
            roleCompanyLine.append(sanitizeMarkdown(exp.officialRole()));
        }
        if (exp.company() != null && !exp.company().isEmpty()) {
            if (roleCompanyLine.length() > 0) roleCompanyLine.append(" | ");
            roleCompanyLine.append(exp.company());
        }

        if (roleCompanyLine.length() > 0) {
            XWPFParagraph rolePara = document.createParagraph();
            XWPFRun roleRun = rolePara.createRun();
            roleRun.setBold(true);
            roleRun.setText(roleCompanyLine.toString());
            roleRun.setFontSize(Integer.parseInt(EXPERIENCE_HEADER_FONT_SIZE));
            roleRun.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(rolePara, SECTION_SPACING_BEFORE, 40);
            setLeftAlignment(rolePara);
            setKeepWithNext(rolePara, true);
        }

        // Location if present (inline with period)
        StringBuilder locationPeriodLine = new StringBuilder();
        if (exp.location() != null && !exp.location().isEmpty()) {
            locationPeriodLine.append(exp.location());
        }

        String period = formatPeriod(exp.startDate(), exp.endDate());
        if (period != null && !period.isEmpty()) {
            if (locationPeriodLine.length() > 0) locationPeriodLine.append(" | ");
            locationPeriodLine.append(period);
        }

        if (locationPeriodLine.length() > 0) {
            XWPFParagraph periodPara = document.createParagraph();
            XWPFRun periodRun = periodPara.createRun();
            periodRun.setItalic(true);
            periodRun.setText(locationPeriodLine.toString());
            periodRun.setFontSize(Integer.parseInt(PERIOD_FONT_SIZE));
            periodRun.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(periodPara, 0, BODY_SPACING_AFTER);
            setLeftAlignment(periodPara);
        }

        // Highlights as bullet items
        if (exp.highlights() != null) {
            for (String highlight : exp.highlights()) {
                if (highlight != null && !highlight.isEmpty()) {
                    createBulletItem(document, highlight);
                }
            }
        }
    }

    /**
     * Creates the projects section.
     */
    private void createProjectsSection(XWPFDocument document, List<Project> projects) {
        for (Project project : projects) {
            createProjectEntry(document, project);
        }
    }

    /**
     * Creates a single project entry.
     */
    private void createProjectEntry(XWPFDocument document, Project project) {
        // Project name as bold
        if (project.name() != null && !project.name().isEmpty()) {
            XWPFParagraph namePara = document.createParagraph();
            XWPFRun nameRun = namePara.createRun();
            nameRun.setBold(true);
            nameRun.setText(project.name());
            nameRun.setFontSize(Integer.parseInt(PROJECT_FONT_SIZE));
            nameRun.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(namePara, 0, 0);
            setLeftAlignment(namePara);
            setKeepTogether(namePara, true);
        }

        // Description
        if (project.description() != null && !project.description().isEmpty()) {
            XWPFParagraph descPara = document.createParagraph();
            XWPFRun descRun = descPara.createRun();
            descRun.setText(project.description());
            descRun.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
            descRun.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(descPara, 0, 0);
            setLeftAlignment(descPara);
        }

        // Technologies
        if (project.technologies() != null && !project.technologies().isEmpty()) {
            String techLine = "Technologies: " + String.join(", ", project.technologies());
            XWPFParagraph techPara = document.createParagraph();
            XWPFRun techRun = techPara.createRun();
            techRun.setText(techLine);
            techRun.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
            techRun.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(techPara, 0, 120);
            setLeftAlignment(techPara);
        }
    }

    /**
     * Creates the education section.
     */
    private void createEducationSection(XWPFDocument document, List<Education> educationList) {
        for (Education edu : educationList) {
            createEducationEntry(document, edu);
        }
    }

    /**
     * Creates a single education entry.
     */
    private void createEducationEntry(XWPFDocument document, Education edu) {
        StringBuilder line = new StringBuilder();
        if (edu.degree() != null && !edu.degree().isEmpty()) {
            line.append(edu.degree());
        }
        if (edu.institution() != null && !edu.institution().isEmpty()) {
            if (line.length() > 0) line.append(", ");
            line.append(edu.institution());
        }
        if (edu.period() != null && !edu.period().isEmpty()) {
            if (line.length() > 0) line.append(" | ");
            line.append(edu.period());
        }

        if (line.length() > 0) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(line.toString());
            run.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
            run.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(para, 0, 60);
            setLeftAlignment(para);
            setKeepTogether(para, true);
        }
    }

    /**
     * Creates the certifications section.
     */
    private void createCertificationsSection(XWPFDocument document, List<Certification> certifications) {
        for (Certification cert : certifications) {
            createCertificationEntry(document, cert);
        }
    }

    /**
     * Creates a single certification entry.
     */
    private void createCertificationEntry(XWPFDocument document, Certification cert) {
        StringBuilder line = new StringBuilder();
        if (cert.name() != null && !cert.name().isEmpty()) {
            line.append(cert.name());
        }
        if (cert.issuer() != null && !cert.issuer().isEmpty()) {
            if (line.length() > 0) line.append(" - ");
            line.append(cert.issuer());
        }
        if (cert.date() != null && !cert.date().isEmpty()) {
            if (line.length() > 0) line.append(" | ");
            line.append(cert.date());
        }

        if (line.length() > 0) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(line.toString());
            run.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
            run.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(para, 0, 60);
            setLeftAlignment(para);
            setKeepTogether(para, true);
        }
    }

    /**
     * Creates the trainings section.
     */
    private void createTrainingsSection(XWPFDocument document, List<Training> trainings) {
        for (Training training : trainings) {
            createTrainingEntry(document, training);
        }
    }

    /**
     * Creates a single training entry.
     */
    private void createTrainingEntry(XWPFDocument document, Training training) {
        StringBuilder line = new StringBuilder();
        if (training.name() != null && !training.name().isEmpty()) {
            line.append(training.name());
        }
        if (training.issuer() != null && !training.issuer().isEmpty()) {
            if (line.length() > 0) line.append(" - ");
            line.append(training.issuer());
        }
        if (training.date() != null && !training.date().isEmpty()) {
            if (line.length() > 0) line.append(" | ");
            line.append(training.date());
        }

        if (line.length() > 0) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(line.toString());
            run.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
            run.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(para, 0, 60);
            setLeftAlignment(para);
            setKeepTogether(para, true);
        }
    }

    /**
     * Creates the languages section.
     */
    private void createLanguagesSection(XWPFDocument document, List<Language> languages) {
        for (Language lang : languages) {
            createLanguageEntry(document, lang);
        }
    }

    /**
     * Creates a single language entry.
     */
    private void createLanguageEntry(XWPFDocument document, Language lang) {
        StringBuilder line = new StringBuilder();
        if (lang.language() != null && !lang.language().isEmpty()) {
            line.append(lang.language());
        }
        if (lang.level() != null && !lang.level().isEmpty()) {
            if (line.length() > 0) line.append(": ");
            else line.append(": ");
            line.append(lang.level());
        }

        if (line.length() > 0) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(line.toString());
            run.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
            run.setFontFamily(DEFAULT_FONT);
            setParagraphSpacing(para, 0, 60);
            setLeftAlignment(para);
            setKeepTogether(para, true);
        }
    }

    /**
     * Creates a bullet item paragraph with proper OOXML numbering.
     */
    private void createBulletItem(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(sanitizeMarkdown(text));
        run.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
        run.setFontFamily(DEFAULT_FONT);
        setParagraphSpacing(paragraph, 0, BODY_SPACING_AFTER);
        setLeftAlignment(paragraph);

        // Apply bullet numbering using the initialized numbering definition
        paragraph.setNumID(bulletNumId);
        paragraph.setIndentationLeft(BULLET_INDENT);
        paragraph.setIndentationHanging(360);
    }

    /**
     * Formats start and end dates into a period string.
     */
    private String formatPeriod(String startDate, String endDate) {
        if (startDate == null && endDate == null) {
            return "";
        }
        StringBuilder period = new StringBuilder();
        if (startDate != null && !startDate.isEmpty()) {
            period.append(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            if (period.length() > 0) period.append(" - ");
            period.append(endDate);
        } else {
            if (period.length() > 0) period.append(" - Present");
        }
        return period.toString();
    }

    /**
     * Sets paragraph spacing with 1.0 line spacing.
     */
    private void setParagraphSpacing(XWPFParagraph paragraph, int before, int after) {
        CTP ctp = paragraph.getCTP();
        CTPPr pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
        spacing.setBefore(BigInteger.valueOf(before));
        spacing.setAfter(BigInteger.valueOf(after));
        spacing.setLine(BigInteger.valueOf(LINE_SPACING));
        spacing.setLineRule(STLineSpacingRule.AUTO);
    }

    /**
     * Sets paragraph alignment to LEFT.
     */
    private void setLeftAlignment(XWPFParagraph paragraph) {
        CTP ctp = paragraph.getCTP();
        CTPPr pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        CTJc jc = pPr.isSetJc() ? pPr.getJc() : pPr.addNewJc();
        jc.setVal(STJc.LEFT);
    }

    /**
     * Sets keepWithNext property for section headings.
     */
    private void setKeepWithNext(XWPFParagraph paragraph, boolean keepWithNext) {
        paragraph.setKeepNext(keepWithNext);
    }

    /**
     * Sets keepTogether property for short blocks.
     */
    private void setKeepTogether(XWPFParagraph paragraph, boolean keepTogether) {
        CTP ctp = paragraph.getCTP();
        CTPPr pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        if (keepTogether) {
            // Create the keepLines element - its mere existence means "keep together"
            if (!pPr.isSetKeepLines()) {
                pPr.addNewKeepLines();
            }
        } else {
            pPr.unsetKeepLines();
        }
    }

    /**
     * Sanitizes text by removing Markdown formatting.
     * Removes **bold**, ### headers, # headers, and leading Markdown bullets.
     * Preserves asterisks that are part of technical data (e.g., C++, Bash*).
     */
    private String sanitizeMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Remove **bold** markers
        String sanitized = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        // Remove ### headers (anywhere in text)
        sanitized = sanitized.replaceAll("\\s*###\\s*", " ");
        // Remove ## headers (anywhere in text)
        sanitized = sanitized.replaceAll("\\s*##\\s*", " ");
        // Remove # headers (anywhere in text)
        sanitized = sanitized.replaceAll("\\s*#\\s*", " ");
        // Remove leading Markdown bullets (- or *) at start of line
        sanitized = sanitized.replaceAll("^[-*]\\s+", "");
        return sanitized.trim();
    }

    /**
     * Extracts ResumeStructure from a JSON Map.
     *
     * @param jsonb Map representing JSON data from database
     * @return ResumeStructure populated from the map
     */
    @SuppressWarnings("unchecked")
    public static ResumeStructure fromJson(Map<String, Object> jsonb) {
        if (jsonb == null) {
            return new ResumeStructure(null, null, null, null, null, null, null, null, null, null);
        }

        String professionalTitle = getString(jsonb, "professional_title");
        String professionalSummary = getString(jsonb, "professional_summary");

        List<SkillCategory> skills = parseSkillCategories(getList(jsonb, "skills"));
        List<Experience> experience = parseExperience(getList(jsonb, "experience"));
        List<String> previousExperienceSummary = getStringList(jsonb, "previous_experience_summary");
        List<Project> projects = parseProjects(getList(jsonb, "projects"));
        List<Education> education = parseEducation(getList(jsonb, "education"));
        List<Certification> certifications = parseCertifications(getList(jsonb, "certifications"));
        List<Training> trainings = parseTrainings(getList(jsonb, "trainings"));
        List<Language> languages = parseLanguages(getList(jsonb, "languages"));

        return new ResumeStructure(
                professionalTitle,
                professionalSummary,
                skills,
                experience,
                previousExperienceSummary,
                projects,
                education,
                certifications,
                trainings,
                languages
        );
    }

    /**
     * Extracts ResumeHeader from a JSON Map.
     *
     * @param jsonb Map representing JSON data from database
     * @return ResumeHeader populated from the map
     */
    public static ResumeHeader headerFromJson(Map<String, Object> jsonb) {
        if (jsonb == null) {
            return new ResumeHeader(null, null, null, null, null, null, null);
        }

        String name = getString(jsonb, "name");
        String title = getString(jsonb, "title");
        String location = getString(jsonb, "location");
        String email = getString(jsonb, "email");
        String phone = getString(jsonb, "phone");
        String linkedin = getString(jsonb, "linkedin");
        String github = getString(jsonb, "github");

        return new ResumeHeader(name, title, location, email, phone, linkedin, github);
    }

    // Helper methods for JSON parsing

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<SkillCategory> parseSkillCategories(List<Map<String, Object>> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item != null)
                .map(item -> {
                    String category = getString(item, "category");
                    List<String> items = getStringList(item, "items");
                    return new SkillCategory(category, items);
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Experience> parseExperience(List<Map<String, Object>> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item != null)
                .map(item -> {
                    String company = getString(item, "company");
                    String role = getString(item, "official_role");
                    String location = getString(item, "location");
                    String startDate = getString(item, "start_date");
                    String endDate = getString(item, "end_date");
                    List<String> highlights = getStringList(item, "highlights");
                    return new Experience(company, role, location, startDate, endDate, highlights);
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Project> parseProjects(List<Map<String, Object>> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item != null)
                .map(item -> {
                    String name = getString(item, "name");
                    String description = getString(item, "description");
                    List<String> technologies = getStringList(item, "technologies");
                    return new Project(name, description, technologies);
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Education> parseEducation(List<Map<String, Object>> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item != null)
                .map(item -> {
                    String institution = getString(item, "institution");
                    String degree = getString(item, "degree");
                    String period = getString(item, "period");
                    return new Education(institution, degree, period);
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Certification> parseCertifications(List<Map<String, Object>> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item != null)
                .map(item -> {
                    String name = getString(item, "name");
                    String issuer = getString(item, "issuer");
                    String date = getString(item, "date");
                    return new Certification(name, issuer, date);
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Training> parseTrainings(List<Map<String, Object>> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item != null)
                .map(item -> {
                    String name = getString(item, "name");
                    String issuer = getString(item, "issuer");
                    String date = getString(item, "date");
                    return new Training(name, issuer, date);
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Language> parseLanguages(List<Map<String, Object>> list) {
        if (list == null) return null;
        return list.stream()
                .filter(item -> item != null)
                .map(item -> {
                    String language = getString(item, "language");
                    String level = getString(item, "level");
                    return new Language(language, level);
                })
                .toList();
    }
}