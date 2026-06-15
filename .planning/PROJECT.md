---
name: PROJECT
description: AI-powered resume builder monorepo
last_mapped_commit: HEAD
---

# PROJECT — Resume Forge

**Date:** 2026-06-15  
**Code:** RFG

## Vision

AI-powered resume builder that generates structured, ATS-friendly resumes by analyzing job descriptions and user profiles. The system integrates multiple AI providers (OpenAI, Claude, Gemini) and exports to professional DOCX format.

## Tech Stack

- **Backend:** Spring Boot (Java 17+), Spring Security, JPA, Flyway
- **Frontend:** Next.js 14, React 18, TypeScript, TanStack Query, Tailwind CSS
- **AI:** Multi-provider strategy (OpenAI, Claude, Gemini, OpenRouter)
- **Export:** Apache POI for DOCX generation

## Current State

Brownfield project with working authentication, resume CRUD, job management, and AI generation. Key areas need improvement: DOCX renderer, test coverage, error handling.

## Planned Epics

| # | Epic | Priority | Base |
|---|------|----------|------|
| 1 | DOCX Fix | 🔴 Alta | concerns: DOCX Generation Complexity |
| 2 | AI Config Frontend | 🟡 Média | concerns: JWT refresh, API key rotation |
| 3 | UX Improvement | 🟢 Baixa | concerns: Admin dashboard, UI gaps |

## Canonical References

- `.planning/codebase/STACK.md` — Technology stack
- `.planning/codebase/ARCHITECTURE.md` — System design
- `.planning/codebase/CONCERNS.md` — Technical debt and risks
