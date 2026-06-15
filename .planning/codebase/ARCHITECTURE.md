---
name: ARCHITECTURE
description: System design, patterns, and data flow
last_mapped_commit: HEAD
---

# ARCHITECTURE — System Design & Patterns

**Date:** 2026-06-15  
**Focus:** Arch

## Architecture Pattern

**Layered Architecture** with clear separation:

```
┌─────────────────────────────────────────┐
│           Frontend (Next.js)            │
│  Pages → Components → Hooks → API       │
└──────────────────┬──────────────────────┘
                   │ HTTP + JWT
┌──────────────────▼──────────────────────┐
│           Backend (Spring Boot)         │
│  Controller → Service → Repository      │
│         (DTOs for data transfer)        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│              Database                   │
│           (JPA/Hibernate)              │
└────────────────────────────────────────┘
```

## Backend Architecture

### Package Structure
```
com.resumeforge
├── auth/           # Authentication (JWT, User, Security)
├── config/         # Spring configurations
├── common/        # Shared utilities (SecurityUtils, DTOs)
├── exception/      # Custom exceptions + GlobalExceptionHandler
├── resume/         # Resume CRUD operations
├── job/            # Job application management
├── generation/     # AI-powered resume generation
├── export/         # DOCX export functionality
├── ai/             # AI provider abstraction & orchestration
└── logging/        # Processing logs
```

### Design Patterns

| Pattern | Implementation |
|---------|----------------|
| **Strategy** | `AiProvider` interface with `OpenAiProvider`, `ClaudeAiProvider`, etc. |
| **Facade** | `AiOrchestrationService` simplifies multi-provider access |
| **Builder** | `PromptBuilderService` constructs AI prompts |
| **Converter** | `StructuredDocxConverter`, `MarkdownToDocxConverter` |

### Security Flow
```
Request → JwtAuthenticationFilter → SecurityContext → Controller
                ↓
         JwtTokenProvider (validates token)
                ↓
         UserDetailsServiceImpl (loads user)
```

### AI Generation Flow
```
1. Frontend: POST /api/generation/generate
2. GenerationController: Validates request
3. AiOrchestrationService: Selects provider
4. PromptBuilderService: Builds prompt from job + resume
5. AiProvider: Calls selected AI API
6. ContentValidationService: Validates output
7. GeneratedResume: Saved to database
8. Response: Returns generated content
```

## Frontend Architecture

### Directory Structure
```
frontend/src/
├── app/              # Next.js App Router pages
│   ├── (auth)/       # Auth pages (login, register)
│   ├── (dashboard)/  # Protected pages (resumes, jobs, generated)
│   └── layout.tsx    # Root layout
├── components/
│   ├── ui/           # Reusable UI components
│   ├── forms/        # Form components
│   ├── layout/       # Layout components (Sidebar, NavBar)
│   └── shared/       # Shared components
├── hooks/
│   └── queries/      # React Query hooks
├── providers/       # Context providers
├── lib/              # Utilities (api, utils, constants)
├── schemas/          # Zod validation schemas
└── types/            # TypeScript types
```

### State Management
- **Server State:** TanStack React Query (caching, refetching)
- **Auth State:** React Context (`AuthProvider`)
- **Form State:** React Hook Form (controlled inputs)

### API Layer
- `lib/api.ts` — Axios instance with interceptors
- JWT token attached automatically via interceptor

## Data Flow

### Resume Generation Flow
```
User fills form → Job + Resume data
       ↓
Frontend sends GenerationRequest
       ↓
Backend validates & stores AiRun
       ↓
PromptBuilderService creates prompt
       ↓
AI Provider generates content
       ↓
ContentValidationService validates
       ↓
GeneratedResume saved
       ↓
Frontend displays & allows editing
       ↓
User downloads as DOCX
```

## Key Entry Points

| Component | Entry Point | Purpose |
|-----------|-------------|---------|
| Backend | `ResumeForgeApplication.java` | Spring Boot main class |
| Auth | `AuthController.java` | `/api/auth/*` endpoints |
| Resume | `ResumeController.java` | `/api/resumes/*` endpoints |
| Job | `JobController.java` | `/api/jobs/*` endpoints |
| Generation | `GenerationController.java` | `/api/generation/*` endpoints |
| Export | `ExportController.java` | `/api/export/*` endpoints |
| Frontend | `frontend/src/app/page.tsx` | Landing page |
| Dashboard | `frontend/src/app/(dashboard)/dashboard/page.tsx` | Main dashboard |
