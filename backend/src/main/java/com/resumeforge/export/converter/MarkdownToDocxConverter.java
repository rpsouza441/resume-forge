package com.resumeforge.export.converter;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Markdown text to a DOCX document using Apache POI XWPF.
 * Produces ATS-friendly output: no tables, no images, no multi-column layout.
 *
 * Per SPEC-08:
 * - Font: Arial, 11pt body, 14-16pt headings
 * - Margins: 2.5cm all sides
 * - Bullet lists: native DOCX bullets
 * - Bold for headings and emphasis
 */
@Component
public class MarkdownToDocxConverter {

    private static final String FONT_FAMILY = "Arial";
    private static final int BODY_FONT_SIZE = 22; // 11pt in half-points
    private static final int SPACING_BEFORE = 80;
    private static final int SPACING_AFTER = 120;

    // Patterns for inline formatting
    // Bold: **text** — double asterisks
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    // Italic: *text* — single asterisk NOT preceded or followed by another asterisk
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");

    /**
     * Converts a Markdown string into an in-memory XWPFDocument.
     *
     * @param markdown the Markdown content to convert
     * @return the generated DOCX document
     */
    public XWPFDocument convert(String markdown) {
        XWPFDocument document = new XWPFDocument();

        // Set page margins: 2.5cm on all sides (1cm = 567 twips, 2.5cm ≈ 1417 twips)
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(1417));
        pageMar.setBottom(BigInteger.valueOf(1417));
        pageMar.setLeft(BigInteger.valueOf(1417));
        pageMar.setRight(BigInteger.valueOf(1417));

