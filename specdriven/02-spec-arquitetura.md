# SPEC-02 — Arquitetura do Sistema

**Projeto**: ResumeForge — Gerador de Currículos Otimizados por IA
**Versão**: 1.0.0
**Status**: Confirmado para Implementação
**Última Atualização**: 2026-06-11

---

## 1. Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NAVEGADOR (Browser)                                 │
│                        [Next.js 14+ Frontend]                              │
│                       Porta: 3000 (desenvolvimento)                        │
└──────────────────────────────┬────────────────────────────────────────────┘
                               │ HTTP/REST (JSON)
                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT API BACKEND                                │
│                      Porta: 8080 (desenvolvimento)                         │
│                                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │  Auth    │  │  Resume  │  │   Job    │  │Generation│  │  Export  │     │
│  │ Module   │  │ Module   │  │ Module   │  │ Module   │  │  Module  │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
│                                                                             │
│  ┌──────────┐  ┌────────────────────────────┐  ┌──────────────────────┐    │
│  │ Logging  │  │   Exception Handler        │  │  AI Abstraction      │    │
│  │ Module   │  │   (GlobalExceptionHandler) │  │  Layer (Strategy)     │    │
│  └──────────┘  └────────────────────────────┘  └──────────┬───────────┘    │
└─────────────────────────────┬──────────────────────┬─────┴───────────────────┘
                              │                      │
              ┌───────────────┘                      └───────────────┐
              ▼                                              ▼
