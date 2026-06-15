---
name: STRUCTURE
description: Directory layout and organization
last_mapped_commit: HEAD
---

# STRUCTURE — Directory Layout

**Date:** 2026-06-15  
**Focus:** Arch

## Monorepo Root

```
resume-forge/
├── package.json           # Root npm scripts (dev:backend, dev:frontend)
├── README.md
├── CLAUDE.md
├── .claude/               # Claude Code config & skills
│   ├── settings.local.json
│   └── skills/            # Installed skills
├── backend/               # Spring Boot application
└── frontend/              # Next.js application
```

## Backend Structure

```
backend/
├── pom.xml                # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/resumeforge/
│   │   │   ├── ResumeForgeApplication.java
│   │   │   ├── auth/              # Authentication
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   └── service/
│   │   │   ├── config/             # Spring configurations
│   │   │   ├── common/             # Shared utilities
│   │   │   ├── exception/          # Custom exceptions
│   │   │   ├── resume/             # Resume management
│   │   │   ├── job/                # Job applications
│   │   │   ├── generation/         # AI generation
│   │   │   ├── export/             # DOCX export
│   │   │   ├── ai/                 # AI providers
│   │   │   │   ├── provider/       # Provider interfaces & implementations
│   │   │   │   └── service/
│   │   │   └── logging/            # Processing logs
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── db/migration/       # Flyway migrations
│   │       └── ai/system-prompt.txt
│   └── test/
└── target/                # Maven build output
```

## Frontend Structure

```
frontend/
├── package.json
├── next.config.js
├── tailwind.config.js
├── postcss.config.js
├── tsconfig.json
├── src/
│   ├── app/                    # Next.js App Router
│   │   ├── layout.tsx          # Root layout
│   │   ├── page.tsx            # Landing page
│   │   ├── (auth)/             # Auth routes
│   │   │   ├── layout.tsx
│   │   │   ├── login/page.tsx
│   │   │   └── register/page.tsx
│   │   └── (dashboard)/        # Protected routes
│   │       ├── layout.tsx
│   │       ├── dashboard/page.tsx
│   │       ├── resumes/
│   │       ├── jobs/
│   │       └── generated/
│   ├── components/
│   │   ├── ui/                 # Base UI components
│   │   ├── forms/              # Form components
│   │   ├── layout/             # Layout components
│   │   ├── shared/             # Shared components
│   │   └── generated/          # Generated resume components
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   └── queries/            # React Query hooks
│   ├── providers/              # Context providers
│   ├── lib/                    # Utilities
│   ├── schemas/zod/            # Validation schemas
│   └── types/                  # TypeScript types
├── public/                     # Static assets
└── .next/                      # Next.js build output
```

## Key File Locations

| Purpose | Path |
|---------|------|
| Auth logic | `backend/src/main/java/com/resumeforge/auth/` |
| AI providers | `backend/src/main/java/com/resumeforge/ai/provider/impl/` |
| DOCX export | `backend/src/main/java/com/resumeforge/export/` |
| Frontend API hooks | `frontend/src/hooks/queries/` |
| Zod schemas | `frontend/src/schemas/zod/` |
| Flyway migrations | `backend/src/main/resources/db/migration/` |
| AI system prompt | `backend/src/main/resources/ai/system-prompt.txt` |
