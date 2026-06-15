package com.resumeforge.export.converter;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structural tests for StructuredDocxConverter.
 * Validates DOCX output without requiring Microsoft Word.
 */
class StructuredDocxConverterTest {

    private static final Pattern MARKDOWN_PATTERNS = Pattern.compile("\\*\\*|###|^#\\s|\\{HEADER\\}");

    @Test
    void testDocumentCreationSucceeds() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();
        StructuredDocxConverter.ResumeStructure structure = StructuredDocxConverterTestData.sampleStructure();

        XWPFDocument document = converter.createDocument(header, structure);

        assertNotNull(document);
        assertFalse(document.getParagraphs().isEmpty());
    }

    @Test
    void testHeaderStructure() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();
        StructuredDocxConverter.ResumeStructure structure = StructuredDocxConverterTestData.sampleStructure();

        XWPFDocument document = converter.createDocument(header, structure);
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        assertFalse(paragraphs.isEmpty(), "Document must have paragraphs");

        // First paragraph should be the name (bold, largest font)
        XWPFParagraph firstPara = paragraphs.get(0);
        assertFalse(firstPara.getRuns().isEmpty(), "First paragraph must have a run");

        XWPFRun nameRun = firstPara.getRuns().get(0);
        assertTrue(nameRun.isBold(), "Name should be bold");
        // Name font size should be NAME_FONT_SIZE = 32 (16pt)
        assertEquals(32, nameRun.getFontSize(), "Name should be 16pt (32 half-points)");
    }

    @Test
    void testMarginsApplied() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();
        StructuredDocxConverter.ResumeStructure structure = StructuredDocxConverterTestData.sampleStructure();

        XWPFDocument document = converter.createDocument(header, structure);

        CTSectPr sectPr = document.getDocument().getBody().getSectPr();
        assertNotNull(sectPr, "Document must have section properties");

        CTPageMar pageMar = sectPr.getPgMar();
        assertNotNull(pageMar, "Page margins must be set");

        // Per D-04: margins are 1.8cm top/bottom (1020 twips), 2.0cm left/right (1134 twips)
        // 1cm = 567 twips, so:
        // 1.8cm = 1020 twips, 2.0cm = 1134 twips
        assertEquals(1020, ((BigInteger) pageMar.getTop()).intValue(), "Top margin should be 1020 twips (1.8cm)");
        assertEquals(1020, ((BigInteger) pageMar.getBottom()).intValue(), "Bottom margin should be 1020 twips (1.8cm)");
        assertEquals(1134, ((BigInteger) pageMar.getLeft()).intValue(), "Left margin should be 1134 twips (2.0cm)");
        assertEquals(1134, ((BigInteger) pageMar.getRight()).intValue(), "Right margin should be 1134 twips (2.0cm)");
    }

    @Test
    void testBulletHasNumbering() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();
        StructuredDocxConverter.ResumeStructure structure = StructuredDocxConverterTestData.sampleStructure();

        XWPFDocument document = converter.createDocument(header, structure);

        // Find paragraphs with experience highlights (they should have bullet numbering)
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        boolean foundBulletParagraph = false;

        for (XWPFParagraph para : paragraphs) {
            BigInteger numId = para.getNumID();
            if (numId != null && numId.intValue() > 0) {
                foundBulletParagraph = true;
                assertTrue(numId.intValue() > 0, "Bullet paragraph must have numId > 0");
                break;
            }
        }

        assertTrue(foundBulletParagraph, "Document should contain bullet paragraphs with numbering");
    }

    @Test
    void testNoMarkdownResidual() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();

        // Create structure with markdown-like content that should be sanitized
        // ResumeStructure order: (professionalTitle, professionalSummary, skills, experience,
        //                         previousExperienceSummary, projects, education,
        //                         certifications, trainings, languages)
        StructuredDocxConverter.ResumeStructure structure = new StructuredDocxConverter.ResumeStructure(
            null, // professionalTitle
            "**Bold summary** with ### markdown", // professionalSummary
            List.of(new StructuredDocxConverter.SkillCategory("## Test ### Category",
                List.of("**Bold skill**", "- dash skill", "C++"))),
            List.of(new StructuredDocxConverter.Experience(
                "Company",
                "## Manager role",
                "Location",
                "Jan 2020",
                "Present",
                List.of("### Do **something**", "- bullet item")
            )),
            null, // previousExperienceSummary
            null, // projects
            null, // education
            null, // certifications
            null, // trainings
            null  // languages
        );

        XWPFDocument document = converter.createDocument(header, structure);

        // Check all paragraph text for markdown residual
        for (XWPFParagraph para : document.getParagraphs()) {
            String text = para.getText();
            if (text != null && !text.isEmpty()) {
                assertFalse(MARKDOWN_PATTERNS.matcher(text).find(),
                    "Paragraph should not contain markdown: " + text);
            }
        }
    }

    @Test
    void testBodyFontSizeLessThanOrEqual11pt() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();
        StructuredDocxConverter.ResumeStructure structure = StructuredDocxConverterTestData.sampleStructure();

        XWPFDocument document = converter.createDocument(header, structure);

        // Per DOCX-01 and D-02: BODY_FONT_SIZE should be 20 (10pt)
        // Check that body text font size is <= 22 half-points (11pt)
        for (XWPFParagraph para : document.getParagraphs()) {
            for (XWPFRun run : para.getRuns()) {
                Integer fontSize = run.getFontSize();
                if (fontSize != null && !run.isBold()) {
                    // Non-bold runs should be body text
                    assertTrue(fontSize <= 22,
                        "Body font size should be <= 11pt (22 half-points), got: " + fontSize);
                }
            }
        }
    }

    @Test
    void testSectionHeadingsLargerThanBody() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();
        StructuredDocxConverter.ResumeStructure structure = StructuredDocxConverterTestData.sampleStructure();

        XWPFDocument document = converter.createDocument(header, structure);

        Integer sectionHeadingSize = null;
        Integer bodySize = null;

        for (XWPFParagraph para : document.getParagraphs()) {
            for (XWPFRun run : para.getRuns()) {
                Integer fontSize = run.getFontSize();
                if (fontSize == null) continue;

                // Section headings are bold (per spec)
                if (run.isBold() && !isNameRun(run)) {
                    if (sectionHeadingSize == null || fontSize > sectionHeadingSize) {
                        sectionHeadingSize = fontSize;
                    }
                }
                // Body text is non-bold
                if (!run.isBold()) {
                    if (bodySize == null || fontSize < bodySize) {
                        bodySize = fontSize;
                    }
                }
            }
        }

        // Section headings should be larger than body
        if (sectionHeadingSize != null && bodySize != null) {
            assertTrue(sectionHeadingSize > bodySize,
                "Section headings should be larger than body. Section: " + sectionHeadingSize +
                ", Body: " + bodySize);
        }
    }

    @Test
    void testExperienceStructureSeparateHeadersAndBullets() {
        StructuredDocxConverter converter = new StructuredDocxConverter();
        StructuredDocxConverter.ResumeHeader header = StructuredDocxConverterTestData.sampleHeader();
        StructuredDocxConverter.ResumeStructure structure = StructuredDocxConverterTestData.sampleStructure();

        XWPFDocument document = converter.createDocument(header, structure);

        List<XWPFParagraph> paragraphs = document.getParagraphs();

        // Count paragraphs with bold text (headers) and paragraphs with bullets
        int boldParagraphs = 0;
        int bulletParagraphs = 0;

        for (XWPFParagraph para : paragraphs) {
            boolean hasBold = para.getRuns().stream().anyMatch(XWPFRun::isBold);
            BigInteger numId = para.getNumID();
            boolean hasBullet = numId != null && numId.intValue() > 0;

            if (hasBold) boldParagraphs++;
            if (hasBullet) bulletParagraphs++;
        }

        // Experience section should have both headers (bold) and bullets
        assertTrue(boldParagraphs > 0, "Document should have bold paragraphs (experience headers)");
        assertTrue(bulletParagraphs > 0, "Document should have bullet paragraphs (highlights)");
    }

    private boolean isNameRun(XWPFRun run) {
        // Name run has the largest font size (NAME_FONT_SIZE = 32)
        return run.isBold() && run.getFontSize() >= 32;
    }
}