┌──────────────────────────────┐              ┌──────────────────────────────────┐
│         PostgreSQL 15+       │              │        AI Provider               │
│     Porta: 5432 (padrão)     │              │  (Gemini | OpenRouter |          │
│                              │              │   OpenAI | Claude)               │
│  Fonte única da verdade      │              └──────────────────────────────────┘
│  TEXT + JSONB                │
│  Histórico de versões       │
└──────────────────────────────┘
```

**Princípio Arquitetural Central**:

> Conteúdo no banco. Arquivos gerados sob demanda.

Este princípio fundamenta todas as decisões de armazenamento e processamento do MVP.

---

## 2. Stack Tecnológico

| Camada | Tecnologia | Versão | Justificativa |
|--------|-----------|--------|--------------|
| **Frontend** | Next.js (App Router) | 14.x+ | React Server Components eliminam hydration overhead; roteamento integrado; deploy simples (Vercel ou VPS). App Router é obrigatório — Pages Router não é aceito para este projeto. |
| **Linguagem Frontend** | TypeScript | 5.x | Type safety reduz erros em runtime; IntelliSense melhora DX; interfaces compartilháveis com backend. |
| **Estado de Servidor** | TanStack Query (React Query) | 5.x | Cache automático, background refetch, optimistic updates, loading/error states padronizados. |
| **Validação de Schema** | Zod | 3.x | Validação de formulários e respostas de API; integração nativa com React Hook Form via zodResolver. |
| **Formulários** | React Hook Form | 7.x | Performance superior a formulários controlados; não re-renderiza em keystroke; uso simples de `useForm`. |
| **Estilização** | Tailwind CSS | 3.x | Utilidades; sem CSS-in-JS overhead; documentação extensa; curva de aprendizado curta. |
| **Backend** | Java 17 LTS | 17+ | LTS, type safety robusto, DI nativo do Spring, ecossistema maduro. |
| **Framework Backend** | Spring Boot | 3.x | Convention over configuration; Spring Security para JWT; Spring Data JPA para acesso a dados. |
| **Build Tool** | Maven | 3.9+ | Padrão de mercado no ecossistema Java; gerenciador de dependências maduro. Alternativa: Gradle Kotlin DSL se a equipe preferir. |
| **Banco de Dados** | PostgreSQL | 15+ | Suporte nativo a JSONB; ACID; extensões geoespaciais disponíveis; custo zero em self-hosted. |
| **ORM** | Spring Data JPA + Hibernate | 6.x | Abstrai SQL; migrations via Flyway; schemas derivados de entidades Java. |
| **Migrations** | Flyway | 10.x | Versionamento de schema SQL; tracking de alterações; rollback quando necessário. |
| **Geração de .docx** | Apache POI (XWPF) | 5.x | Biblioteca Java pura; sem dependência externa; conversao markdown→DOCX sob demanda. |
| **HTTP Client (Backend)** | Spring WebClient | built-in | Reativo; timeout configurável; retry nativo; substitui RestTemplate em novos projetos. |
| **Abstração de IA** | Strategy Pattern (interface customizada) | — | Camada proprietária com interface `AiProvider`; provedores plugáveis via configuração. |
| **Autenticação** | Spring Security + JWT (jjwt) | 0.12.x | Stateless; BCrypt para senhas; access token 1h; refresh token opcional na Fase 2. |
| **Containerização** | Docker + Docker Compose | 24.x | Reproducibilidade de ambiente; desenvolvimento local padronizado; staging idêntico a produção. |
| **CI/CD** | GitHub Actions | — | Gratuito para repositórios públicos/privados; marketplace rico; YAML declarativo. |

---

## 3. Frontend — Next.js App Router

### 3.1 Estrutura de Pastas

```
src/
├── app/                          # App Router — todas as rotas da aplicação
│   ├── (auth)/                   # Grupo de rotas autenticadas
│   │   ├── login/
│   │   │   └── page.tsx
│   │   └── register/
│   │       └── page.tsx
│   ├── (dashboard)/              # Grupo de rotas do painel (protegidas)
│   │   ├── dashboard/
│   │   │   └── page.tsx
│   │   ├── resumes/
│   │   │   ├── page.tsx              # lista de currículos base
│   │   │   ├── new/
│   │   │   │   └── page.tsx          # criação de currículo base
│   │   │   └── [id]/
│   │   │       └── edit/
│   │   │           └── page.tsx     # edição de currículo base
│   │   ├── jobs/
│   │   │   ├── page.tsx              # lista de vagas
│   │   │   ├── new/
│   │   │   │   └── page.tsx          # wizard de criação de vaga + geração
│   │   │   └── [id]/
│   │   │       └── page.tsx          # detalhe da vaga
│   │   ├── generated/
│   │   │   ├── page.tsx              # histórico de currículos gerados
│   │   │   ├── new/
│   │   │   │   └── page.tsx          # geração direta (pós-criação de vaga)
│   │   │   └── [id]/
│   │   │       ├── page.tsx          # detalhe do currículo gerado
│   │   │       └── edit/
│   │   │           └── page.tsx     # editor de currículo gerado
│   │   └── layout.tsx               # layout autenticado (navbar + sidebar)
│   ├── api/                        # API Routes (BFF para cenários específicos)
│   ├── layout.tsx                  # Root Layout (providers, fonts, global styles)
│   ├── page.tsx                    # Redirecionamento (→ /login ou /dashboard)
│   └── globals.css                 # Reset + variáveis CSS + Tailwind
├── components/
│   ├── ui/                         # Componentes primitivos (Button, Input, Badge, Card, Modal, Toast)
│   ├── forms/                      # Formulários reutilizáveis (ResumeForm, JobForm, LoginForm, RegisterForm)
│   └── layout/                     # Shell, Sidebar, Navbar, PageHeader, Footer
├── hooks/
│   ├── useAuth.ts                  # Hook de autenticação (token, user, login, logout)
│   ├── queries/                    # Queries React Query por entidade
│   │   ├── useResumes.ts
│   │   ├── useJobs.ts
│   │   └── useGenerated.ts
│   └── mutations/                  # Mutations React Query por entidade
│       ├── useCreateResume.ts
│       ├── useUpdateResume.ts
│       └── useGenerateResume.ts
├── lib/
│   ├── api.ts                      # Cliente HTTP (axios ou fetch wrapper com interceptors)
│   ├── utils.ts                    # Funções utilitárias (formatDate, cn(), etc.)
│   └── constants.ts                # Constantes da aplicação (API_BASE_URL, etc.)
├── types/
│   ├── auth.types.ts               # LoginRequest, RegisterRequest, AuthResponse, User
│   ├── resume.types.ts             # ResumeProfile, ResumeFormData
│   ├── job.types.ts                # JobApplication, JobFormData
│   ├── generated.types.ts          # GeneratedResume, GenerationResponse, AnalysisReport
│   └── api.types.ts                # ApiError, PaginatedResponse, etc.
├── providers/
│   ├── AuthProvider.tsx            # Context: token, user, login, logout, isAuthenticated
│   ├── QueryProvider.tsx           # Configura QueryClient no root layout
│   └── ToastProvider.tsx           # Sistema de notificações toast
└── schemas/
    └── zod/                        # Schemas Zod compartilhados com backend
        ├── auth.schemas.ts
        ├── resume.schemas.ts
        └── generation.schemas.ts
