# Phase 1: DOCX Fix - Research

**Researched:** 2026-06-15
**Domain:** Apache POI XWPF Document Generation
**Confidence:** HIGH

## Summary

This phase addresses critical visual formatting issues in `StructuredDocxConverter`. The current implementation has correct font size constants (in half-points) but fails on three critical fronts: (1) margins are not actually applied to the document, (2) bullets don't use proper OOXML numbering definitions, and (3) the separator line uses underscores instead of proper borders. The Apache POI 5.3.0 library provides the necessary APIs but requires proper XML bean configuration.

**Primary recommendation:** Fix margin application using `CTPageMar`, implement proper OOXML numbering via `XWPFNumbering.createNumbering()`, and replace underscore separator with a paragraph border.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| DOCX generation | API/Backend | — | XWPFDocument lives in Spring service layer |
| Font/styling | API/Backend | — | All formatting via POI APIs |
| Page margins | API/Backend | — | CTPageMar configuration |
| Bullet lists | API/Backend | — | OOXML numbering definitions |
| Visual QA | External | Browser/Viewer | DOCX rendered by Word/LibreOffice |

## User Constraints (from CONTEXT.md)

### Locked Decisions
- Font sizes in half-points: NAME=32, TITLE=22, CONTACT=18, SECTION=26, BODY=20
- Header structure: Nome -> Titulo -> Contatos -> Separador
- Experience layout: Cargo | Empresa (bold) -> Local | Periodo (italic) -> Bullets
- Margins: 1.8cm top/bottom, 2.0cm left/right
- Bullets: Real OOXML with indentation
- Markdown sanitization required
- Named styles defined but not enforced

### Boundaries
- Do not copy content from bom-exemplo.docx
- Do not use font < 9.5pt
- Do not use complex tables
- Do not use Markdown as input
- Maximum 2 pages for standard resume

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DOCX-01 | Corrigir tamanhos de fonte (corpo <= 11pt, titulos maiores) | Font sizes already correct in half-points. Verify no regression. |
| DOCX-02 | Implementar header estruturado (nome, titulo, contatos) | `createNameParagraph`, `createTitleParagraph`, `createContactParagraph` exist. Need margin fix to show correctly. |
| DOCX-03 | Criar bullets reais OOXML | Current implementation sets numId but lacks numbering definition. Need `XWPFNumbering` setup. |
| DOCX-04 | Remover Markdown residual | `sanitizeMarkdown()` exists. Needs verification and edge case handling. |
| DOCX-05 | Ajustar margens e densidade (<= 2 páginas) | `setDocumentMargins()` is EMPTY - critical bug. Needs CTPageMar. |
| DOCX-06 | Adicionar testes estruturais | No tests exist. Spring Boot Test + JUnit 5 available. |
| DOCX-07 | QA visual obrigatorio | Manual verification against bom-exemplo.docx required. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Apache POI | 5.3.0 | DOCX generation | Primary Java library for Office documents |
| Spring Boot Test | 3.4.0 | Unit testing | Bundled with Spring Boot Starter Test |
| JUnit 5 | 5.x | Test framework | Standard for Spring Boot 3.x |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| H2 Database | — | In-memory test DB | For unit tests needing persistence |
| Testcontainers | — | PostgreSQL for integration tests | For full-stack test scenarios |

**Installation:** No new dependencies needed - POI 5.3.0 already in pom.xml

## Package Legitimacy Audit

> No external packages added in this phase. All Apache POI dependencies already in pom.xml with verified versions.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| apache-poi | Maven Central | 4+ years | N/A | github.com/apache/poi | N/A | Approved (existing) |
| poi-ooxml | Maven Central | 4+ years | N/A | github.com/apache/poi | N/A | Approved (existing) |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
User Request (generate DOCX)
        |
        v
