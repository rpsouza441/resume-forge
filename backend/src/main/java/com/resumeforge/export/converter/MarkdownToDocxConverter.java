package com.resumeforge.export.converter;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
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
 * - Font: Arial, 10.5pt body, 16pt headings, 12pt section titles, 9pt dates/contacts
 * - Margins: 2.5cm all sides
 * - Bullet lists: native DOCX bullets with CTNumbering
 * - Bold only for headings and semantic emphasis
 * - LEFT alignment for body (not justified)
 * - 1.0 line spacing
 * - Keep-with-next for headings
 */
@Component
public class MarkdownToDocxConverter {

    private static final String FONT_FAMILY = "Arial";

    // Font sizes in half-points (spec compliant)
    private static final int HEADER_FONT_SIZE = 32;      // 16pt - Name/Header
    private static final int SECTION_FONT_SIZE = 24;     // 12pt - Section titles (##)
    private static final int BODY_FONT_SIZE = 21;        // 10.5pt - Body text (NOT 11pt/22)
    private static final int SMALL_FONT_SIZE = 18;       // 9pt - Dates/contacts

    // Spacing values in twips (1pt = 20 twips)
    private static final int SPACING_BEFORE = 60;        // 3pt before paragraphs
    private static final int SPACING_AFTER = 60;         // 3pt after paragraphs
    private static final int LINE_SPACING = 240;         // 1.0 line spacing (12pt * 20)

    // Patterns for inline formatting
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");

    // Pattern to detect orphaned header: # followed by newline (not ## or ###)
    private static final Pattern ORPHANED_HEADER_PATTERN = Pattern.compile("^#\\s*\\n");

    /**
     * Converts a Markdown string into an in-memory XWPFDocument.
     *
     * @param markdown the Markdown content to convert
     * @return the generated DOCX document
     */
    public XWPFDocument convert(String markdown) {
        XWPFDocument document = new XWPFDocument();

        // Set page margins: 2.5cm on all sides (1cm = 567 twips, 2.5cm = 1417 twips)
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(1417));
        pageMar.setBottom(BigInteger.valueOf(1417));
        pageMar.setLeft(BigInteger.valueOf(1417));
        pageMar.setRight(BigInteger.valueOf(1417));

        // Remove orphaned header (# at start followed by newline) before processing
        markdown = stripOrphanedHeader(markdown);

