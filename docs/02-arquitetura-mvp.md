# Arquitetura do MVP

## Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                    NAVEGADOR (Browser)                       │
│                   [Next.js Frontend]                          │
│                  Porta: 3000 (padrão)                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/REST (JSON)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 SPRING BOOT API BACKEND                      │
│                  Porta: 8080 (padrão)                        │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │   Auth   │  │  Resume   │  │   Job    │  │Generation│    │
│  │  Module  │  │  Module   │  │  Module  │  │ Module   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐     │
│  │  Export  │  │  Logging  │  │  AI Abstraction      │     │
│  │  Module  │  │  Module   │  │  Layer (Strategy)    │     │
│  └──────────┘  └──────────┘  └──────────┬───────────┘     │
└─────────────────────────┬──────────────────┼─────────────────┘
                          │                  │
              ┌───────────┘                  └───────────────┐
              ▼                                            ▼
┌─────────────────────────┐              ┌──────────────────────────┐
│       PostgreSQL        │              │    AI Provider           │
│   Porta: 5432 (padrão)  │              │  (Gemini | OpenRouter |  │
│                         │              │   OpenAI | Claude)       │
│  Fonte única da verdade │              └──────────────────────────┘
│  TEXT + JSONB           │
│  Histórico de versões   │
└─────────────────────────┘
```

---

## Frontend — Next.js

### Por que Next.js

Next.js é a escolha natural para o ecossistema React quando o objetivo é um sistema web com rotas, formulários e comunicação com API backend. As alternativas (Vite, Remix, Angular) apresentam trade-offs que pesam contra no contexto deste MVP:

- **Vite**: excelente bundler, mas não fornece estrutura de rotas nem SSR — seria necessário adicionar React Router manualmente e perder as vantagens de server-side rendering.
- **Remix**: arquitetura excelente e conceitualmente mais próxima do Next.js do que o Vite, mas o ecossistema de bibliotecas (React Query, NextAuth, etc.) é menos maduro no contexto brasileiro. Next.js tem mais recursos de deployment (Vercel, qualquer VPS) e mais documentação em português.
- **Angular**: over-engineering para um MVP; TypeScript obrigatório sem benefício proporcional no frontend.

### Estrutura de Pastas do Frontend

```
src/
├── app/                    # App Router (Next.js 13+)
│   ├── (auth)/
│   │   ├── login/
│   │   └── register/
│   ├── (dashboard)/
│   │   ├── page.tsx        # Dashboard
│   │   ├── resumes/
│   │   ├── jobs/
│   │   ├── generated/
│   │   └── history/
│   ├── api/                # API routes (BFF se necessário)
│   ├── layout.tsx
│   └── globals.css
├── components/
│   ├── ui/                 # Componentes reutilizáveis (Button, Input, etc.)
│   ├── forms/              # Formulários (ResumeForm, JobForm, etc.)
│   └── layout/             # Header, Footer, Sidebar
├── hooks/                  # Custom hooks (useAuth, useResumes, etc.)
├── lib/
│   ├── api.ts              # Cliente HTTP (axios ou fetch wrapper)
│   └── utils.ts            # Funções utilitárias
├── types/                  # TypeScript types (matching backend schemas)
└── providers/              # Context providers (AuthProvider, etc.)
```

### Server vs. Client Components

- **Server Components**: páginas que fazem fetch inicial de dados (dashboard, lista de currículos, detalhe de currículo). Acesso direto ao backend via fetch server-side.
- **Client Components**: formulários, estados interativos, loading states, toasts. Usam React Query para mutations e fetch.

---

## Backend — Java/Spring Boot

### Por que Java/Spring Boot

Java com Spring Boot é a escolha que oferece melhor relação entre maturidade, type safety e ecossistema para um MVP que precisa de:

- **Confiabilidade**: Spring Boot é utilizado em produção por milhares de empresas; bibliotecas são maduras e bem testadas.
- **Type safety**: Java é statically typed, o que reduz erros em runtime e facilita refactoring. No contexto de MVP, isso significa menos bugs surfando após deploy.
- **DI robusto**: Spring Boot's IoC container facilita arquitetura modular sem service locator pattern ou manual wiring.
- **Ecossistema de IA em crescimento**: Spring AI (da VMware, agora Broadcom) oferece abstrações para principais provedores de IA. Mesmo sem Spring AI, a comunidade Java tem bibliotecas maduras para integração com OpenAI, Gemini, etc.
- **Java vs. Node.js**: Node.js seria uma escolha legítima, mas Java oferece melhor type safety em projetos de domínio complexo (currículo com seções, versões, análise de aderência). O sistema tem lógica de negócio não trivial; TypeScript não oferece o mesmo nível de segurança em runtime que Java.
- **Java vs. Python**: Python com FastAPI ou Flask seria mais rápido para prototipar, mas adicionaria uma segunda runtime (Python) ao stack. O objetivo é manter o MVP simples com uma única linguagem server-side. Python também adicionaria complexidade de deployment (venv, gunicorn, etc.) sem benefício proporcional no MVP.

### Estrutura de Pastas do Backend (Maven/Gradle)

```
src/main/java/com/resumegen/
├── ResumeGenApplication.java
├── config/
│   ├── SecurityConfig.java         # Spring Security + JWT
│   ├── CorsConfig.java
│   └── AiProviderConfig.java      # Configuration properties for AI
├── auth/
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── dto/LoginRequest.java
│   ├── dto/RegisterRequest.java
│   ├── dto/AuthResponse.java
│   └── security/JwtTokenProvider.java
├── resume/
│   ├── controller/ResumeController.java
│   ├── service/ResumeService.java
│   ├── entity/ResumeProfile.java
│   └── repository/ResumeProfileRepository.java
├── job/
│   ├── controller/JobController.java
│   ├── service/JobService.java
│   ├── entity/JobApplication.java
│   └── repository/JobApplicationRepository.java
├── generation/
│   ├── controller/GenerationController.java
│   ├── service/GenerationService.java
│   ├── dto/GenerationRequest.java
│   ├── dto/GenerationResponse.java
│   ├── entity/GeneratedResume.java
│   ├── entity/AnalysisReport.java
│   └── entity/AiRun.java
├── export/
│   ├── controller/ExportController.java
│   ├── service/DocxGenerationService.java
│   └── converter/MarkdownToDocxConverter.java
├── ai/
│   ├── provider/AiProvider.java              # Interface
│   ├── provider/impl/GeminiAiProvider.java
│   ├── provider/impl/OpenAiProvider.java
│   ├── provider/impl/ClaudeAiProvider.java
│   ├── provider/impl/OpenRouterProvider.java
│   └── service/AiOrchestrationService.java
├── logging/
│   ├── entity/ProcessingLog.java
│   └── service/LoggingService.java
└── exception/
    ├── GlobalExceptionHandler.java
    └── custom exceptions