```

### 3.2 Server vs. Client Components — Regra Obrigatória

```
┌─────────────────────────────────────────────────────┐
│               REGRADE FRONTEIRA                     │
│                                                     │
│  Server Components = dados do servidor (SSR)       │
│  Client Components = interatividade (useState,     │
│                      useEffect, handlers)           │
└─────────────────────────────────────────────────────┘
```

**Server Components** (sem `"use client"`):
- Páginas de listagem (`page.tsx`): fetch inicial via `fetch` nativo no Server Component.
- Páginas de detalhe: carregam dados uma única vez no servidor.
- Componentes de display puro: Cards, Tables, Badges que só recebem props.

**Client Components** (com `"use client"` no topo):
- Formulários (`LoginForm`, `ResumeForm`, `JobForm`).
- Componentes com estado interativo (modais, accordions, tabs).
- Componentes que disparam mutations React Query.
- Qualquer componente que use `useState`, `useEffect`, event handlers.

**Regra prática**: se o componente precisa de `useState`, `useEffect`, `onClick`, `onChange`, ou interage com React Query mutations → `"use client"`. Caso contrário, permanece Server Component.

### 3.3 React Query — Configuração de QueryClient

```typescript
// providers/QueryProvider.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,    // 5 minutos para listas
      staleTime: 0,                 // 0 para dados que mudam frequentemente
      retry: 2,
      refetchOnWindowFocus: true,  // refetch após 30s inativo
    },
    mutations: {
      onError: (error) => {
        // toast de erro via ToastProvider
      },
    },
  },
});
```

### 3.4 Zod — Validação de Resposta e Formulários

```typescript
// Exemplo: schema de currículo
import { z } from 'zod';

export const resumeSchema = z.object({
  title: z.string().min(1, 'Título é obrigatório').max(255),
  contentMarkdown: z.string().min(50, 'Currículo muito curto (mínimo 50 caracteres)'),
  isDefault: z.boolean().optional(),
});

