---
name: REQUIREMENTS
description: Requirement traceability matrix
---

# REQUIREMENTS — Resume Forge

**Last Updated:** 2026-06-15

## Requirement Matrix

| ID | Requirement | Phase | Status | Source |
|----|-------------|-------|--------|--------|
| DOCX-01 | Corrigir tamanhos de fonte (corpo <= 11pt, titulos maiores) | 01-docx-fix | COMPLETE | Plan 01 |
| DOCX-02 | Implementar header estruturado (nome, titulo, contatos) | 01-docx-fix | COMPLETE | Plan 01 |
| DOCX-03 | Criar bullets reais OOXML | 01-docx-fix | COMPLETE | Plan 01 |
| DOCX-04 | Remover Markdown residual | 01-docx-fix | COMPLETE | Plan 01 |
| DOCX-05 | Ajustar margens e densidade (<= 2 páginas) | 01-docx-fix | COMPLETE | Plan 01 |
| DOCX-06 | Adicionar testes estruturais | 01-docx-fix | DEFERRED | Future plan |
| DOCX-07 | QA visual obrigatorio | 01-docx-fix | MANUAL | Manual verification |
| AUTH-01 | JWT authentication | Core | COMPLETE | Existing |
| AUTH-02 | Refresh token rotation | Core | COMPLETE | Existing |
| UI-01 | Admin dashboard | 03-ux | DEFERRED | Future plan |

## Verification

All DOCX-01 through DOCX-05 requirements verified via:
- Compilation success
- grep verification for key patterns (CTPageMar, initBulletNumbering, Borders.SINGLE)
