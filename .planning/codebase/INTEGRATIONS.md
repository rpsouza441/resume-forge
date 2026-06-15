---
name: INTEGRATIONS
description: External services, APIs, and integrations
last_mapped_commit: HEAD
---

# INTEGRATIONS — External Services & APIs

**Date:** 2026-06-15  
**Focus:** Tech

## AI Providers

The system supports multiple AI providers via a **strategy pattern** (`AiProvider` interface):

### OpenAI
- **Endpoint:** `https://api.openai.com/v1/chat/completions`
- **Model:** Configurable (e.g., `gpt-4o`)
- **Config:** `ai.provider.openai.api-key`, `ai.provider.openai.model`

### Anthropic Claude
- **Endpoint:** `https://api.anthropic.com/v1/messages`
- **Model:** Configurable (e.g., `claude-sonnet-4-20250514`)
- **Config:** `ai.provider.anthropic.api-key`, `ai.provider.anthropic.model`

### Google Gemini
- **Endpoint:** `https://generativelanguage.googleapis.com/v1beta/models/`
- **Model:** Configurable (e.g., `gemini-2.0-flash`)
- **Config:** `ai.provider.gemini.api-key`, `ai.provider.gemini.model`

### OpenRouter
- **Endpoint:** `https://openrouter.ai/api/v1/chat/completions`
- **Model:** Configurable
- **Config:** `ai.provider.openrouter.api-key`, `ai.provider.openrouter.model`

### Orchestration
- `AiOrchestrationService` — Routes requests to selected provider
- `PromptBuilderService` — Builds prompts from job/resume data
- `ContentValidationService` — Validates AI output

## Database

- **H2** (development) — In-memory/file-based
- **PostgreSQL** (production) — External database
- **Flyway** — Migration management

## Authentication

- **JWT** — Stateless token-based auth
- **Spring Security** — Filter chain with `JwtAuthenticationFilter`
- `JwtTokenProvider` — Token generation/validation
- `UserDetailsServiceImpl` — User loading for Spring Security

## Export Integrations

### DOCX Generation
- **Apache POI** (`org.apache.poi:poi-ooxml`) — Word document creation
- **Custom Converters:**
  - `StructuredDocxConverter` — Creates structured Word docs
  - `MarkdownToDocxConverter` — Converts markdown to DOCX

## Frontend → Backend Communication

- **Axios** HTTP client with JWT interceptor
- **React Query** for caching and state management
- API base URL configured via environment

## Key Integration Points

| Service | Integration | File |
|---------|-------------|------|
| AI APIs | WebClient | `backend/src/main/java/com/resumeforge/ai/provider/impl/*` |
| Database | Spring Data JPA | `backend/src/main/java/com/resumeforge/*/repository/*` |
| DOCX Export | Apache POI | `backend/src/main/java/com/resumeforge/export/*` |
| Auth | JWT + Spring Security | `backend/src/main/java/com/resumeforge/auth/security/*` |