export type ResumeFormData = z.infer<typeof resumeSchema>;
```

### 3.5 React Hook Form — Padrão de Uso

```typescript
const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
  resolver: zodResolver(resumeSchema),
  defaultValues: { title: '', contentMarkdown: '' },
});
```

### 3.6 Tailwind CSS — Configuração

```javascript
// tailwind.config.js — extensão não necessária para MVP
// Configuração padrão Next.js + Tailwind é suficiente.
// Adicionar design tokens customizados se necessário:
module.exports = {
  theme: {
    extend: {
      colors: {
        brand: { 500: '#3B82F6', 600: '#2563EB' },
      },
    },
  },
};
```

---

## 4. Backend — Java 17+ / Spring Boot 3

### 4.1 Estrutura de Pastas (Maven)

```
resume-forge-backend/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── resumeforge/
│       │           ├── ResumeForgeApplication.java       # Main class
│       │           │
│       │           ├── config/
│       │           │   ├── SecurityConfig.java          # Spring Security + JWT filter chain
│       │           │   ├── CorsConfig.java              # CORS para frontend (porta 3000)
│       │           │   ├── AiProviderConfig.java        # Bean do provedor ativo via YAML
│       │           │   └── WebClientConfig.java         # WebClient com timeouts
│       │           │
│       │           ├── auth/
│       │           │   ├── controller/
│       │           │   │   └── AuthController.java      # POST /api/auth/login, /register
│       │           │   ├── service/
│       │           │   │   └── AuthService.java         # lógica de auth, JWT issuance
│       │           │   ├── dto/
│       │           │   │   ├── LoginRequest.java        # {email, password}
│       │           │   │   ├── RegisterRequest.java     # {name, email, password}
│       │           │   │   └── AuthResponse.java        # {accessToken, user: {id, name, email}}
│       │           │   ├── entity/
│       │           │   │   └── User.java                 # JPA entity → tabela users
│       │           │   ├── repository/
│       │           │   │   └── UserRepository.java
│       │           │   └── security/
│       │           │       ├── JwtTokenProvider.java    # generate, validate, extract claims
│       │           │       ├── JwtAuthenticationFilter.java
│       │           │       └── UserDetailsServiceImpl.java
│       │           │
│       │           ├── resume/
│       │           │   ├── controller/
│       │           │   │   └── ResumeController.java    # CRUD /api/resumes
│       │           │   ├── service/
│       │           │   │   └── ResumeService.java       # lógica, is_default enforcement
│       │           │   ├── entity/
│       │           │   │   └── ResumeProfile.java       # JPA entity → resume_profiles
│       │           │   ├── repository/
│       │           │   │   └── ResumeProfileRepository.java
│       │           │   └── dto/
│       │           │       ├── CreateResumeRequest.java
│       │           │       ├── UpdateResumeRequest.java
│       │           │       └── ResumeResponse.java
│       │           │
│       │           ├── job/
│       │           │   ├── controller/
│       │           │   │   └── JobController.java       # CRUD /api/jobs
│       │           │   ├── service/
│       │           │   │   └── JobService.java
│       │           │   ├── entity/
│       │           │   │   └── JobApplication.java      # JPA entity → job_applications
│       │           │   ├── repository/
│       │           │   │   └── JobApplicationRepository.java
│       │           │   └── dto/
│       │           │       ├── CreateJobRequest.java
│       │           │       └── JobResponse.java
│       │           │
│       │           ├── generation/
│       │           │   ├── controller/
│       │           │   │   └── GenerationController.java  # POST /api/generate, GET /generated/{id}
│       │           │   ├── service/
│       │           │   │   └── GenerationService.java      # orchestration principal
│       │           │   ├── entity/
│       │           │   │   ├── GeneratedResume.java       # JPA → generated_resumes
│       │           │   │   ├── AnalysisReport.java        # JPA → analysis_reports
│       │           │   │   └── AiRun.java                 # JPA → ai_runs
│       │           │   ├── repository/
│       │           │   │   ├── GeneratedResumeRepository.java
│       │           │   │   ├── AnalysisReportRepository.java
│       │           │   │   └── AiRunRepository.java
│       │           │   └── dto/
│       │           │       ├── GenerationRequest.java      # {resumeProfileId, jobApplicationId}
│       │           │       ├── GenerationResponse.java    # {generatedResumeId, version, analysisSummary}
│       │           │       └── GeneratedResumeResponse.java
│       │           │
│       │           ├── export/
│       │           │   ├── controller/
│       │           │   │   └── ExportController.java       # GET /api/generated/{id}/docx
│       │           │   ├── service/
│       │           │   │   └── DocxGenerationService.java   # ORCHESTRATOR de exportação
│       │           │   └── converter/
│       │           │       └── MarkdownToDocxConverter.java # Parser markdown → XWPF
│       │           │
│       │           ├── ai/
│       │           │   ├── provider/
│       │           │   │   ├── AiProvider.java            # INTERFACE do Strategy
│       │           │   │   ├── GenerationOptions.java      # {temperature, maxTokens, timeout, modelOverride}
│       │           │   │   ├── GenerationResult.java       # {success, rawResponse, parsedResponse, tokens, cost, duration, error}
│       │           │   │   ├── CostEstimate.java
│       │           │   │   └── impl/
│       │           │   │       ├── GeminiAiProvider.java    # Google Gemini API
│       │           │   │       ├── OpenAiProvider.java     # OpenAI Chat Completions
│       │           │   │       ├── ClaudeAiProvider.java   # Anthropic Messages API
│       │           │   │       └── OpenRouterProvider.java # OpenRouter universal
│       │           │   └── service/
│       │           │       └── AiOrchestrationService.java # Caller da interface, retry, fallback
│       │           │
│       │           ├── logging/
│       │           │   ├── entity/
│       │           │   │   └── ProcessingLog.java         # JPA → processing_logs
│       │           │   ├── repository/
│       │           │   │   └── ProcessingLogRepository.java
│       │           │   └── service/
│       │           │       └── LoggingService.java        # log operacional
│       │           │
│       │           └── exception/
│       │               ├── GlobalExceptionHandler.java   # @RestControllerAdvice
│       │               ├── ResourceNotFoundException.java
│       │               ├── UnauthorizedException.java
│       │               ├── ForbiddenException.java
│       │               ├── AiGenerationException.java
│       │               └── ValidationException.java
│       │
│       └── resources/
│           ├── application.yml               # Configuração principal
│           ├── application-dev.yml          # Overrides para desenvolvimento
│           ├── application-prod.yml         # Overrides para produção
│           └── db/
│               └── migration/
│                   └── V1__initial_schema.sql  # Flyway migration
│
└── src/test/
    └── java/com/resumeforge/
        ├── auth/
        │   └── AuthServiceTest.java
        ├── resume/
        │   └── ResumeServiceTest.java
        ├── generation/
        │   └── GenerationServiceTest.java
        ├── export/
        │   └── MarkdownToDocxConverterTest.java
        └── ai/
            └── AiOrchestrationServiceTest.java
