---
phase: "01-docx-fix"
plan: "02"
subsystem: "StructuredDocxConverter"
tags: [docx, testing, apache-poi, junit]
dependency-graph:
  requires: ["01-01"]
  provides: ["DOCX-06"]
  affects: ["backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java"]
tech-stack:
  added: ["JUnit 5", "Apache POI XWPF API"]
  patterns: ["OOXML document structure validation", "Test fixture pattern"]
key-files:
  created:
    - "backend/src/test/java/com/resumeforge/export/converter/StructuredDocxConverterTest.java"
    - "backend/src/test/java/com/resumeforge/export/converter/StructuredDocxConverterTestData.java"
  modified:
    - "backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java"
decisions:
  - "Use BigInteger for CTLvl.setIlvl() (ooxml-schemas 1.4 API)"
  - "Use addNewStart() and addNewLvlText() for numbering definition"
  - "Set keepLines element without val (existence = true)"
  - "Inline regex patterns for markdown sanitization (\s*##\s*)"
metrics:
  duration: "~5 min"
  completed: "2026-06-15T14:33:38Z"
  tasks: "2/2"
  files: "3"
---

# Phase 01 Plan 02: Structural Tests for StructuredDocxConverter Summary

**Objective:** Add structural tests for StructuredDocxConverter that validate DOCX output without requiring Word.

## One-liner

Structural validation tests for DOCX output verifying margins, bullet numbering, markdown sanitization, font sizes, and header structure.

## Tasks Executed

### Task 1: Create test fixtures (TestData class)
- **Status:** Completed
- **Commit:** 42c0da8
- **Files:** `StructuredDocxConverterTestData.java`
- **Description:** Created test data fixtures providing sample resume structures (`sampleHeader()`, `sampleStructure()`, `minimalStructure()`)

### Task 2: Create structural validation tests
- **Status:** Completed
- **Commit:** 42c0da8
- **Files:** `StructuredDocxConverterTest.java`
- **Tests added:**
  - `testDocumentCreationSucceeds` - Basic document creation
  - `testHeaderStructure` - Verifies name is bold with correct font size (32 half-points)
  - `testMarginsApplied` - Verifies CTPageMar values (1.8cm top/bottom, 2.0cm left/right)
  - `testBulletHasNumbering` - Verifies bullet paragraphs have numId
  - `testNoMarkdownResidual` - Verifies **, ###, # markers are removed
  - `testBodyFontSizeLessThanOrEqual11pt` - Verifies body text <= 22 half-points
  - `testSectionHeadingsLargerThanBody` - Verifies section headings > body font size
  - `testExperienceStructureSeparateHeadersAndBullets` - Verifies bold headers and bullets exist

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] CTLvl API compatibility with ooxml-schemas 1.4**
- **Found during:** Running tests
- **Issue:** Production code used incorrect API methods:
  - `lvl.setIlvl(int)` should be `lvl.setIlvl(BigInteger)`
  - `lvl.addNewStartMsgId()` doesn't exist
  - `lvl.addNewLevelText()` doesn't exist
- **Fix:** Changed to correct API: `setIlvl(BigInteger.valueOf(0))`, `addNewStart()`, `addNewLvlText()`
- **Files modified:** `StructuredDocxConverter.java`
- **Commit:** b5da4f1

**2. [Rule 1 - Bug] XWPFParagraph spacing methods expect int, not BigInteger**
- **Found during:** Running tests
- **Issue:** `setSpacingBefore(BigInteger)` and `setSpacingAfter(BigInteger)` don't exist
- **Fix:** Changed to `setSpacingBefore(80)` and `setSpacingAfter(160)` (int parameters)
- **Files modified:** `StructuredDocxConverter.java`
- **Commit:** b5da4f1

**3. [Rule 1 - Bug] CTOnOff keepLines property setVal() fails**
- **Found during:** Running tests
- **Issue:** `onOff.setVal(STOnOff.TRUE)` throws XmlValueOutOfRangeException
- **Fix:** Removed setVal() call - element existence means "keep together"
- **Files modified:** `StructuredDocxConverter.java`
- **Commit:** b5da4f1

**4. [Rule 2 - Missing functionality] Missing markdown sanitization**
- **Found during:** testNoMarkdownResidual test
- **Issue:** Skill category names and experience role titles not sanitized
- **Fix:** Added `sanitizeMarkdown()` calls to `createSkillsSection()` and `createExperienceEntry()`
- **Files modified:** `StructuredDocxConverter.java`
- **Commit:** b5da4f1

**5. [Bug] Regex patterns for markdown headers only matched at line start**
- **Found during:** testNoMarkdownResidual test
- **Issue:** `^###\s*` only matches at line start
- **Fix:** Changed to `\s*###\s*` to match markdown headers anywhere in text
- **Files modified:** `StructuredDocxConverter.java`
- **Commit:** b5da4f1

## Test Results

```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
```

All structural validation tests pass:
- Document structure verified programmatically
- Margins, bullets, fonts, markdown removal all tested
- No Word installation required for validation

## Threat Flags

None - test code only validates production behavior.

## Self-Check

- [x] TestData class exists at `backend/src/test/java/com/resumeforge/export/converter/StructuredDocxConverterTestData.java`
- [x] Test class exists at `backend/src/test/java/com/resumeforge/export/converter/StructuredDocxConverterTest.java`
- [x] Tests pass with `mvn test -Dtest=StructuredDocxConverterTest`
- [x] Commits created with proper format

## Known Stubs

None.

## Commits

| Hash | Message |
|------|---------|
| 42c0da8 | feat(01-docx-fix-02): add structural validation tests for StructuredDocxConverter |
| b5da4f1 | fix(01-docx-fix-02): fix production code bugs found during testing |