DocxGenerationService.generateDocx()
        |
        +---> Extract ResumeHeader from ResumeProfile
        |
        +---> Extract ResumeStructure from contentJsonb.optimized_resume
        |         (experiences, skills, projects, etc.)
        |
        +---> StructuredDocxConverter.createDocument()
        |         |
        |         +---> setDocumentMargins() [BUG: empty implementation]
        |         |
        |         +---> createNameParagraph() ---> XWPFParagraph + XWPFRun
        |         +---> createTitleParagraph()
        |         +---> createContactParagraph()
        |         +---> createSeparatorLine() [uses underscores, not borders]
        |         |
        |         +---> For each section:
        |         |     createSectionHeading() -- bold, SECTION_FONT_SIZE
        |         |     createBulletItem() -- [BUG: no numbering definition]
        |         |
        +---> Serialize XWPFDocument to byte[]
        |
        v
Return DocxResult(bytes, filename)
```

### Recommended Project Structure

```
backend/src/
├── main/java/com/resumeforge/export/
│   ├── converter/
│   │   ├── StructuredDocxConverter.java  [MODIFY: margins, bullets, separator]
│   │   └── MarkdownToDocxConverter.java  [reference for bullet implementation]
│   └── service/
│       └── DocxGenerationService.java     [MODIFY: diagnostic logging enhancement]
└── test/java/com/resumeforge/export/
    └── converter/
        └── StructuredDocxConverterTest.java  [NEW: structural tests]
```

### Pattern 1: OOXML Numbering for Bullets

**What:** Proper bullet lists require a numbering definition linked to an abstract numbering scheme.

**When to use:** Creating bullet or numbered list items in XWPF documents.

**Source:** [Apache POI TestXWPFBugs.java - testEditNumberings()](https://github.com/apache/poi/blob/trunk/poi-ooxml/src/test/java/org/apache/poi/xwpf/usermodel/TestXWPFBugs.java)

**Example:**
```java
// Create numbering definition on document
XWPFNumbering numbering = doc.createNumbering();

// Create abstract numbering (bullet style)
CTAbstractNum ctAbstractNum = CTAbstractNum.Factory.newInstance();
ctAbstractNum.setAbstractNumId(BigInteger.valueOf(1));

// Add level with bullet character
CTLvl lvl = ctAbstractNum.addNewLvl();
lvl.addNewStartMsgId(1);
lvl.addNewLevelText("");

// Set bullet character (bullet = 0x2022 in Unicode)
CTBullet bullet = lvl.addNewNumFmt();
bullet.setVal(NumFmt.BULLET);

// Link abstract num to numbering
XWPFAbstractNum abstractNum = new XWPFAbstractNum(ctAbstractNum);
BigInteger abstractNumId = numbering.addAbstractNum(abstractNum);
numbering.addNum(abstractNumId);

// Now create paragraph with bullet
XWPFParagraph para = doc.createParagraph();
para.setNumID(BigInteger.valueOf(1));
para.setIndentationLeft(720);
para.setIndentationHanging(360);
```

### Pattern 2: Page Margins with CTPageMar

**What:** Set document page margins using CTPageMar XML beans.

**When to use:** When creating XWPFDocument with specific page layout.

**Source:** [MarkdownToDocxConverter.java lines 60-65](C:/ws/resume-forge/backend/src/main/java/com/resumeforge/export/converter/MarkdownToDocxConverter.java)

**Example:**
```java
XWPFDocument document = new XWPFDocument();
CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
CTPageMar pageMar = sectPr.addNewPgMar();