```

### 4.2 Módulos e Suas Responsabilidades

| Módulo | Pacote Base | Responsabilidade | Principais Classes |
|--------|------------|-----------------|-------------------|
| **auth** | `com.resumeforge.auth` | Autenticação JWT, registro de usuários, password hashing BCrypt | `AuthController`, `AuthService`, `JwtTokenProvider`, `User` |
| **resume** | `com.resumeforge.resume` | CRUD do currículo base, gestão de padrão (`is_default`), conversão de formato | `ResumeController`, `ResumeService`, `ResumeProfile` |
| **job** | `com.resumeforge.job` | CRUD de vagas, extração de metadados da descrição, status (active/closed/archived) | `JobController`, `JobService`, `JobApplication` |
| **generation** | `com.resumeforge.generation` | Orquestração de IA, parsing de resposta, versionamento, salvamento | `GenerationController`, `GenerationService`, `GeneratedResume`, `AnalysisReport`, `AiRun` |
| **export** | `com.resumeforge.export` | Conversão markdown→DOCX via Apache POI, streaming de arquivo HTTP | `ExportController`, `DocxGenerationService`, `MarkdownToDocxConverter` |
| **ai** | `com.resumeforge.ai` | Abstração Strategy, retry logic, fallback, normalização de API | `AiProvider` (interface), `GeminiAiProvider`, `OpenAiProvider`, `ClaudeAiProvider`, `OpenRouterProvider`, `AiOrchestrationService` |
| **logging** | `com.resumeforge.logging` | Log operacional de todas as operações do sistema | `ProcessingLog`, `LoggingService` |
| **exception** | `com.resumeforge.exception` | Tratamento centralizado de erros, mapping para HTTP status codes | `GlobalExceptionHandler`, exceções customizadas |

---

## 5. Banco de Dados — PostgreSQL 15+

### 5.1 Estratégia TEXT + JSONB Híbrido

O PostgreSQL é a **única fonte da verdade** do sistema. Todo conteúdo textual existe em dois formatos simultâneos:

- **`content_text` (TEXT)**: texto puro legível, usado para diff e busca.
- **`content_markdown` (TEXT)**: formato com estrutura leve (headings, bullets, bold), usado para display no frontend e geração de .docx.
- **`content_jsonb` (JSONB)**: estrutura machine-readable, usada para envio à IA e edição programática futura.

**Princípio**: fonte única da verdade — o banco armazena tudo; .docx é gerado sob demanda a partir do `content_markdown`.

### 5.2 Lista de Tabelas e IDs

| Tabela | PK (tipo) | Estratégia de ID | Descrição |
|--------|-----------|-----------------|-----------|
| `users` | UUID | `gen_random_uuid()` via Java (Application-managed) | Autenticação e perfil do usuário |
| `resume_profiles` | UUID | `gen_random_uuid()` via Java | Currículo base do usuário |
| `job_applications` | UUID | `gen_random_uuid()` via Java | Vaga de emprego cadastrada |
| `generated_resumes` | UUID | `gen_random_uuid()` via Java | Currículo gerado + versões |
| `analysis_reports` | UUID | `gen_random_uuid()` via Java | Análise de aderência da IA |
| `ai_runs` | UUID | `gen_random_uuid()` via Java | Log de cada chamada à IA |
| `processing_logs` | UUID | `gen_random_uuid()` via Java | Log operacional geral |

### 5.3 Diagrama de Entidades (Simplificado)

```
users ──────────────────────────────────────────────┐
  │  (1:N)                                         │
  ├─→ resume_profiles ──────────────────────────────┤
  │  (1:N)        │                                │
  │               │                                │
  │        generated_resumes ──────────────────────┤
  │               │         │                      │
  │               │         │ (1:1)               │
  │               │         ↓                      │
  │               │  analysis_reports              │
  │               │                                │
  │               ├─→ ai_runs (log de IA)         │
  │               │                                │
  │               └─→ processing_logs (log ops)   │
  │                                                 │
  └─→ job_applications ─────────────────────────────┘
          (1:N)
          │
          └──→ generated_resumes
                  (cada vaga pode ter múltiplas versões de currículo)
```

### 5.4 Índices Recomendados

| Tabela | Índice | Tipo | Justificativa |
|--------|--------|------|---------------|
| `users` | `email` | UNIQUE | Busca por login |
| `resume_profiles` | `user_id` | B-tree | Listar currículos do usuário |
| `job_applications` | `user_id` | B-tree | Listar vagas do usuário |
| `job_applications` | `created_at` | B-tree | Ordenação cronológica |
| `job_applications` | `status` | B-tree | Filtrar ativas/arquivadas |
| `generated_resumes` | `user_id` | B-tree | Listar gerações do usuário |
| `generated_resumes` | `(resume_profile_id, job_application_id)` | B-tree | Histórico por par |
| `generated_resumes` | `is_current` | B-tree | Buscar versão atual |
| `generated_resumes` | `created_at` | B-tree | Ordenação cronológica |
| `analysis_reports` | `generated_resume_id` | B-tree | Join com geração |
| `ai_runs` | `generated_resume_id` | B-tree | Log de IA por geração |
| `ai_runs` | `provider` | B-tree | Análise de custo por provedor |
| `ai_runs` | `created_at` | B-tree | Relatórios |
| `ai_runs` | `status` | B-tree | Identificar falhas |
| `processing_logs` | `(user_id, created_at)` | B-tree | Logs por usuário |

---

## 6. Abstração de IA — Strategy Pattern

### 6.1 Diagrama da Arquitetura

```
                           ┌──────────────────────────────────┐
                           │     GenerationService            │
                           │  (apenas conhece a interface)    │
                           └──────────────────┬───────────────┘
                                              │
                           ┌──────────────────▼───────────────┐
                           │          AiProvider              │  ← Interface (contrato)
                           │  + getProviderId(): String      │
                           │  + getDefaultModel(): String     │
                           │  + generate(prompt, options):    │
                           │    GenerationResult              │
                           │  + estimateCost(tokens):         │
                           │    CostEstimate                  │
                           └──────┬───────────┬───────┬───────┘
                                  │           │       │
            ┌─────────────────────┘           │       └─────────────────────┐
            │                                   │                               │
      ┌─────▼─────┐  ┌──────────┐  ┌────────▼──────┐  ┌──────────▼─────────┐
      │  Gemini    │  │  OpenAI  │  │    Claude     │  │    OpenRouter      │
      │ Provider  │  │ Provider │  │   Provider    │  │     Provider       │
      │  (Google) │  │(OpenAI)  │  │(Anthropic)   │  │ (Agregador)       │
      └───────────┘  └──────────┘  └───────────────┘  └────────────────────┘
