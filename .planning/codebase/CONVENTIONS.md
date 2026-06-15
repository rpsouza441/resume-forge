---
name: CONVENTIONS
description: Code style, naming conventions, and patterns
last_mapped_commit: HEAD
---

# CONVENTIONS — Code Style & Patterns

**Date:** 2026-06-15  
**Focus:** Quality

## Backend (Java/Spring Boot)

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `ResumeController`, `UserService` |
| Methods | camelCase | `findById`, `createResume` |
| Variables | camelCase | `resumeProfile`, `userId` |
| Constants | SCREAMING_SNAKE | `MAX_RETRY_COUNT` |
| Packages | lowercase | `com.resumeforge.auth` |
| DTOs | PascalCase + suffix | `CreateResumeRequest`, `ResumeResponse` |
| Entities | PascalCase | `User`, `ResumeProfile` |
| Repositories | PascalCase + suffix | `UserRepository`, `ResumeProfileRepository` |

### Package Structure Pattern
```
module/
├── controller/    # REST endpoints
├── dto/           # Data transfer objects
├── entity/         # JPA entities
├── repository/     # Data access
├── service/        # Business logic
└── exception/      # Module-specific exceptions
```

### Error Handling
- Custom exceptions extend `RuntimeException`
- `GlobalExceptionHandler` handles all exceptions
- Exception pattern: `ResourceNotFoundException`, `ValidationException`, `UnauthorizedException`, `ForbiddenException`, `ConflictException`, `AiGenerationException`

### REST API Patterns
- `@RestController` for API controllers
- `@RequestMapping` with versioned paths (`/api/auth`, `/api/resumes`)
- DTOs for request/response (no entity exposure)
- `@Valid` for bean validation

### Security Patterns
- JWT token in `Authorization: Bearer <token>` header
- `JwtAuthenticationFilter` extracts and validates token
- `SecurityContext` holds authenticated user
- `SecurityUtils` for current user access

## Frontend (TypeScript/React)

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Components | PascalCase | `ResumeForm.tsx`, `NavBar.tsx` |
| Hooks | camelCase + use prefix | `useAuth.ts`, `useResumes.ts` |
| Utilities | camelCase | `utils.ts`, `formatters.ts` |
| Types | PascalCase | `Resume`, `JobApplication` |
| Schemas | camelCase | `resume.schemas.ts` |
| CSS classes | kebab-case (Tailwind) | `flex items-center gap-4` |

### File Organization
```
components/
├── ui/           # Base components (Button, Input, Card)
├── forms/        # Form components (ResumeForm, JobForm)
├── layout/       # Layout components (Sidebar, NavBar)
└── shared/       # Cross-cutting components
```

### React Patterns
- Server state via React Query hooks
- Auth state via React Context
- Forms via React Hook Form + Zod
- Components are functional with hooks
- Props interfaces defined in same file or `types/`

### API Integration
- Axios instance in `lib/api.ts`
- JWT interceptor attaches token
- React Query hooks in `hooks/queries/`
- Query keys for cache management

### Styling
- Tailwind CSS for all styling
- `clsx` + `tailwind-merge` for conditional classes
- Radix UI for accessible primitives
- Custom components wrap Radix primitives

## Git Conventions

### Commit Messages
- Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`
- Example: `feat(resume): add version history tracking`

### Branch Naming
- Feature: `feature/description`
- Bugfix: `fix/description`
- Hotfix: `hotfix/description`

## Database Conventions

### Flyway Migration Naming
```
V{version}__{description}.sql
V1__initial_schema.sql
V2__drop_job_type_constraint.sql
```

### Entity Conventions
- `@Entity` with `@Table` name
- `@Id` with `@GeneratedValue`
- JPA auditing fields where applicable