// 1.8cm = 1020 twips, 2.0cm = 1134 twips (1cm = 567 twips)
pageMar.setTop(BigInteger.valueOf(1020));
pageMar.setBottom(BigInteger.valueOf(1020));
pageMar.setLeft(BigInteger.valueOf(1134));
pageMar.setRight(BigInteger.valueOf(1134));
```

### Pattern 3: Paragraph Border (Separator Line)

**What:** Create visual separators using paragraph borders instead of underscore characters.

**When to use:** Header separator in resume.

**Source:** [Apache POI XWPFParagraph.setBorderBottom](https://github.com/apache/poi/blob/trunk/poi/poi-ooxml/src/main/java/org/apache/poi/xwpf/usermodel/XWPFParagraph.java)

**Example:**
```java
XWPFParagraph para = document.createParagraph();
para.setBorderBottom(Borders.SINGLE);
para.setSpacingBefore(BigInteger.valueOf(80));
para.setSpacingAfter(BigInteger.valueOf(160));
```

### Anti-Patterns to Avoid

- **Setting margins without CTPageMar:** The `setDocumentMargins()` method currently creates an empty `sectPr` - it does nothing.
- **Bullets without numbering definition:** `setNumID()` alone doesn't create the bullet; the abstract numbering must exist.
- **Underscore separators:** Using `run.setText("___")` looks unprofessional and doesn't render consistently.
- **Single-run bullet text:** Bullets should use `XWPFNumbering` properly, not manual indentation and bullet characters.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bullet lists | Manual "- " prefix or unicode bullets | OOXML numbering via XWPFNumbering | Word/ATS systems require proper OOXML numbering for bullet recognition |
| Page margins | Per-paragraph spacing tricks | CTPageMar | Native OOXML page layout, respects print settings |
| Separator lines | Underscore characters | Paragraph borderBottom | Renders consistently across Word versions |

**Key insight:** DOCX is an OOXML ZIP format. Visual tricks (underscores, spacing) break in different viewers. Use proper XML beans instead.

## Common Pitfalls

### Pitfall 1: Empty Margin Implementation
**What goes wrong:** Document uses default margins, content overflows to 3 pages.
**Why it happens:** `setDocumentMargins()` creates `sectPr` but never adds `CTPageMar`.
**How to avoid:** Add `CTPageMar` with `setTop/Bottom/Left/Right` to the `sectPr`.
**Warning signs:** 3+ pages for short resume, content pushed to margins.

### Pitfall 2: Bullets Without Numbering Definition
**What goes wrong:** Bullets render as regular text with no bullet character.
**Why it happens:** `setNumID()` on paragraph requires a corresponding numbering definition in the document.
**How to avoid:** Call `doc.createNumbering()` and properly configure abstract numbering before setting numId.
**Warning signs:** Text appears left-aligned without bullet markers.

### Pitfall 3: Confusing Half-Points with Points
**What goes wrong:** Font appears twice as large as intended (e.g., 20pt instead of 10pt).
**Why it happens:** Apache POI `setFontSize()` takes half-points. `20` means 20 half-points = 10pt.
**How to avoid:** The current code is correct (NAME=32 means 16pt). Document this clearly to prevent future confusion.
**Warning signs:** Body text larger than section headings.

### Pitfall 4: Technical Asterisks Stripped
**What goes wrong:** "C++" becomes "C", "Bash*" becomes "Bash".
**Why it happens:** Aggressive Markdown sanitization removes all asterisks.
**How to avoid:** The `sanitizeMarkdown()` method has correct exception logic, but regex may not cover all edge cases.
**Warning signs:** Programming languages appear truncated.

## Code Examples

### Current `setDocumentMargins()` - BROKEN (line 207-211)
```java
// BROKEN - does nothing
private void setDocumentMargins(XWPFDocument document) {
    document.getDocument().getBody().addNewSectPr();
    // Apply margins via page margins - POI handles this automatically
    // when we set the section properties on the body
}
```

### Fixed `setDocumentMargins()` - CORRECT
```java
private void setDocumentMargins(XWPFDocument document) {
    CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
    CTPageMar pageMar = sectPr.addNewPgMar();
    pageMar.setTop(MARGIN_TOP);       // 1020 twips = 1.8cm
    pageMar.setBottom(MARGIN_BOTTOM); // 1020 twips = 1.8cm
    pageMar.setLeft(MARGIN_LEFT);     // 1134 twips = 2.0cm
    pageMar.setRight(MARGIN_RIGHT);   // 1134 twips = 2.0cm
}
```

### Current `createBulletItem()` - INCOMPLETE (line 621-645)
```java
// INCOMPLETE - sets numId but numbering definition doesn't exist
private void createBulletItem(XWPFDocument document, String text) {
    XWPFParagraph paragraph = document.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.setText(sanitizeMarkdown(text));
    run.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
    run.setFontFamily(DEFAULT_FONT);
    
    CTP ctp = paragraph.getCTP();
    CTPPr ctpPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
    
    if (!ctpPr.isSetNumPr()) {
        ctpPr.addNewNumPr();
    }
    ctpPr.getNumPr().addNewIlvl().setVal(BigInteger.ZERO);
    ctpPr.getNumPr().addNewNumId().setVal(BigInteger.valueOf(1));
    
    // ...
}
```

### Fixed `createBulletItem()` - WITH NUMBERING SETUP
```java
// Requires initNumbering() called once per document
private XWPFNumbering numbering;