```

### 6.2 Interface AiProvider

```java
// com.resumeforge.ai.provider.AiProvider
public interface AiProvider {

    /**
     * Identificador único do provedor.
     * Usado para logging, auditoria e seleção.
     * Valores: "gemini", "openai", "anthropic", "openrouter"
     */
    String getProviderId();

    /**
     * Modelo padrão deste provedor.
     * Pode ser sobrescrito por options em tempo de chamada.
     */
    String getDefaultModel();

    /**
     * Gera conteúdo otimizado a partir do prompt.
     * @param systemPrompt prompt de sistema (regras, constraints)
     * @param userPrompt   prompt do usuário (currículo + vaga)
     * @param options      opções de geração (temperatura, maxTokens, timeout, modelo override)
     * @return GenerationResult com resposta, metadados e custo
     */
    GenerationResult generate(String systemPrompt, String userPrompt, GenerationOptions options);

    /**
     * Estima custo em USD com base na contagem de tokens.
     */
    CostEstimate estimateCost(int inputTokens, int outputTokens);
}
```

### 6.3 Provedores Suportados

| Provedor | ID | Modelos Recomendados | Tier | Limite Rate |
|----------|----|---------------------|------|-------------|
| Google Gemini | `gemini` | `gemini-2.0-flash`, `gemini-1.5-pro` | Gratuito (generoso) | 15 req/min (free) |
| OpenAI | `openai` | `gpt-4o-mini`, `gpt-4o` | Pago (pay-as-you-go) | 60 req/min (tier padrão) |
| Anthropic Claude | `anthropic` | `claude-3-5-sonnet-20241022`, `claude-3-haiku-20240307` | Pago | 50 req/min (tier padrão) |
| OpenRouter | `openrouter` | `meta-llama/llama-3-8b-instruct`, `mistralai/mistral-7b-instruct` | Variado (free + pago) | Varia por modelo |

### 6.4 Configuração via application.yml

```yaml
# application.yml
spring:
  application:
    name: resume-forge

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/resumeforge}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate        # Flyway gerencia o schema
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# AI Provider Configuration
ai:
  provider: ${AI_PROVIDER:gemini}                        # Trocar para trocar provedor
  model: ${AI_MODEL:gemini-2.0-flash}                    # Modelo específico
  api-key: ${AI_API_KEY:}
  timeout-seconds: ${AI_TIMEOUT:90}
  temperature: ${AI_TEMPERATURE:0.3}
  max-tokens: ${AI_MAX_TOKENS:8192}
  # Fallback: provider secundário se o primário falhar
  fallback-provider: ${AI_FALLBACK_PROVIDER:}
  fallback-model: ${AI_FALLBACK_MODEL:}

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:}
  expiration-ms: ${JWT_EXPIRATION:3600000}                # 1 hora

# Server Configuration
server:
  port: ${SERVER_PORT:8080}

# CORS
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

**Trocar de provedor**: alterar `AI_PROVIDER` e `AI_API_KEY` — sem mudança de código na aplicação.

### 6.5 Retry e Fallback — AiOrchestrationService

