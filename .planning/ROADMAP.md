---
name: ROADMAP
description: Phase roadmap for resume-forge
last_mapped_commit: HEAD
---

# ROADMAP — Resume Forge

**Date:** 2026-06-15  
**Project:** RFG

## Phases

| # | Phase | Goal | Status | Requirements |
|---|-------|------|--------|--------------|
| 1 | DOCX Fix | Corrigir renderer estruturado com fonte, bullets, header corretos | 🔴 Pending | DOCX-01 |
| 2 | AI Config Frontend | Mover AI config do env para UI (API key, provider, modelo) | 🟡 Pending | AI-01 |
| 3 | UX Improvement | Melhorias visuais e admin dashboard | 🟢 Future | UX-01 |

---

## Phase 1: DOCX Fix

**Goal:** Corrigir renderer estruturado com fonte, bullets, header corretos

**Plans:**
- [ ] 01-01-PLAN.md — Fix margins, bullets, separator (Wave 1)
- [ ] 01-02-PLAN.md — Add structural tests (Wave 2)
- [ ] 01-03-PLAN.md — Visual QA verification (Wave 3)

**Requirements:**
- DOCX-01: Corrigir tamanhos de fonte (corpo <= 11pt, titulos maiores)
- DOCX-02: Implementar header estruturado (nome, titulo, contatos)
- DOCX-03: Criar bullets reais OOXML
- DOCX-04: Remover Markdown residual
- DOCX-05: Ajustar margens e densidade (<= 2 paginas)
- DOCX-06: Adicionar testes estruturais
- DOCX-07: QA visual obrigatorio

**Canonical refs:**
- `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`
- `docs/resume-generation-v2/`

---

## Phase 2: AI Config Frontend

**Goal:** Mover AI config do env para UI (API key, provider, modelo)

**Requirements:**
- AI-01: Criar settings page no frontend
- AI-02: Provider selection (OpenAI, Claude, Gemini, OpenRouter)
- AI-03: API key input com masking
- AI-04: Model selection por provider
- AI-05: Backend storage (encrypted)
- AI-06: Rate limiting configuration

**Canonical refs:**
- `backend/src/main/java/com/resumeforge/config/AiProviderConfig.java`
- `frontend/src/app/(dashboard)/settings/page.tsx`

---

## Phase 3: UX Improvement

**Goal:** Melhorias visuais e admin dashboard

**Requirements:**
- UX-01: Design system audit
- UX-02: Admin dashboard para métricas
- UX-03: Version history improvements
- UX-04: Loading states e empty states

**Canonical refs:**
- `frontend/src/components/`
- `frontend/src/app/(dashboard)/`

---

## Deferred Ideas

- Lovable/Stitch para redesign visual
- WebSocket para real-time updates
- Multi-language support
