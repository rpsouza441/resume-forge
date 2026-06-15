---
phase: 01-docx-fix
plan: "01"
subsystem: docx-renderer
tags: [apache-poi, ooxml, docx, structured-resume]
dependency_graph:
  requires: []
  provides:
    - StructuredDocxConverter: "Fixed DOCX renderer with proper OOXML formatting"
  affects:
    - DocxGenerationService: "Uses fixed converter for resume export"
tech_stack:
  added: [apache-poi-ooxml]
  patterns: [CTPageMar margins, OOXML numbering, paragraph borders]
key_files:
  created: []
  modified:
    - path: "backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java"
      changes: "Implemented CTPageMar margins, initBulletNumbering(), border separator"
---

# Phase 01 Plan 01: DOCX Fix Summary

**One-liner:** Fixed critical DOCX rendering bugs: margins now applied via CTPageMar, bullets use proper OOXML numbering, separator uses paragraph border instead of underscores.

## Objective

Fix critical DOCX rendering bugs in StructuredDocxConverter: margins not applied, bullets missing numbering definition, separator using underscores instead of borders.

## Tasks Completed

| # | Task | Status | Commit |
|---|------|--------|--------|
| 1 | Fix document margins using CTPageMar | DONE | a313b07 |
| 2 | Implement real OOXML bullet numbering | DONE | a313b07 |
| 3 | Replace underscore separator with paragraph border | DONE | a313b07 |

## Changes Made

### Task 1: Fix document margins using CTPageMar

**Files modified:** `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`

**Changes:**
- Added imports for `CTSectPr` and `CTPageMar`
- Modified `setDocumentMargins()` to create a `CTPageMar` with proper margin values:
  - Top/Bottom: 1020 twips (1.8cm per D-04)
  - Left/Right: 1134 twips (2.0cm per D-04)

```java
private void setDocumentMargins(XWPFDocument document) {
    CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
    CTPageMar pageMar = sectPr.addNewPgMar();
    pageMar.setTop(MARGIN_TOP);
    pageMar.setBottom(MARGIN_BOTTOM);
    pageMar.setLeft(MARGIN_LEFT);
    pageMar.setRight(MARGIN_RIGHT);
}
```

### Task 2: Implement real OOXML bullet numbering

**Files modified:** `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`

**Changes:**
- Added instance fields: `XWPFNumbering numbering` and `BigInteger bulletNumId`
- Added `initBulletNumbering()` method that creates proper OOXML abstract numbering definition
- Called `initBulletNumbering()` in `createDocument()`
- Simplified `createBulletItem()` to use the initialized numbering

```java
private void initBulletNumbering() {
    CTAbstractNum ctAbstractNum = CTAbstractNum.Factory.newInstance();
    ctAbstractNum.setAbstractNumId(BigInteger.valueOf(1));
    // ... level configuration with bullet format
    XWPFAbstractNum abstractNum = new XWPFAbstractNum(ctAbstractNum);
    BigInteger abstractNumId = numbering.addAbstractNum(abstractNum);
    numbering.addNum(abstractNumId);
    this.bulletNumId = BigInteger.valueOf(1);
}
```

### Task 3: Replace underscore separator with paragraph border

**Files modified:** `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`

**Changes:**
- Added import for `Borders`
- Replaced `createSeparatorLine()` implementation to use `paragraph.setBorderBottom(Borders.SINGLE)` instead of underscore characters

```java
private void createSeparatorLine(XWPFDocument document) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setBorderBottom(Borders.SINGLE);
    paragraph.setSpacingBefore(BigInteger.valueOf(80));
    paragraph.setSpacingAfter(BigInteger.valueOf(160));
}
```

## Decisions Made

| ID | Decision | Rationale |
|----|----------|-----------|
| D-02 | Font sizes in half-points | Matches user decisions: NAME=32, TITLE=22, CONTACT=18, SECTION=26, BODY=20 |
| D-04 | Margins: 1.8cm top/bottom, 2.0cm left/right | Per user decision from CONTEXT.md |
| N/A | OOXML numbering approach | Creates custom abstract numbering for cross-platform bullet rendering |

## Metrics

| Metric | Value |
|--------|-------|
| Duration | ~5 minutes |
| Tasks Completed | 3/3 |
| Files Modified | 1 |
| Commits | 1 |
| Lines Added | +172 |
| Lines Removed | -62 |
| Compilation | PASSED |

## Verification Results

| Check | Result |
|-------|--------|
| `grep -c "CTPageMar"` | 2 (pass) |
| `grep -c "initBulletNumbering\|bulletNumId"` | 5 (pass) |
| `grep -c "setBorderBottom\|Borders.SINGLE"` | 1 (pass) |
| `mvn compile` | PASSED |

## Success Criteria

- [x] Document margins applied via CTPageMar (1.8cm/2.0cm per D-04)
- [x] OOXML bullet numbering initialized and used in createBulletItem()
- [x] Header separator uses paragraph border, not underscores
- [x] Font sizes match user decisions: NAME=32, TITLE=22, CONTACT=18, SECTION=26, BODY=20
- [x] Code compiles without errors

## Known Issues

None.

## Threat Flags

None - no new security surface introduced.