```java
@Service
public class AiOrchestrationService {

    private final AiProvider primaryProvider;
    private final AiProvider fallbackProvider;
    private final LoggingService loggingService;

    public GenerationResult generate(String systemPrompt, String userPrompt, GenerationOptions options) {
        GenerationResult result = attempt(primaryProvider, systemPrompt, userPrompt, options);
        if (result.isSuccess()) return result;

        if (fallbackProvider != null && options.isRetryEnabled()) {
            log.warn("Primary provider failed, attempting fallback...");
            result = attempt(fallbackProvider, systemPrompt, userPrompt, options);
        }

        if (!result.isSuccess()) {
            throw new AiGenerationException("All AI providers failed: " + result.getErrorMessage());
        }
        return result;
    }

    private GenerationResult attempt(AiProvider provider, String systemPrompt, String userPrompt, GenerationOptions options) {
        try {
            return provider.generate(systemPrompt, userPrompt, options);
        } catch (Exception e) {
            return GenerationResult.failure(provider.getProviderId(), e.getMessage());
        }
    }
}
```

---

## 7. Responsabilidades por Camada

| Camada | Responsabilidades | O que NÃO faz |
|--------|-----------------|---------------|
| **Frontend (Next.js)** | Renderização de UI, validação de formulários no cliente, gerenciamento de estado de servidor (React Query), chamadas de API REST, download de arquivos via blob, roteamento, proteção de rotas autenticadas | Lógica de negócio, validação de segurança, acesso direto ao banco |
| **Backend (Spring Boot)** | Lógica de negócio, validação de input rigorosa, acesso ao banco via JPA, orquestração de IA, parsing de respostas, geração de .docx sob demanda, autenticação JWT, verificação de ownership, rate limiting | Renderização de UI, gestão de estado no cliente |
| **Banco (PostgreSQL)** | Persistência de todos os dados, integridade referencial (FK), índices para performance de queries, constraints (NOT NULL, UNIQUE), backup e restore, ACID transactions | Processamento de lógica, formatação de resposta |
| **Provedor de IA** | Processamento puro de texto (geração de currículo e análise) — stateless, sem persistência | Armazenamento, validação, formatação de saída |
| **Storage de arquivos** | Nenhum no MVP — .docx é gerado sob demanda a partir do banco | Armazenamento de binários |

---

## 8. Módulos e Dependências

| Módulo | Responsabilidade | Dependências Externas | Dependências Internas |
|--------|-----------------|---------------------|---------------------|
| `auth` | Emissão/validação de JWT, registro de usuários, hashing de senhas (BCrypt) | Spring Security, jjwt | — |
| `resume` | CRUD de currículo base, gestão de padrão, validação de conteúdo | Spring Data JPA | — |
| `job` | CRUD de vagas, extração de metadados, status management | Spring Data JPA | — |
| `generation` | Orquestração de IA, parsing de resposta estruturada, versionamento, salvamento ACID | `ai` (AiOrchestrationService) | `resume`, `job` |
| `export` | Conversão markdown→DOCX (Apache POI XWPF), streaming HTTP | Apache POI 5.x | `generation` (dados) |
| `ai` | Abstração de provider, retry, fallback, normalização de API | WebClient (built-in), bibliotecas específicas por provedor | — |
| `logging` | Log operacional de todas as operações, auditoria | Spring Data JPA | Todos os módulos |
| `exception` | Tratamento centralizado de erros, mapping para HTTP codes | Spring MVC | Todos os módulos |

---

## 9. Por Que NÃO Worker Python no MVP

### 9.1 Comparação de Arquiteturas

| Aspecto | Java/Spring Boot Only (escolhido) | Java API + Python Worker |
|---------|----------------------------------|-------------------------|
| **Runtimes para manter** | 1 (JVM) | 2 (JVM + Python) |
| **Complexidade de deploy** | Baixa (um JAR + DB) | Alta (dois serviços + message queue) |
| **Comunicação entre serviços** | N/A (chamada direta) | HTTP/REST ou queue (Redis/RabbitMQ) |
| **Custo operacional** | 1 serviço | 2 serviços, maior infraestrutura |
| **Geração de currículo** | Síncrona (usuário espera) | Síncrona via worker (mesmo resultado, mais complexo) |
| **Parsing de PDF (futuro)** | Apache Tika (JVM) | pdfplumber (Python) — única justificativa real |
| **Custo de CPU** | WebClient para chamadas de IA | Igual — IA é chamada externa |
| **Maturidade do ecossistema** | Spring AI ou WebClient com OkHttp | FastAPI + Celery |
| **Debugging** | Uma stack (Java) | Duas stacks (Java + Python) |

### 9.2 Justificativa Técnica

A única razão válida para introduzir um worker Python seria o **parsing de PDF** (uploads de currículo em PDF do usuário), que no MVP não existe. O fluxo do MVP é:

1. Usuário cola texto/markdown no formulário.
2. Backend monta prompt.
3. Backend chama provedor de IA via HTTP (WebClient) — **identico em Java ou Python**.
4. Backend recebe resposta, parseia, salva no banco.
5. Backend gera .docx sob demanda via Apache POI — **nativo Java**.

