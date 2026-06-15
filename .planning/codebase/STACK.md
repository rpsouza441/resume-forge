---
name: STACK
description: Technology stack and dependencies
last_mapped_commit: HEAD
---

# STACK — Technology & Dependencies

**Date:** 2026-06-15  
**Focus:** Tech

## Languages & Runtimes

| Component | Language | Version |
|-----------|----------|---------|
| Backend | Java | 17+ |
| Frontend | TypeScript | 5.6.3 |
| Frontend Runtime | Node.js | 18+ |
| Build Tool (Backend) | Maven | Wrapper (`./mvnw`) |

## Backend Stack

### Core Framework
- **Spring Boot** — REST API framework
- **Spring Security** — Authentication & authorization
- **Spring Data JPA** — Database ORM

### Database
- **Flyway** — Database migrations (`db/migration/*.sql`)

### AI Integration
- **WebClient** (Spring WebFlux) — HTTP client for AI APIs
- **AI Providers** (strategy pattern):
  - `OpenAiProvider` — OpenAI GPT models
  - `ClaudeAiProvider` — Anthropic Claude
  - `GeminiAiProvider` — Google Gemini
  - `OpenRouterProvider` — OpenRouter aggregator

### Export
- **Apache POI** — DOCX generation (`StructuredDocxConverter`, `MarkdownToDocxConverter`)

### Utilities
- **jjwt** — JWT token handling
- **Lombok** — Boilerplate reduction

## Frontend Stack

### Core Framework
- **Next.js** 14.2.18 — React framework
- **React** 18.3.1 — UI library
- **TypeScript** 5.6.3 — Type safety

### State & Data Fetching
- **TanStack React Query** 5.59.20 — Server state management
- **Axios** 1.7.9 — HTTP client

### Forms & Validation
- **React Hook Form** 7.53.2 — Form management
- **@hookform/resolvers** 3.9.1 — Schema resolvers
- **Zod** 3.23.8 — Schema validation

### UI Components
- **Tailwind CSS** 3.4.15 — Styling
- **Radix UI** — Headless components (Dialog, Select, Tabs, Toast)
- **Lucide React** 0.460.0 — Icons
- **Sonner** 1.7.1 — Toast notifications
- **date-fns** 3.6.0 — Date formatting

### Utilities
- **clsx** / **tailwind-merge** — Class name utilities

## Development Tools

### Backend
- **Maven Wrapper** (`./mvnw`) — Build & test
- **Spring Boot DevTools** — Hot reload

### Frontend
- **ESLint** 8.57.1 — Linting
- **ESLint Config Next** 14.2.18 — Next.js linting rules
- **PostCSS** + **Autoprefixer** — CSS processing

## Key Configuration Files

| File | Purpose |
|------|---------|
| `backend/pom.xml` | Maven dependencies |
| `backend/src/main/resources/application.yml` | Spring config |
| `frontend/package.json` | Node dependencies |
| `frontend/next.config.js` | Next.js config |
| `frontend/tailwind.config.js` | Tailwind config |

## Environment Variables

### Backend (`application.yml` profiles)
- `spring.datasource.*` — Database connection
- `jwt.secret` — JWT signing key
- `ai.provider.*` — AI API keys per provider

### Frontend
- `NEXT_PUBLIC_API_URL` — Backend API URL