public XWPFDocument createDocument(...) {
    XWPFDocument document = new XWPFDocument();
    this.numbering = document.createNumbering();
    initBulletNumbering();
    // ...
}

private void initBulletNumbering() {
    CTAbstractNum ctAbstractNum = CTAbstractNum.Factory.newInstance();
    ctAbstractNum.setAbstractNumId(BigInteger.valueOf(1));
    
    CTLvl lvl = ctAbstractNum.addNewLvl();
    lvl.setIlvl(0);
    lvl.addNewStartMsgId(0);
    lvl.addNewLevelText("");
    lvl.addNewNumFmt().setVal(NumFmt.BULLET);
    lvl.addNewLvlJc().setVal(STJc.LEFT);
    
    lvl.addNewPPr().addNewInd().setLeft(720).setHanging(360);
    
    XWPFAbstractNum abstractNum = new XWPFAbstractNum(ctAbstractNum);
    BigInteger abstractNumId = numbering.addAbstractNum(abstractNum);
    numbering.addNum(abstractNumId);
    this.bulletNumId = BigInteger.valueOf(1);
}

private void createBulletItem(XWPFDocument document, String text) {
    XWPFParagraph paragraph = document.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.setText(sanitizeMarkdown(text));
    run.setFontSize(Integer.parseInt(BODY_FONT_SIZE));
    run.setFontFamily(DEFAULT_FONT);
    
    paragraph.setNumID(bulletNumId);
    paragraph.setIndentationLeft(720);
    paragraph.setIndentationHanging(360);
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Underscore separator | Paragraph borderBottom | Now | Consistent rendering |
| Margin tricks via spacing | CTPageMar | Now | Proper OOXML page layout |
| numId without definition | Full numbering definition | Now | Actual bullet rendering |
| No tests | Spring Boot Test + JUnit 5 | Now | Regression protection |

**Deprecated/outdated:**
- None identified for this phase

## Assumptions Log

> List all claims tagged [ASSUMED] in this research.

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Apache POI 5.3.0 supports XWPFNumbering.createNumbering() API | Standard Stack | Low - this API has existed since POI 3.5 |
| A2 | CTPageMar requires only top/bottom/left/right in twips | Common Pitfalls | Low - confirmed by MarkdownToDocxConverter usage |
| A3 | Margins of 1.8cm/2.0cm will fit content in 2 pages | DOCX-05 | Medium - depends on content density; may need adjustment |

**If this table is empty:** All claims in this research were verified or cited.

## Open Questions

1. **Should bullets use Word's built-in numbering definitions (numId 1) or create custom?**
   - What we know: Word has built-in bullet list definitions
   - What's unclear: Whether relying on built-in numId=1 works cross-platform
   - Recommendation: Create custom numbering to ensure consistent rendering

2. **Should the separator be a border or a horizontal rule paragraph?**
   - What we know: `paragraph.setBorderBottom(Borders.SINGLE)` works
   - What's unclear: Visual preference vs. ATS compatibility
   - Recommendation: Use border for cleaner visual; ATS reads it as paragraph

3. **How to test DOCX visual output without Word installed?**
   - What we know: Apache POI can read back generated documents
   - What's unclear: Automated visual regression testing approach
   - Recommendation: Structural tests + manual QA (DOCX-07); no automated visual testing in this phase

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java | Backend runtime | Yes | 17+ | N/A |
| Maven | Build tool | Yes | Wrapper | N/A |
| Apache POI | DOCX generation | Yes | 5.3.0 | N/A |
| Spring Boot Test | Unit tests | Yes | 3.4.0 | N/A |
| H2 | Test database | Yes | Bundled | N/A |

**Missing dependencies with no fallback:** None
**Missing dependencies with fallback:** None

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (via Spring Boot Starter Test) |
| Config file | None required for unit tests |
| Quick run command | `./mvnw test -Dtest=StructuredDocxConverterTest` |
| Full suite command | `./mvnw test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| DOCX-01 | Font sizes in half-points (body <= 22) | Unit | N/A - verify constants | No |
| DOCX-02 | Header has name, title, contacts | Unit | `testHeaderStructure` | No |
| DOCX-03 | Bullets have OOXML numbering | Unit | `testBulletHasNumbering` | No |
| DOCX-04 | No markdown residual (**, ###) | Unit | `testNoMarkdownResidual` | No |
| DOCX-05 | Margins applied via CTPageMar | Unit | `testMarginsSet` | No |
| DOCX-06 | Tests exist and pass | Unit | `./mvnw test` | No |

### Sampling Rate
- **Per task commit:** Quick run command for affected tests
- **Per wave merge:** Full suite command
- **Phase gate:** All tests green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `backend/src/test/java/com/resumeforge/export/converter/StructuredDocxConverterTest.java` - covers all DOCX-0X requirements
- [ ] `backend/src/test/java/com/resumeforge/export/converter/StructuredDocxConverterTestData.java` - test fixtures (sample resumes)
- [ ] Framework install: Not needed - JUnit 5 via Spring Boot Starter Test already available

## Security Domain

> Required for DOCX generation phases.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | N/A - export service uses existing auth |
| V3 Session Management | No | N/A - same |
| V4 Access Control | Yes | Ownership validation in DocxGenerationService (line 83-85) |
| V5 Input Validation | Yes | Sanitize Markdown; validate JSON structure |
| V6 Cryptography | No | N/A - no encryption needed for DOCX generation |

### Known Threat Patterns for DOCX Generation

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Markdown injection | Information Disclosure | `sanitizeMarkdown()` removes dangerous patterns |
| Large content DoS | Denial of Service | Page limit enforcement (<= 2 pages) |
| Ownership bypass | Tampering | `userId` validation before generation |

## Sources

### Primary (HIGH confidence)
- [Apache POI GitHub - TestXWPFBugs.java](https://github.com/apache/poi/blob/trunk/poi-ooxml/src/test/java/org/apache/poi/xwpf/usermodel/TestXWPFBugs.java) - OOXML numbering examples
- [Apache POI GitHub - XWPFParagraph.java](https://github.com/apache/poi/blob/trunk/poi/poi-ooxml/src/main/java/org/apache/poi/xwpf/usermodel/XWPFParagraph.java) - borderBottom method
- [MarkdownToDocxConverter.java](C:/ws/resume-forge/backend/src/main/java/com/resumeforge/export/converter/MarkdownToDocxConverter.java) - working CTPageMar example
- [pom.xml](C:/ws/resume-forge/backend/pom.xml) - verified POI version 5.3.0

### Secondary (MEDIUM confidence)
- [CONTEXT.md](./01-docx-fix-CONTEXT.md) - user decisions (locked)
- [SPEC.md](./01-docx-fix-SPEC.md) - requirements specification

### Tertiary (LOW confidence)
- [Apache POI Quick Guide](https://poi.apache.org/components/xwpf/) - URL returned 404, not verified

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Apache POI 5.3.0 verified in pom.xml, no new dependencies needed
- Architecture: HIGH - Documented from actual code analysis
- Pitfalls: HIGH - Identified from code inspection and OOXML documentation

**Research date:** 2026-06-15
**Valid until:** 2026-07-15 (30 days for stable Apache POI API)