```

### Por que não Worker Python no MVP

Uma alternativa frequentemente considerada é usar Python (FastAPI ou worker Celery) para processamento de IA, deixando Java apenas para a API web. Esta abordagem adiciona:

- **Duas runtimes** para manter, fazer deploy e monitorar.
- **Complexidade de comunicação** entre serviços (queue, HTTP, etc.).
- **Custo operacional** (máquina separada ou container extra).
- **Sincronização** de estado entre serviços.

No MVP, a geração de currículo é uma operação síncrona (usuário espera o resultado) — não há necessidade de processamento em background. Java com WebClient ou RestTemplate consegue chamar qualquer provedor de IA de forma equivalente ao Python. A única justificativa para um worker Python seria se o parsing de PDF (com Apache Tika ou pdfplumber) exigisse bibliotecas Python específicas — mas isso é fase 3, não MVP.

---

## PostgreSQL como Fonte da Verdade

### Comparação de Estratégias de Storage

| Aspecto | PostgreSQL como fonte | Pasta local (fs) | Bucket/Storage (S3/MinIO) |
|---------|----------------------|------------------|--------------------------|
| Complexidade de setup | Baixa (uma variável DATABASE_URL) | Baixa | Alta (credenciais, SDK, policies) |
| Custo | Custo do banco (já existe) | Zero | Custo de storage + egress (real em produção) |
| Backup | Rotinas nativas de backup do PostgreSQL | Manual (scripts) | Delegado ao provedor, mas precisa de restore procedure |
| Versionamento | Via tabela (is_current, parent_version_id) | Nomenclatura de arquivo (fragil) | Versões de objeto (novamente, complexidade adicional) |
| Controle de acesso | Permissões PostgreSQL (RLS futura) | Permissões de arquivo (perigoso) | IAM policies (boas práticas, mas mais para aprender) |
| Consistência transacional | ACID garantido pelo PostgreSQL | Nenhuma | Eventual consistency (pode gerar inconsistências) |
| Compleksidade de restore | `pg_restore` único | Scripts manuais por arquivo |複合法 different per provider |
| Adequação ao MVP | **Perfeita** | Frágil (um rm -rf e perdemos tudo) | Excesso (MVP não tem volume que justifique) |

### Conclusão: PostgreSQL como única fonte da verdade

No MVP, o volume de dados é baixo (centenas de usuários, milhares de currículos), e o banco já é a dependency obrigatória para dados relacionais. Adicionar storage de arquivos traz complexidade de configuration, custo monetário e synchronization issues sem nenhum benefício tangível neste estágio.

A decisão pode mudar na Fase 4, quando o volume de uploads (PDF, DOCX) justificar o custo e a complexidade de um storage dedicado.

---

## Integração com IA — Abstração por Strategy Pattern

### Arquitetura da Abstração

```
                    ┌─────────────────────────────┐
                    │   AiOrchestrationService    │
                    │   (conhece apenas a interface)│
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │      AiProvider            │  ← Interface
                    │  + generateOptimizedResume │
                    │  + generateAnalysis        │
                    │  + validateOutput          │
                    │  + estimateCost            │
                    └──────────────┬──────────────┘
                                   │
          ┌────────────┬───────────┼───────────┬────────────┐
          │            │           │           │            │
    ┌─────▼────┐ ┌────▼────┐ ┌───▼────┐ ┌───▼────┐ ┌─────▼─────┐
    │  Gemini   │ │ OpenAI  │ │Claude  │ │OpenRouter│ │Future     │
    │ Provider │ │Provider │ │Provider │ │Provider │ │Providers  │
    └──────────┘ └─────────┘ └────────┘ └────────┘ └───────────┘
