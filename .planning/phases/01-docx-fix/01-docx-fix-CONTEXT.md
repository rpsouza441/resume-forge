---
name: CONTEXT
description: Phase 1 context - DOCX Fix discussion decisions
phase: 01-docx-fix
---

# CONTEXT — Phase 1: DOCX Fix

**Phase:** 01-docx-fix  
**Date:** 2026-06-15

---

## Domain

Corrigir o renderer estruturado `StructuredDocxConverter` para gerar DOCX visualmente correto, comparável ao `bom-exemplo.docx`.

---

## Canonical refs

- `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`
- `backend/src/main/java/com/resumeforge/export/converter/MarkdownToDocxConverter.java`
- `backend/src/main/java/com/resumeforge/export/service/DocxGenerationService.java`
- `docs/resume-generation-v2/`
- `bom-exemplo.docx` (referência visual apenas)

---

## Decisions

### DOCX Renderer Selection
- Confirmar se `StructuredDocxConverter` foi selecionado
- Log diagnóstico obrigatório antes de qualquer correção
- Se fallback foi usado, identificar motivo exato

### Font Sizes (Apache POI Half-Points)
- Nome: 32-36 half-points (16-18 pt)
- Título profissional: 21-23 half-points (10.5-11.5 pt)
- Contatos: 18-20 half-points (9-10 pt)
- Títulos de seção: 23-26 half-points (11.5-13 pt)
- Corpo: 20-22 half-points (10-11 pt)
- Cargo/empresa: 20-22 half-points (10-11 pt)
- Período: 18-20 half-points (9-10 pt)
- Skills compactas: 19-21 half-points (9.5-10.5 pt)

### Header Structure
```
NOME COMPLETO
Título profissional
Cidade/Estado | email | LinkedIn | GitHub | site
────────────────────────────────────────────
```
- Header local (não nativo Word - ATS pode ignorar)
- Nome em destaque (16-18 pt)
- Contatos omitidos se ausentes

### Experience Structure
```
Empresa | Localidade
Cargo | Período
• Highlight 1
• Highlight 2
```
-keepWithNext no cargo
- Bullets reais OOXML
- Período em itálico, menor

### Margins
- Superior: 1.5-1.8 cm
- Inferior: 1.5-1.8 cm
- Esquerda: 1.6-1.9 cm
- Direita: 1.6-1.9 cm

### Spacing
- Linha: simples ou 1.05
- spaceAfter corpo: 2-4 pt
- spaceBefore seções: 5-8 pt
- Sem linhas vazias artificiais

### Markdown Sanitization
- Remover `**`, `###`, `#` de campos estruturados
- Manter asteriscos de dados técnicos (C++, Bash*)
- Validação pós-resposta da IA

### Named Styles
- `ResumeName`
- `ResumeProfessionalTitle`
- `ResumeContact`
- `ResumeSectionHeading`
- `ResumeSummary`
- `ResumeJobHeader`
- `ResumeJobPeriod`
- `ResumeBullet`
- `ResumeSkill`
- `ResumeCompactItem`

---

## Boundaries

- Não copiar conteúdo do bom-exemplo.docx
- Não usar fonte < 9.5 pt
- Não usar tabelas complexas
- Não usar Markdown como entrada
- Máximo 2 páginas para currículo padrão

---

## Test Requirements

Testes estruturais devem validar:
1. Nome no início
2. Título profissional presente
3. Contatos quando existirem
4. Sem `**`, `###`, `{HEADER}`
5. Fonte corpo ≤ 11 pt
6. Bullets reais OOXML
7. Experiências com header separado dos highlights
8. Seções esperadas presentes
9. Sem fallback Markdown

---

## Deferred Ideas

- Lovable/Stitch para redesign visual (UX Improvement - Phase 3)
- Multi-language support
- WebSocket para real-time updates
