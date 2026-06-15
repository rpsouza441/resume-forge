# Phase 1: DOCX Fix - Context

**Gathered:** 2026-06-15
**Status:** Ready for planning

## Phase Boundary

Corrigir o `StructuredDocxConverter` para gerar DOCX visualmente profissional: header com nome/contatos, fontes hierárquicas corretas (10-11pt corpo), bullets OOXML reais, margens densas, sem Markdown residual, máximo 2 páginas.

## Requirements (locked via SPEC.md)

**8 requirements are locked.** See `01-docx-fix-SPEC.md` for full requirements, boundaries, and acceptance criteria.

Downstream agents MUST read `01-docx-fix-SPEC.md` before planning or implementing. Requirements are not duplicated here.

**In scope (from SPEC.md):**
- StructuredDocxConverter.java — correções de formatação
- DocxGenerationService.java — passagem de dados de header
- Testes estruturais — validação de formatação
- Sanitização de campos estruturados

**Out of scope (from SPEC.md):**
- Alterar system prompt da IA
- Copiar conteúdo do bom-exemplo.docx
- Criar novos dados de currículo
- Frontend de exibição
- Geração de markdown
- Alterar schema JSON da resposta IA

## Implementation Decisions

### Font Sizes (CRÍTICO - primeira execução falhou)
- **D-01:** Nome: 32-36 half-points (16-18 pt) — `run.setFontSize(32)`
- **D-02:** Título profissional: 21-23 half-points (10.5-11.5 pt) — `run.setFontSize(22)`
- **D-03:** Contatos: 18-20 half-points (9-10 pt) — `run.setFontSize(18)`
- **D-04:** Títulos de seção: 23-26 half-points (11.5-13 pt) — `run.setFontSize(24)`
- **D-05:** Corpo: 20-22 half-points (10-11 pt) — `run.setFontSize(20)` ou `21`
- **D-06:** Período: 18-20 half-points (9-10 pt) — `run.setFontSize(18)`

⚠️ **ERRO ANTERIOR:** `run.setFontSize(21)` criava 21pt, não 10.5pt. O código deve usar half-points corretos.

### Margins (CRÍTICO - primeira execução falhou)
- **D-07:** Superior/Inferior: 1.5-1.8cm → ~850-1020 twips
- **D-08:** Esquerda/Direita: 1.6-1.9cm → ~907-1076 twips
- **D-09:** Implementar com `CTPageMar` via `sectPr.getPgMar()`

⚠️ **ERRO ANTERIOR:** `setDocumentMargins()` estava vazio, sem `CTPageMar`.

### Header Structure (CRÍTICO - primeira execução falhou)
- **D-10:** Header local (não nativo Word — ATS pode ignorar)
- **D-11:** Estrutura:
  ```
  NOME COMPLETO (32-36 half-points, bold)
  Título profissional (21-23 half-points)
  Cidade | email | LinkedIn | GitHub | site (18-20 half-points)
  ──────────────────────────────────────── (border line)
  ```
- **D-12:** Omitir contatos ausentes
- **D-13:** Separador com `paragraph.setBorderBottom(Borders.SINGLE)`

⚠️ **ERRO ANTERIOR:** Documento iniciava em "Resumo Profissional" sem header.

### Bullets OOXML (IMPLEMENTAR CORRETAMENTE)
- **D-14:** Usar `document.createNumbering()` com `XWPFNumbering`
- **D-15:** Cada bullet: `paragraph.setNumId(numId)` via numbering definition
- **D-16:** Não simular com `-`, `•` ou caracteres especiais

### Experience Structure
- **D-17:** Estrutura:
  ```
  Empresa | Localidade (bold)
  Cargo | Período (bold/italic)
  • Highlight 1 (bullet real)
  • Highlight 2 (bullet real)
  ```
- **D-18:** `keepWithNext` no cargo para evitar quebra de página
- **D-19:** Período em itálico, menor que cargo

### Markdown Sanitization (CRÍTICO - primeira execução falhou)
- **D-20:** Remover `**`, `###`, `#` de campos estruturados ANTES de criar runs
- **D-21:** Validação pós-resposta da IA
- **D-22:** Manter asteriscos de dados técnicos (C++, Bash*)

⚠️ **ERRO ANTERIOR:** "**Infraestrutura:**" visível no DOCX.

### Named Styles
- **D-23:** Criar estilos XWPFStyle:
  - `ResumeName` — 16-18pt bold
  - `ResumeSectionHeading` — 11.5-13pt bold
  - `ResumeBullet` — 10-11pt
  - `ResumeJobPeriod` — 9-10pt italic

### Spacing
- **D-24:** Linha: simples ou 1.05
- **D-25:** `spaceAfter` corpo: 2-4 pt
- **D-26:** `spaceBefore` seções: 5-8 pt
- **D-27:** Sem linhas vazias artificiais

## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Specs
- `.planning/phases/01-docx-fix/01-docx-fix-SPEC.md` — **Locked requirements — MUST read**
- `.planning/phases/01-docx-fix/01-RESEARCH.md` — Technical research with bug findings

### Source Code
- `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java` — Main converter to fix
- `backend/src/main/java/com/resumeforge/export/service/DocxGenerationService.java` — Service layer

### QA Report (Issue Analysis)
- QA audit identified critical failures in first implementation:
  - Font sizes inverted (21pt body instead of 10.5pt)
  - Margins not applied (setDocumentMargins empty)
  - Header missing (name/title/contacts not generated)
  - Markdown residual (`**` visible in document)
  - No real OOXML bullets (text paragraphs, not numPr)

## Existing Code Insights

### Reusable Assets
- `StructuredDocxConverter.java` — Base implementation exists, needs fixes
- `DocxGenerationService.java` — Service layer, passes data to converter

### Established Patterns
- Apache POI 5.3.0 API (verified in pom.xml)
- Half-points for font sizes: `setFontSize(21)` = 10.5pt
- Twips for margins: 1cm ≈ 567 twips

### Integration Points
- `DocxGenerationService.generateDocx()` → `StructuredDocxConverter.convert()`
- Resume profile JSON → Converter → DOCX output

## Specific Ideas

- Header deve vir de `profile` e `contacts` do JSON estruturado
- Não é necessário enviar dados pessoais para a IA — inserir localmente
- Comparar com `bom-exemplo.docx` para referência visual (não copiar conteúdo)

## Deferred Ideas

- Lovable/Stitch para redesign visual (UX Improvement - Phase 3)
- Multi-language support
- WebSocket para real-time updates
- Frontend de exibição do currículo (problema separado)

---

*Phase: 01-docx-fix*
*Context gathered: 2026-06-15*
*Updated after QA audit — first implementation failed*