```

### Como funciona na prática

1. O arquivo `application.yml` define qual provedor usar:
   ```yaml
   ai:
     provider: openrouter  # Trocar para "gemini", "openai", etc.
     model: meta-llama/llama-3-8b-instruct  # Modelo específico
     api-key: ${AI_API_KEY}
     timeout-seconds: 30
   ```

2. `AiProviderConfig` cria o bean do provedor correto em startup.
3. `GenerationService` chama `aiProvider.generateOptimizedResume(...)` — sem saber qual provedor está configurado.
4. Para trocar de provedor: mudar uma linha no YAML, sem mudança de código.

### Contrato da Interface

```text
AiProvider (interface)
  ├── getProviderId(): String           — identificador único do provedor
  ├── getDefaultModel(): String         — modelo padrão do provedor
  ├── generateOptimizedResume(
  │     baseResume: ResumeContent,
  │     jobDescription: String,
  │     systemPrompt: String,
  │     options: GenerationOptions
  │   ): GenerationResult              — gera currículo otimizado
  ├── generateAnalysis(
  │     resumeContent: String,
  │     jobDescription: String,
  │     options: GenerationOptions
  │   ): AnalysisResult                 — gera análise de aderência
  └── estimateCost(
  │     inputTokens: Int,
  │     outputTokens: Int
  │   ): BigDecimal                     — estima custo em USD
```

---

## Geração de .docx Sob Demanda

### Por que não salvar o binário

As alternativas são:
1. **Salvar binário no banco** (BLOB): complica o backup (dump SQL maior), cria dados redundantes (conteúdo já existe como texto), pode ficar desatualizado se o currículo for editado depois.
2. **Salvar em storage externo** (S3/MinIO): custo adicional (bucket + egress), complexidade de configuration, synchronization entre banco e storage.
3. **Gerar sob demanda**: arquivo sempre atual, backup simples, zero storage overhead.

### Como funciona

1. Usuário clica "Baixar DOCX" no frontend.
2. Frontend chama `GET /api/generated/{id}/docx`.
3. Backend busca `content_markdown` do `generated_resumes`.
4. `MarkdownToDocxConverter` transforma markdown → XWPF (Apache POI).
5. Response stream com `Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document` e `Content-Disposition: attachment`.
6. Timestamp `docx_generated_at` atualizado no banco.

### Critérios para migrar para storage de arquivos (Fase 4)

- Volume de downloads > 1.000 por dia (geração sob demanda começa a pesar).
- Tempo médio de geração de .docx > 5 segundos (indicando custo de CPU excessivo).
- Necessidade de auditoria do arquivo exato enviado pelo candidato.
- Necessidade de versões "congeladas" que não mudam se o currículo no banco for editado.

---

## Responsabilidades por Camada

| Camada | Responsabilidades |
|--------|------------------|
| **Frontend (Next.js)** | Renderização de UI, validação de formulários, gerenciamento de estado server (React Query), chamadas de API, download de arquivos, roteamento |
| **Backend (Spring Boot)** | Lógica de negócio, validação de input, acesso ao banco, orquestração de IA, geração de .docx, autenticação, autorização |
| **Banco (PostgreSQL)** | Persistência de todos os dados, integridade referencial, índices para performance, backup e restore |
| **Provedor de IA** | Processamento puro (geração de texto) — sem estado, sem persistência |
| **Storage de arquivos** | Nenhum no MVP — geração sob demanda |

---

## Componentes Principais e Suas Responsabilidades

| Módulo | Responsabilidade | Dependências |
|--------|-----------------|-------------|
| `auth` | JWT issuance/validation, password hashing, user registration | users table |
| `resume` | CRUD de currículo base, gerenciamento de padrão | users table |
| `job` | CRUD de vagas, extração de metadados | users table |
| `generation` | Orchestração de IA, parsing de resposta, salvamento de resultado | resume, job, ai modules |
| `export` | Conversão markdown→DOCX, streaming de resposta HTTP | generation module |
| `ai` | Abstração de provider, retry logic, error handling | provedor específico |
| `logging` | Registro de ai_runs e processing_logs | todos os módulos |