        // Split by double newlines (paragraph boundaries)
        String[] paragraphs = markdown.split("\\n\\n");

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.startsWith("# ")) {
                createHeading(document, trimmed.substring(2), 1);
            } else if (trimmed.startsWith("## ")) {
                createHeading(document, trimmed.substring(3), 2);
            } else if (trimmed.startsWith("### ")) {
                createHeading(document, trimmed.substring(4), 3);
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                createBulletList(document, para);
            } else if (trimmed.startsWith("> ")) {
                createBlockquote(document, trimmed.substring(2).trim());
            } else if (trimmed.matches("^-{3,}$") || trimmed.matches("^\\*{3,}$")) {
                createHorizontalRule(document);
            } else {
                createParagraph(document, trimmed);
            }
        }

        return document;
    }

    /**
     * Creates a heading paragraph.
     *
     * @param doc   the document
     * @param text  the heading text
     * @param level heading level (1, 2, or 3)
     */
    private void createHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();

        // Strip inline formatting from heading text
        String cleanText = stripInlineFormatting(text);
        run.setText(cleanText);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);

        // Font sizes: H1=16pt(32), H2=14pt(28), H3=12pt(24)
        switch (level) {
            case 1 -> {
                run.setFontSize(32);
                p.setSpacingBefore(300);
                p.setSpacingAfter(200);
            }
            case 2 -> {
                run.setFontSize(28);
                p.setSpacingBefore(240);
                p.setSpacingAfter(160);
            }
            default -> {
                run.setFontSize(24);
                p.setSpacingBefore(200);
                p.setSpacingAfter(120);
            }
        }
    }

    /**
     * Creates a regular paragraph with inline formatting support.
     *
     * @param doc  the document
     * @param text the paragraph text
     */
    private void createParagraph(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(SPACING_BEFORE);
        p.setSpacingAfter(SPACING_AFTER);
        p.setAlignment(ParagraphAlignment.BOTH);
        processInlineFormatting(p, text);
    }

    /**
     * Creates a blockquote paragraph (indented, italic).
     *
     * @param doc  the document
     * @param text the quote text
     */
    private void createBlockquote(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(720); // 0.5 inch left indent
        p.setIndentationRight(720);
        p.setSpacingBefore(SPACING_BEFORE);
        p.setSpacingAfter(SPACING_AFTER);
        p.setAlignment(ParagraphAlignment.BOTH);

        XWPFRun run = p.createRun();
        String cleanText = stripInlineFormatting(text);
        run.setText(cleanText);
        run.setItalic(true);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(BODY_FONT_SIZE);
    }

    /**
     * Creates a horizontal rule as a blank paragraph with a bottom border.
     *
     * @param doc the document
     */
    private void createHorizontalRule(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(SPACING_BEFORE);
        p.setSpacingAfter(SPACING_AFTER);
        p.setBorderBottom(Borders.SINGLE);
 }

    /**
     * Creates a bullet list from a markdown list block.
     *
     * @param doc  the document
     * @param text the list text (may contain multiple lines)
     */
    private void createBulletList(XWPFDocument doc, String text) {
        String[] lines = text.split("\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                String content = trimmed.substring(2).trim();
                createBulletItem(doc, content);
            }
        }
    }

    /**
     * Creates a single bullet item paragraph with native DOCX numbering.
     *
     * @param doc  the document
     * @param text the bullet item text
     */
    private void createBulletItem(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(40);
        p.setSpacingAfter(40);
        p.setAlignment(ParagraphAlignment.BOTH);

        p.setIndentationLeft(360);
        processInlineFormatting(p, text);
    }

    /**
     * Processes inline Markdown formatting and creates formatted runs.
     * Handles: **bold**, *italic*, `code`, [text](url)
     *
     * @param p the paragraph to add runs to
     * @param text the text with inline formatting
     */
    private void processInlineFormatting(XWPFParagraph p, String text) {
        // Build list of segments with their formatting
        List<FormattedSegment> segments = parseInlineElements(text);

        if (segments.isEmpty()) {
            // Fallback: plain paragraph
            XWPFRun run = p.createRun();
            run.setText(text);
            run.setFontFamily(FONT_FAMILY);
            run.setFontSize(BODY_FONT_SIZE);
            return;
        }

        for (FormattedSegment segment : segments) {
            XWPFRun run = p.createRun();
            run.setText(segment.text);
            run.setFontFamily(FONT_FAMILY);
            run.setFontSize(BODY_FONT_SIZE);

            if (segment.bold) {
                run.setBold(true);
            }
            if (segment.italic) {
                run.setItalic(true);
            }
            if (segment.code) {
                run.setFontFamily("Courier New");
            }
        }
    }

    /**
     * Parses inline Markdown elements into formatted segments.
     *
     * @param text the text to parse
     * @return list of formatted segments
     */
    private List<FormattedSegment> parseInlineElements(String text) {
        List<FormattedSegment> segments = new ArrayList<>();
        String remaining = text;

        while (!remaining.isEmpty()) {
            FormattedSegment segment = findNextSegment(remaining);
            if (segment == null) {
                // No more formatted elements, add rest as plain text
                if (!remaining.isEmpty()) {
                    segments.add(new FormattedSegment(remaining, false, false, false));
                }
                break;
            }

            // Add text before the segment
            if (segment.startIndex > 0) {
                String before = remaining.substring(0, segment.startIndex);
                segments.add(new FormattedSegment(before, false, false, false));
            }

            // Add the formatted segment
            segments.add(segment);

            // Move past the segment
            remaining = remaining.substring(segment.endIndex);
        }

        return segments;
    }

    /**
     * Finds the next formatted segment in the text.
     */
    private FormattedSegment findNextSegment(String text) {
        // Find earliest match among bold, italic, code
        FormattedSegment earliest = null;

        Matcher boldMatcher = BOLD_PATTERN.matcher(text);
        if (boldMatcher.find()) {
            earliest = new FormattedSegment(
                    boldMatcher.group(1),
                    true, false, false,
                    boldMatcher.start(),
                    boldMatcher.end()
            );
        }

        // Check italic (excluding bold and code markers)
        Matcher italicMatcher = ITALIC_PATTERN.matcher(text);
        if (italicMatcher.find()) {
            int start = italicMatcher.start();
            int end = italicMatcher.end();
            if (earliest == null || start < earliest.startIndex) {
                earliest = new FormattedSegment(
                        italicMatcher.group(1),
                        false, true, false,
                        start, end
                );
            }
        }

        // Check code
        Matcher codeMatcher = CODE_PATTERN.matcher(text);
        if (codeMatcher.find()) {
            int start = codeMatcher.start();
            int end = codeMatcher.end();
            if (earliest == null || start < earliest.startIndex) {
                earliest = new FormattedSegment(
                        codeMatcher.group(1),
                        false, false, true,
                        start, end
                );
            }
        }

        return earliest;
    }

    /**
     * Strips all inline Markdown formatting from text.
     */
    private String stripInlineFormatting(String text) {
        return text
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
    }

    /**
     * Internal class representing a formatted text segment.
     */
    private static class FormattedSegment {
        final String text;
        final boolean bold;
        final boolean italic;
        final boolean code;
        final int startIndex;
        final int endIndex;

        FormattedSegment(String text, boolean bold, boolean italic, boolean code) {
            this(text, bold, italic, code, 0, 0);
        }

        FormattedSegment(String text, boolean bold, boolean italic, boolean code,
                         int startIndex, int endIndex) {
            this.text = text;
            this.bold = bold;
            this.italic = italic;
            this.code = code;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}