Não há nenhuma etapa que exija bibliotecas Python específicas. O Apache POI fornece toda a capacidade de geração de .docx em Java. A etapa de IA é chamada HTTP pura — qualquer linguagem com cliente HTTP faz isso.

### 9.3 Decisão Confirmada

> **D08 — Sem worker Python no MVP**: Java com Spring Boot é suficiente para todas as etapas do MVP. A introdução de uma segunda runtime não traz nenhum benefício mensurável e adiciona complexidade significativa de deployment, debugging e manutenção.

---

## 10. Decisões Arquiteturais Confirmadas (D01–D14)

| ID | Decisão | Status | Data |
|----|---------|--------|------|
| **D01** | PostgreSQL como fonte da verdade | Confirmada | 2026-06-11 |
| **D02** | Armazenamento TEXT (Markdown) + JSONB híbrido para currículos | Confirmada | 2026-06-11 |
| **D03** | .docx gerado sob demanda (não persistido) | Confirmada | 2026-06-11 |
| **D04** | Java 17+ / Spring Boot 3 como backend | Confirmada | 2026-06-11 |
| **D05** | Next.js (App Router) como frontend | Confirmada | 2026-06-11 |
| **D06** | Apache POI (XWPF) para geração de .docx | Confirmada | 2026-06-11 |
| **D07** | Strategy Pattern para abstração de IA | Confirmada | 2026-06-11 |
| **D08** | Sem worker Python no MVP | Confirmada | 2026-06-11 |
| **D09** | Sem multi-tenant no MVP | Confirmada | 2026-06-11 |
| **D10** | Sem storage externo (S3/MinIO) no MVP | Confirmada | 2026-06-11 |
| **D11** | Sem validação de schema JSONB no MVP | Confirmada | 2026-06-11 |
| **D12** | Versionamento via `version_number` + `parent_version_id` | Confirmada | 2026-06-11 |
| **D13** | PostgreSQL como fonte da verdade (vs. pasta local vs. bucket) | Confirmada | 2026-06-11 |
| **D14** | Geração sob demanda de .docx (vs. salvar binário no banco vs. storage) | Confirmada | 2026-06-11 |

---

## Apêndice A — Mapeamento de Arquivos de Documentação

| Este documento | Fonte em `docs/` |
|----------------|-------------------|
| Seção 1 — Visão Geral | `02-arquitetura-mvp.md` (seção "Visão Geral da Arquitetura") |
| Seção 2 — Stack Tecnológico | `07-frontend.md` + `02-arquitetura-mvp.md` |
| Seção 3 — Frontend | `07-frontend.md` |
| Seção 4 — Backend | `02-arquitetura-mvp.md` (seção "Backend") |
| Seção 5 — Banco de Dados | `03-modelo-dados.md` |
| Seção 6 — Abstração de IA | `02-arquitetura-mvp.md` + `05-regras-ia.md` |
| Seção 7 — Responsabilidades | `02-arquitetura-mvp.md` (seção "Responsabilidades por Camada") |
| Seção 8 — Módulos | `02-arquitetura-mvp.md` (seção "Componentes Principais") |
| Seção 9 — Worker Python | `02-arquitetura-mvp.md` (seção "Por que não Worker Python") |
| Seção 10 — Decisões | `11-riscos-decisoes.md` (seção "Decisões Tomadas") |

---

## Apêndice B — Variáveis de Ambiente Obrigatórias

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `DATABASE_URL` | JDBC connection string PostgreSQL | `jdbc:postgresql://localhost:5432/resumeforge` |
| `DATABASE_USERNAME` | Usuário do banco | `postgres` |
| `DATABASE_PASSWORD` | Senha do banco | `secret` |
| `AI_PROVIDER` | Provedor ativo (`gemini`, `openai`, `anthropic`, `openrouter`) | `gemini` |
| `AI_API_KEY` | Chave da API do provedor | `AIza...` |
| `AI_MODEL` | Modelo específico (opcional, override) | `gemini-2.0-flash` |
| `AI_TIMEOUT` | Timeout em segundos | `90` |
| `JWT_SECRET` | Secret para assinatura JWT (min 256 bits) | `sua-chave-super-secreta-com-256-bits-minimo` |
| `JWT_EXPIRATION` | Expiração do access token em ms | `3600000` |
| `CORS_ALLOWED_ORIGINS` | Origens CORS separadas por vírgula | `http://localhost:3000,https://resume-forge.vercel.app` |
| `SERVER_PORT` | Porta do backend | `8080` |

---

*Este documento é parte do conjunto SPEC-driven para o projeto ResumeForge. Alterações arquiteturais devem ser documentadas como nova decisão (D15, D16...) neste arquivo e no documento `11-riscos-decisoes.md`.*