        // Split by single newlines and process line by line
        String[] lines = markdown.split("\\n");

        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                i++;
                continue;
            }

            // Check for headings
            if (trimmed.startsWith("# ")) {
                createHeading(document, trimmed.substring(2), 1);
                i++;
            } else if (trimmed.startsWith("## ")) {
                createHeading(document, trimmed.substring(3), 2);
                i++;
            } else if (trimmed.startsWith("### ")) {
                createHeading(document, trimmed.substring(4), 3);
                i++;
            }
            // Check for horizontal rules
            else if (trimmed.matches("^-{3,}$") || trimmed.matches("^\\*{3,}$")) {
                createHorizontalRule(document);
                i++;
            }
            // Check for blockquotes
            else if (trimmed.startsWith("> ")) {
                // Collect consecutive blockquote lines
                StringBuilder blockquoteText = new StringBuilder();
                while (i < lines.length && lines[i].trim().startsWith("> ")) {
                    if (blockquoteText.length() > 0) {
                        blockquoteText.append(" ");
                    }
                    blockquoteText.append(lines[i].trim().substring(2));
                    i++;
                }
                createBlockquote(document, blockquoteText.toString());
            }
            // Check for bullet lists
            else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                // Collect consecutive list items
                List<String> listItems = new ArrayList<>();
                while (i < lines.length &&
                       (lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("* "))) {
                    String itemText = lines[i].trim().substring(2);
                    listItems.add(itemText);
                    i++;
                }
                createBulletList(document, listItems);
            }
            // Regular paragraph - collect consecutive non-empty lines
            else {
                StringBuilder paraText = new StringBuilder();
                while (i < lines.length) {
                    String currentLine = lines[i].trim();
                    if (currentLine.isEmpty()) {
                        break;
                    }
                    // Stop if we hit a special element
                    if (currentLine.startsWith("# ") || currentLine.startsWith("## ") ||
                        currentLine.startsWith("### ") ||
                        currentLine.matches("^-{3,}$") || currentLine.matches("^\\*{3,}$") ||
                        currentLine.startsWith("> ") ||
                        currentLine.startsWith("- ") || currentLine.startsWith("* ")) {
                        break;
                    }
                    if (paraText.length() > 0) {
                        paraText.append(" ");
                    }
                    paraText.append(currentLine);
                    i++;
                }
                if (paraText.length() > 0) {
                    createParagraph(document, paraText.toString());
                }
            }
        }

        return document;
    }

    /**
     * Strips orphaned header from markdown.
     * An orphaned header is a single # followed by a newline (not ## or ###).
     * This handles cases where the backend injects {HEADER} but Gemini doesn't use it.
     */
    private String stripOrphanedHeader(String markdown) {
        Matcher matcher = ORPHANED_HEADER_PATTERN.matcher(markdown);
        if (matcher.find()) {
            // Remove the orphaned # and following newline
            return markdown.substring(matcher.end());
        }
        return markdown;
    }

    /**
     * Creates a heading paragraph with proper styling and keep-with-next.
     *
     * @param doc   the document
     * @param text  the heading text
     * @param level heading level (1, 2, or 3)
     */
    private void createHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();

        // Configure paragraph properties for keep-with-next
        XPFOpts pPr = new XPFOpts();
        pPr.setKeepNext(true);
        pPr.setKeepLines(true);
        pPr.setSpacingBefore(240);
        pPr.setSpacingAfter(120);
        pPr.setLineSpacing(LINE_SPACING);

        XWPFRun run = p.createRun();

        // Strip inline formatting from heading text
        String cleanText = stripInlineFormatting(text);
        run.setText(cleanText);
        run.setBold(true);
        run.setFontFamily(FONT_FAMILY);

        // Font sizes: H1=16pt(32), H2=12pt(24), H3=10pt(20)
        switch (level) {
            case 1 -> {
                run.setFontSize(HEADER_FONT_SIZE);
                p.setSpacingBefore(360);
                p.setSpacingAfter(180);
            }
            case 2 -> {
                run.setFontSize(SECTION_FONT_SIZE);
                p.setSpacingBefore(240);
                p.setSpacingAfter(120);
            }
            default -> {
                run.setFontSize(20);
                p.setSpacingBefore(200);
                p.setSpacingAfter(100);
            }
        }

        // Set keep-with-next for headings
        p.setKeepNext(true);
    }

    /**
     * Creates a regular paragraph with inline formatting support.
     * Uses LEFT alignment (not justified).
     *
     * @param doc  the document
     * @param text the paragraph text
     */
    private void createParagraph(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(SPACING_BEFORE);
        p.setSpacingAfter(SPACING_AFTER);
        p.setAlignment(ParagraphAlignment.LEFT);  // LEFT, not BOTH (justified)
        p.setLineSpacing(LINE_SPACING);
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
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setLineSpacing(LINE_SPACING);

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
     * Creates a bullet list using native DOCX numbering (CTNumbering).
     *
     * @param doc   the document
     * @param items list of bullet item texts
     */
    private void createBulletList(XWPFDocument doc, List<String> items) {
        // Create numbering definition for bullets
        int numberingId = allocateNumberingId(doc);
        int numRefId = allocateAbstractNumId(doc);

        for (String itemText : items) {
            createNumberedBulletItem(doc, itemText, numberingId, numRefId);
        }
    }

    /**
     * Allocates a numbering ID for the document.
     */
    private int allocateNumberingId(XWPFDocument doc) {
        // Use a simple approach - find max existing ID and add 1
        int maxId = 0;
        try {
            for (XWPFNumbering numbering : doc.getNumbering()) {
                int id = numbering.getNumId();
                if (id > maxId) {
                    maxId = id;
                }
            }
        } catch (Exception e) {
            // Ignore if no numbering exists
        }
        return maxId + 1;
    }

    /**
     * Allocates an abstract numbering ID for the document.
     */
    private int allocateAbstractNumId(XWPFDocument doc) {
        int maxId = 0;
        try {
            for (XWPFNumbering numbering : doc.getNumbering()) {
                int id = numbering.getAbstractNumId();
                if (id > maxId) {
                    maxId = id;
                }
            }
        } catch (Exception e) {
            // Ignore if no numbering exists
        }
        return maxId + 1;
    }

    /**
     * Creates a single bullet item paragraph with native DOCX numbering.
     *
     * @param doc       the document
     * @param text      the bullet item text
     * @param numId     the numbering ID
     * @param abstractNumId the abstract numbering ID
     */
    private void createNumberedBulletItem(XWPFDocument doc, String text, int numId, int abstractNumId) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(40);
        p.setSpacingAfter(40);
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setLineSpacing(LINE_SPACING);

        // Set indentation for bullet
        p.setIndentationLeft(360);
        p.setIndentationHanging(360);

        // Apply numbering properties
        p.setNumId(numId);

        // Create run with text
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontFamily(FONT_FAMILY);
        run.setFontSize(BODY_FONT_SIZE);
        // Body text should NOT be bold
    }

    /**
     * Processes inline Markdown formatting and creates formatted runs.
     * Handles: **bold**, *italic*, `code`, [text](url)
     * Bold is only applied for semantic emphasis, not for entire content.
     *
     * @param p    the paragraph to add runs to
     * @param text the text with inline formatting
     */
    private void processInlineFormatting(XWPFParagraph p, String text) {
        // Build list of segments with their formatting
        List<FormattedSegment> segments = parseInlineElements(text);

        if (segments.isEmpty()) {
            // Fallback: plain paragraph (no bold)
            XWPFRun run = p.createRun();
            run.setText(text);
            run.setFontFamily(FONT_FAMILY);
            run.setFontSize(BODY_FONT_SIZE);
            run.setBold(false);  // Explicitly not bold
            return;
        }

        for (FormattedSegment segment : segments) {
            XWPFRun run = p.createRun();
            run.setText(segment.text);
            run.setFontFamily(FONT_FAMILY);
            run.setFontSize(BODY_FONT_SIZE);

            // Bold only for semantic emphasis (explicit ** markers)
            if (segment.bold) {
                run.setBold(true);
            } else {
                run.setBold(false);
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
        int earliestStart = Integer.MAX_VALUE;

        Matcher boldMatcher = BOLD_PATTERN.matcher(text);
        if (boldMatcher.find() && boldMatcher.start() < earliestStart) {
            earliestStart = boldMatcher.start();
            earliest = new FormattedSegment(
                    boldMatcher.group(1),
                    true, false, false,
                    boldMatcher.start(),
                    boldMatcher.end()
            );
        }

        // Check italic (excluding bold and code markers)
        Matcher italicMatcher = ITALIC_PATTERN.matcher(text);
        if (italicMatcher.find() && italicMatcher.start() < earliestStart) {
            earliestStart = italicMatcher.start();
            earliest = new FormattedSegment(
                    italicMatcher.group(1),
                    false, true, false,
                    italicMatcher.start(),
                    italicMatcher.end()
            );
        }

        // Check code
        Matcher codeMatcher = CODE_PATTERN.matcher(text);
        if (codeMatcher.find() && codeMatcher.start() < earliestStart) {
            earliestStart = codeMatcher.start();
            earliest = new FormattedSegment(
                    codeMatcher.group(1),
                    false, false, true,
                    codeMatcher.start(),
                    codeMatcher.end()
            );
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

    /**
     * Helper class for paragraph options (spacing, keep settings).
     */
    private static class XPFOpts {
        private boolean keepNext = false;
        private boolean keepLines = false;
        private int spacingBefore = 60;
        private int spacingAfter = 60;
        private int lineSpacing = 240;

        void setKeepNext(boolean keepNext) {
            this.keepNext = keepNext;
        }

        boolean isKeepNext() {
            return keepNext;
        }

        void setKeepLines(boolean keepLines) {
            this.keepLines = keepLines;
        }

        boolean isKeepLines() {
            return keepLines;
        }

        void setSpacingBefore(int spacingBefore) {
            this.spacingBefore = spacingBefore;
        }

        int getSpacingBefore() {
            return spacingBefore;
        }

        void setSpacingAfter(int spacingAfter) {
            this.spacingAfter = spacingAfter;
        }

        int getSpacingAfter() {
            return spacingAfter;
        }

        void setLineSpacing(int lineSpacing) {
            this.lineSpacing = lineSpacing;
        }

        int getLineSpacing() {
            return lineSpacing;
        }
    }
}
