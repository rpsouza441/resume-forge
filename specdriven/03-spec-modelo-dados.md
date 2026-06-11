# SPEC-03 — Modelo de Dados
## ResumeForge — Especificacao de Implementacao de Banco de Dados

**Versao:** 1.0
**Data:** 2026-06-11
**Status:** Aprovado
**Projeto:** ResumeForge — AI Resume Optimizer SaaS
**Stack:** Next.js (frontend) + Spring Boot (backend) + PostgreSQL 15+

---

## 1. Estrategia de Armazenamento

### Comparacao de Abordagens

| Aspecto | TEXT (Markdown) | Markdown puro | JSONB puro | Hibrido (adotado) |
|---|---|---|---|---|
| Legibilidade humana | Alta | Alta | Baixa | Alta |
| Busca full-text | Sim (PostgreSQL tsvector) | Sim | Limitada (paths especificos) | Sim |
| Integridade estrutural | Nenhuma | Nenhuma | Forte (JSON Schema validation) | Forte |
| Query/analise interna | Ruim | Ruim | Excelente | Boa |
| Flexibilidade de formato | Alta | Media | Alta | Alta |
| Overhead de armazenamento | Baixo | Baixo | Medio | Baixo-Medio |
| Custo de parse (aplicacao) | Baixo | Baixo | Medio | Baixo |

### [DECISAO] Hibrido: TEXT (Markdown) + JSONB

**Justificativa:**

- `content_markdown` (TEXT): documento canonico editavel e base para geracao de DOCX.
- `content_text` (TEXT): versao texto plano derivada do Markdown, usada para busca full-text e exibicao simples.
- `content_jsonb` (JSONB): estrutura parseada para queries, geracao de diferenciais e analise por campos — evita parse repetido.
- O JSONB funciona como cache estruturado do Markdown; a aplicacao decide qual campo usar conforme o caso de uso.
- Durante cadastro/geracao, os campos sao sincronizados: Markdown e fonte canonica; texto puro e JSONB sao derivados quando possivel.
- Permite migracao futura: se JSONB se tornar mais completo, Markdown continua como fallback legivel.

---

## 2. Entidade: users

```sql
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    name            VARCHAR(255)    NOT NULL,

    -- Timestamps padrao
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE
);

-- Constraints
ALTER TABLE users ADD CONSTRAINT users_email_unique UNIQUE (email);

-- Indice para busca de login
CREATE INDEX idx_users_email ON users (email);
```

---

## 3. Entidade: resume_profiles

```sql
CREATE TABLE resume_profiles (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Identificacao
    title           VARCHAR(255)    NOT NULL,
    content_text    TEXT            NOT NULL,
    content_markdown TEXT          NOT NULL,
    content_jsonb   JSONB           NOT NULL DEFAULT '{}',
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Timestamps
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

-- Indice
CREATE INDEX idx_resume_profiles_user_id ON resume_profiles (user_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_resume_profiles_user_id_title_unique
    ON resume_profiles (user_id, title) WHERE deleted_at IS NULL;
```

**Nota de implementacao para `is_default`:**
- Regra aplicada via trigger ou lock de aplicacao: ao definir um perfil como default, todos os outros do mesmo usuario devem ter `is_default = FALSE`.
- Utilizar transacao isolada com `SELECT FOR UPDATE` sobre os perfis do usuario ao alternar o flag.

---

## 4. Entidade: job_applications

```sql
CREATE TABLE job_applications (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_profile_id   UUID            REFERENCES resume_profiles(id) ON DELETE SET NULL,

    -- Informacoes da vaga
    job_title           VARCHAR(255)    NOT NULL,
    company_name        VARCHAR(255)    NOT NULL,
    job_url             TEXT,
    job_description      TEXT            NOT NULL,  -- Campo obrigatorio: job posting

    -- Metadados da vaga (extraidos ou inseridos)
    job_source          VARCHAR(50),
    job_location        VARCHAR(255),
    job_type            VARCHAR(50)     CHECK (job_type IN ('fulltime', 'parttime', 'contract', 'internship', 'remote')),
    seniority           VARCHAR(50),

    -- Status do processo
    status              VARCHAR(30)     NOT NULL DEFAULT 'saved'
                        CHECK (status IN ('saved','applied','interviewing','offer','rejected','withdrawn','archived')),
    status_changed_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Tracking
    applied_at          TIMESTAMPTZ,
    notes               TEXT,
    contact_name        VARCHAR(255),
    contact_email       VARCHAR(255),

    -- Contagem de versoes de curriculo geradas
    generated_count     INTEGER         NOT NULL DEFAULT 0,

    -- Timestamps
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

-- Indices
CREATE INDEX idx_job_applications_user_id ON job_applications (user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_applications_resume_profile_id ON job_applications (resume_profile_id) WHERE resume_profile_id IS NOT NULL;
CREATE INDEX idx_job_applications_status ON job_applications (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_applications_created_at ON job_applications (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_applications_job_title_gin ON job_applications USING gin (to_tsvector('english', job_title || ' ' || company_name));
```

---

## 5. Entidade: generated_resumes

```sql
CREATE TABLE generated_resumes (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_profile_id       UUID            NOT NULL REFERENCES resume_profiles(id) ON DELETE CASCADE,
    job_application_id      UUID            REFERENCES job_applications(id) ON DELETE CASCADE,

    -- Versao
    version_number          INTEGER         NOT NULL DEFAULT 1,
    parent_version_id       UUID            REFERENCES generated_resumes(id) ON DELETE SET NULL,
    is_current              BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Conteudo hibrido (Markdown + texto plano + JSONB)
    content_text            TEXT            NOT NULL,  -- Texto plano derivado
    content_markdown        TEXT            NOT NULL,  -- Markdown canonico para edicao e DOCX
    content_jsonb           JSONB           NOT NULL DEFAULT '{}',  -- Estrutura parseada

    -- Metadados da geracao
    ai_model                VARCHAR(100)    NOT NULL,
    ai_provider             VARCHAR(50)     NOT NULL,
    prompt_version          VARCHAR(20)     NOT NULL DEFAULT 'v1',
    generation_reason       TEXT,           -- Explicacao do que foi alterado/gerado
    status                  VARCHAR(30)     NOT NULL DEFAULT 'completed'
                            CHECK (status IN ('completed')),

    -- Controle de uso
    word_count              INTEGER,
    char_count              INTEGER,

    -- Scores (se gerados junto)
    match_score             NUMERIC(5,2),   -- % de palavras-chave casadas
    readability_score       NUMERIC(5,2),
    docx_generated_at       TIMESTAMPTZ,

    -- Timestamps
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ
);

-- Constraints
CREATE UNIQUE INDEX idx_generated_resumes_unique_current_version
    ON generated_resumes (resume_profile_id, job_application_id)
    WHERE is_current = TRUE AND deleted_at IS NULL;

ALTER TABLE generated_resumes ADD CONSTRAINT generated_resumes_version_number_positive
    CHECK (version_number >= 1);

-- Indices
CREATE INDEX idx_generated_resumes_profile_id ON generated_resumes (resume_profile_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_application_id ON generated_resumes (job_application_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_is_current ON generated_resumes (is_current) WHERE is_current = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_created_at ON generated_resumes (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_generated_resumes_content_text_search ON generated_resumes USING gin (to_tsvector('english', content_text));
CREATE INDEX idx_generated_resumes_content_jsonb ON generated_resumes USING gin (content_jsonb);
CREATE INDEX idx_generated_resumes_ai_model ON generated_resumes (ai_model, ai_provider);
```

**Regras de versionamento:**

1. `is_current = TRUE` existe para exatamente um registro por par `(resume_profile_id, job_application_id)` que nao esteja deletado.
2. Ao criar uma nova versao:
   - A aplicacao abre uma transacao.
   - Marca o registro atual como `is_current = FALSE`.
   - Insere o novo registro com `is_current = TRUE` e `version_number = anterior + 1`.
   - Atualiza `parent_version_id` do novo registro para referenciar o anterior.
3. `generated_count` em `job_applications` e incrementado via trigger `AFTER INSERT` ou mantido pela aplicacao.
4. Nao existe tabela separada de versoes no MVP: cada versao e uma linha em `generated_resumes`.

---

## 6. Entidade: analysis_reports

```sql
CREATE TABLE analysis_reports (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    generated_resume_id     UUID            NOT NULL REFERENCES generated_resumes(id) ON DELETE CASCADE,

    -- Tipo de analise
    report_type             VARCHAR(50)     NOT NULL
                            CHECK (report_type IN ('ats_score', 'keyword_analysis', 'format_analysis', 'content_quality', 'combined')),
    report_version          VARCHAR(20)     NOT NULL DEFAULT 'v1',

    -- Scores gerais
    overall_score           NUMERIC(5,2),
    ats_compatibility_score NUMERIC(5,2),

    -- Scores por dimensao (JSONB para flexibilidade)
    dimension_scores        JSONB           NOT NULL DEFAULT '{}',
    -- Estrutura: { "keyword_density": 85.5, "format_score": 90.0, "readability": 78.2, ... }

    -- Findings e recomendacoes (JSONB)
    findings                JSONB           NOT NULL DEFAULT '[]',
    recommendations         JSONB           NOT NULL DEFAULT '[]',
    -- Estrutura findings: [{ "type": "missing_keyword", "severity": "high", "field": "experience", "detail": "..." }]
    -- Estrutura recommendations: [{ "action": "add_skill", "target_field": "skills", "value": "...", "reason": "..." }]

    -- Metadata da analise
    analyzed_fields         JSONB           NOT NULL DEFAULT '[]',
    -- Ex: ["experience", "skills", "education"]

    -- Timestamps
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Indices
CREATE INDEX idx_analysis_reports_generated_resume_id ON analysis_reports (generated_resume_id);
CREATE INDEX idx_analysis_reports_report_type ON analysis_reports (report_type);
CREATE INDEX idx_analysis_reports_overall_score ON analysis_reports (overall_score DESC) WHERE overall_score IS NOT NULL;
CREATE INDEX idx_analysis_reports_created_at ON analysis_reports (created_at DESC);
CREATE INDEX idx_analysis_reports_dimension_scores ON analysis_reports USING gin (dimension_scores);
CREATE INDEX idx_analysis_reports_findings ON analysis_reports USING gin (findings);
```

---

## 7. Entidade: ai_runs

```sql
CREATE TABLE ai_runs (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    generated_resume_id     UUID            REFERENCES generated_resumes(id) ON DELETE SET NULL,

    -- Identificacao da operacao
    run_type                VARCHAR(50)     NOT NULL
                            CHECK (run_type IN ('resume_generation', 'resume_regeneration')),
    prompt_version          VARCHAR(20)     NOT NULL,

    -- Modelo e provider
    ai_model                VARCHAR(100)    NOT NULL,
    ai_provider             VARCHAR(50)     NOT NULL,

    -- Tokens (para billing e otimizacao)
    input_tokens            INTEGER,
    output_tokens           INTEGER,
    total_tokens            INTEGER GENERATED ALWAYS AS (COALESCE(input_tokens, 0) + COALESCE(output_tokens, 0)) STORED,

    -- Latencia
    latency_ms              INTEGER,

    -- Custo estimado
    estimated_cost_usd      NUMERIC(10,6),

    -- Prompt e respostas para auditoria
    prompt_text             TEXT            NOT NULL,
    raw_response            TEXT,
    parsed_response         JSONB,

    -- Contexto da requisicao (JSONB para flexibilidade)
    context                 JSONB           NOT NULL DEFAULT '{}',
    -- Estrutura: { "resume_profile_id": "...", "job_application_id": "...", "resume_version": 2, "prompt_hash": "...", ... }

    -- Resultado
    status                  VARCHAR(30)     NOT NULL DEFAULT 'started'
                            CHECK (status IN ('started','succeeded','failed')),
    success                 BOOLEAN,
    error_code              VARCHAR(50),
    error_message           TEXT,

    -- Timestamps
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ
);

-- Indices
CREATE INDEX idx_ai_runs_user_id ON ai_runs (user_id);
CREATE INDEX idx_ai_runs_generated_resume_id ON ai_runs (generated_resume_id) WHERE generated_resume_id IS NOT NULL;
CREATE INDEX idx_ai_runs_run_type ON ai_runs (run_type);
CREATE INDEX idx_ai_runs_ai_model ON ai_runs (ai_model, ai_provider);
CREATE INDEX idx_ai_runs_created_at ON ai_runs (created_at DESC);
CREATE INDEX idx_ai_runs_user_monthly ON ai_runs (user_id, created_at DESC) WHERE success = TRUE;
CREATE INDEX idx_ai_runs_context ON ai_runs USING gin (context);
CREATE INDEX idx_ai_runs_success ON ai_runs (success) WHERE success = FALSE;
```

---

## 8. Entidade: processing_logs

```sql
CREATE TABLE processing_logs (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            REFERENCES users(id) ON DELETE SET NULL,
    ai_run_id           UUID            REFERENCES ai_runs(id) ON DELETE SET NULL,
    generated_resume_id UUID            REFERENCES generated_resumes(id) ON DELETE SET NULL,

    -- Classificacao
    log_level           VARCHAR(20)     NOT NULL
                        CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL')),
    category            VARCHAR(50)     NOT NULL
                        CHECK (category IN ('ai_request', 'data_sync', 'validation', 'export', 'import', 'system', 'security', 'billing')),

    -- Mensagem e contexto
    message             TEXT            NOT NULL,
    context_data        JSONB          NOT NULL DEFAULT '{}',

    -- Origem
    source_service      VARCHAR(100),  -- 'nextjs-frontend', 'spring-boot-api', 'ai-provider'
    source_ip           VARCHAR(45),

    -- Timestamps
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Indices
CREATE INDEX idx_processing_logs_user_id ON processing_logs (user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_processing_logs_ai_run_id ON processing_logs (ai_run_id) WHERE ai_run_id IS NOT NULL;
CREATE INDEX idx_processing_logs_generated_resume_id ON processing_logs (generated_resume_id) WHERE generated_resume_id IS NOT NULL;
CREATE INDEX idx_processing_logs_log_level ON processing_logs (log_level);
CREATE INDEX idx_processing_logs_category ON processing_logs (category);
CREATE INDEX idx_processing_logs_created_at ON processing_logs (created_at DESC);
CREATE INDEX idx_processing_logs_context_data ON processing_logs USING gin (context_data);

-- Particionamento recomendado (para alto volume)
-- PARTITION BY RANGE (created_at) USING monthly partitions
-- Recomendado a partir de 1M de registros
```

**Nota:** `processing_logs` nao tem `deleted_at` — logs de auditoria sao imutaveis por exigencia de compliance.

---

## 9. Schema JSONB de Referencia

O schema abaixo define a estrutura esperada do campo `content_jsonb` em `generated_resumes`. **Este schema NAO e validado no MVP** (sem JSON Schema enforcement no PostgreSQL); a validacao acontece na camada de aplicacao.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ResumeContent",
  "description": "Estrutura padrao do conteudo de curriculo em generated_resumes.content_jsonb",
  "type": "object",
  "properties": {
    "personalInfo": {
      "type": "object",
      "properties": {
        "fullName":  { "type": "string" },
        "email":     { "type": "string", "format": "email" },
        "phone":     { "type": "string" },
        "location":  { "type": "string" },
        "linkedin":  { "type": "string", "format": "uri" },
        "portfolio": { "type": "string", "format": "uri" },
        "summary":   { "type": "string" }
      },
      "required": ["fullName", "email"]
    },
    "experience": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "jobTitle":   { "type": "string" },
          "company":    { "type": "string" },
          "location":   { "type": "string" },
          "startDate":  { "type": "string" },
          "endDate":    { "type": "string" },
          "current":    { "type": "boolean" },
          "highlights": {
            "type": "array",
            "items": { "type": "string" }
          }
        },
        "required": ["jobTitle", "company"]
      }
    },
    "education": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "degree":     { "type": "string" },
          "field":      { "type": "string" },
          "institution":{ "type": "string" },
          "location":   { "type": "string" },
          "graduationDate": { "type": "string" },
          "gpa":        { "type": "string" }
        },
        "required": ["degree", "institution"]
      }
    },
    "skills": {
      "type": "object",
      "properties": {
        "technical": {
          "type": "array",
          "items": { "type": "string" }
        },
        "soft": {
          "type": "array",
          "items": { "type": "string" }
        },
        "tools": {
          "type": "array",
          "items": { "type": "string" }
        }
      }
    },
    "certifications": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name":        { "type": "string" },
          "issuer":      { "type": "string" },
          "date":        { "type": "string" },
          "expiryDate":  { "type": "string" },
          "credentialId": { "type": "string" }
        },
        "required": ["name"]
      }
    },
    "languages": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "language": { "type": "string" },
          "proficiency": {
            "type": "string",
            "enum": ["Native", "Fluent", "Advanced", "Intermediate", "Basic"]
          }
        },
        "required": ["language"]
      }
    }
  },
  "required": ["personalInfo"]
}
```

**Exemplo de conteudo:**

```json
{
  "personalInfo": {
    "fullName": "Jane Doe",
    "email": "jane.doe@example.com",
    "phone": "+1-555-0100",
    "location": "San Francisco, CA",
    "linkedin": "https://linkedin.com/in/janedoe",
    "summary": "Senior Full-Stack Engineer with 8+ years of experience building scalable web applications."
  },
  "experience": [
    {
      "jobTitle": "Senior Software Engineer",
      "company": "TechCorp Inc.",
      "location": "San Francisco, CA",
      "startDate": "2021-03",
      "current": true,
      "highlights": [
        "Led migration of monolithic app to microservices, reducing latency by 40%",
        "Mentored 5 junior engineers and established code review practices"
      ]
    }
  ],
  "education": [
    {
      "degree": "Bachelor of Science",
      "field": "Computer Science",
      "institution": "MIT",
      "graduationDate": "2015-06"
    }
  ],
  "skills": {
    "technical": ["Python", "TypeScript", "React", "PostgreSQL", "Kubernetes"],
    "soft": ["Leadership", "Communication", "Agile"],
    "tools": ["Docker", "GitHub Actions", "Datadog"]
  },
  "certifications": [
    {
      "name": "AWS Solutions Architect",
      "issuer": "Amazon Web Services",
      "date": "2023-01"
    }
  ],
  "languages": [
    { "language": "English", "proficiency": "Native" },
    { "language": "Spanish", "proficiency": "Advanced" }
  ]
}
```

---

## 10. Resumo de Indices

| Tabela | Indice | Tipo | Colunas / Expressao | Justificativa |
|---|---|---|---|---|
| users | idx_users_email | B-tree | email | Login rápido, busca por email |
| resume_profiles | idx_resume_profiles_user_id | B-tree | user_id | Listar perfis de um usuário |
| job_applications | idx_job_applications_user_id | B-tree | user_id | Dashboard do usuario |
| job_applications | idx_job_applications_resume_profile_id | B-tree | resume_profile_id | Perfis relacionados |
| job_applications | idx_job_applications_status | B-tree | status | Filtro Kanban/kanban |
| job_applications | idx_job_applications_created_at | B-tree | created_at DESC | Ordenacao cronologica |
| job_applications | idx_job_applications_job_title_gin | GIN | to_tsvector(job_title, company_name) | Busca full-text de vagas |
| generated_resumes | idx_generated_resumes_profile_id | B-tree | resume_profile_id | Listar geracoes de um perfil |
| generated_resumes | idx_generated_resumes_application_id | B-tree | job_application_id | Historico por aplicacao |
| generated_resumes | idx_generated_resumes_is_current | B-tree | is_current (WHERE TRUE) | Acesso rapido a versao atual |
| generated_resumes | idx_generated_resumes_created_at | B-tree | created_at DESC | Timeline de geracoes |
| generated_resumes | idx_generated_resumes_content_text_search | GIN | to_tsvector(content_text) | Busca full-text em curriculos |
| generated_resumes | idx_generated_resumes_content_jsonb | GIN | content_jsonb | Queries em estrutura JSONB |
| generated_resumes | idx_generated_resumes_ai_model | B-tree | (ai_model, ai_provider) | Analise de uso de modelos |
| analysis_reports | idx_analysis_reports_generated_resume_id | B-tree | generated_resume_id | JOIN com curriculo |
| analysis_reports | idx_analysis_reports_overall_score | B-tree | overall_score DESC | Ranking de qualidade |
| analysis_reports | idx_analysis_reports_dimension_scores | GIN | dimension_scores | Busca por scores de dimensao |
| analysis_reports | idx_analysis_reports_findings | GIN | findings | Busca por tipo de finding |
| ai_runs | idx_ai_runs_user_id | B-tree | user_id | Historico de uso por usuario |
| ai_runs | idx_ai_runs_ai_model | B-tree | (ai_model, ai_provider) | Analise de custos por modelo |
| ai_runs | idx_ai_runs_user_monthly | B-tree | (user_id, created_at DESC) | Contagem mensal de runs |
| ai_runs | idx_ai_runs_context | GIN | context | Queries sobre contexto de run |
| ai_runs | idx_ai_runs_success | B-tree | success (WHERE FALSE) | Auditoria de falhas |
| processing_logs | idx_processing_logs_user_id | B-tree | user_id | Logs por usuario |
| processing_logs | idx_processing_logs_log_level | B-tree | log_level | Filtro por nivel |
| processing_logs | idx_processing_logs_category | B-tree | category | Filtro por categoria |
| processing_logs | idx_processing_logs_created_at | B-tree | created_at DESC | Consulta cronologica |
| processing_logs | idx_processing_logs_context_data | GIN | context_data | Busca em dados de contexto |

**Total: 31 indices**

---

## 11. Regras de Implementacao

### 11.1 Regra: `is_default` — apenas um por usuario

**Regra:** Em qualquer momento, apenas um `resume_profiles` pode ter `is_default = TRUE` por usuario.

**Implementacao via trigger:**

```sql
CREATE OR REPLACE FUNCTION enforce_one_default_resume_profile()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE resume_profiles
        SET is_default = FALSE
        WHERE user_id = NEW.user_id
          AND id != NEW.id
          AND is_default = TRUE
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_resume_profiles_one_default
    BEFORE INSERT OR UPDATE ON resume_profiles
    FOR EACH ROW
    WHEN (NEW.deleted_at IS NULL)
    EXECUTE FUNCTION enforce_one_default_resume_profile();
```

### 11.2 Regra: `is_current` — exatamente uma versao atual por par

**Regra:** Para cada par `(resume_profile_id, job_application_id)` ativo, apenas um `generated_resumes` pode ter `is_current = TRUE`.

**Implementacao via trigger:**

```sql
CREATE OR REPLACE FUNCTION enforce_one_current_resume_version()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_current = TRUE THEN
        UPDATE generated_resumes
        SET is_current = FALSE
        WHERE resume_profile_id = NEW.resume_profile_id
          AND job_application_id = NEW.job_application_id
          AND id != NEW.id
          AND is_current = TRUE
          AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generated_resumes_one_current
    BEFORE INSERT OR UPDATE ON generated_resumes
    FOR EACH ROW
    WHEN (NEW.deleted_at IS NULL)
    EXECUTE FUNCTION enforce_one_current_resume_version();
```

### 11.3 UUIDs gerados pelo aplicativo

**Regra:** Todos os IDs sao `UUID` gerados pela aplicacao via `gen_random_uuid()`, NAO usando `SERIAL` ou `BIGSERIAL`.

**Justificativa:**
- UUIDs sao seguros (impossivel enumerar registros por ID).
- Permite geracao offline e federacao de dados no futuro.
- Nao revela cardinalidade/existencia de registros.
- O desempenho de B-tree para UUIDs v4 e aceitavel com PostgreSQL 16+.

### 11.4 Timestamps em TIMESTAMPTZ

**Regra:** Todos os campos de timestamp usam `TIMESTAMPTZ` com valores em UTC. O timezone e armazenado no campo, nao na sessao.

**Configuracao recomendada:**

```sql
-- SESSION DEFAULT timezone
SET TIME ZONE 'UTC';
ALTER TABLE <table> ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE <table> ALTER COLUMN updated_at SET DEFAULT NOW();
```

**Para atualizacao automatica de `updated_at`:**

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar em cada tabela com updated_at
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_resume_profiles_updated_at
    BEFORE UPDATE ON resume_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_job_applications_updated_at
    BEFORE UPDATE ON job_applications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_generated_resumes_updated_at
    BEFORE UPDATE ON generated_resumes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 11.5 Soft delete

**Regra:** Registros deletados recebem `deleted_at = NOW()` em vez de DELETE fisico. Queries devem sempre incluir `WHERE deleted_at IS NULL`.

**Padrao de query:**

```sql
-- Exemplo: buscar aplicacoes ativas
SELECT * FROM job_applications
WHERE user_id = '...' AND deleted_at IS NULL;

-- Exemplo: buscar com JOIN seguro
SELECT gr.*, ja.job_title
FROM generated_resumes gr
LEFT JOIN job_applications ja ON ja.id = gr.job_application_id
WHERE gr.deleted_at IS NULL
  AND (ja.deleted_at IS NULL OR ja.id IS NULL);
```

### 11.6 JSONB — Operadores de query

```sql
-- Buscar curriculos com skill especifica
SELECT * FROM generated_resumes
WHERE content_jsonb -> 'skills' -> 'technical' @> '"Python"'::jsonb;

-- Buscar experiencias com empresa especifica
SELECT * FROM generated_resumes
WHERE content_jsonb #> '{experience,0,company}' = '"TechCorp Inc."';

-- Buscar findings de alta severidade
SELECT * FROM analysis_reports
WHERE jsonb_path_exists(findings, '$[*] ? (@.severity == "high")');

-- Aggregations em dimension_scores
SELECT
    report_type,
    AVG((dimension_scores ->> 'keyword_density')::numeric) as avg_keyword_density
FROM analysis_reports
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY report_type;
```

---

## 12. Resumo de Triggers

| Trigger | Tabela | Evento | Funcao | Proposito |
|---|---|---|---|---|
| trg_users_updated_at | users | BEFORE UPDATE | update_updated_at_column | Auto-update updated_at |
| trg_resume_profiles_one_default | resume_profiles | BEFORE INSERT/UPDATE | enforce_one_default_resume_profile | Apenas um default por usuario |
| trg_resume_profiles_updated_at | resume_profiles | BEFORE UPDATE | update_updated_at_column | Auto-update updated_at |
| trg_job_applications_updated_at | job_applications | BEFORE UPDATE | update_updated_at_column | Auto-update updated_at |
| trg_generated_resumes_one_current | generated_resumes | BEFORE INSERT/UPDATE | enforce_one_current_resume_version | Apenas uma versao atual |
| trg_generated_resumes_updated_at | generated_resumes | BEFORE UPDATE | update_updated_at_column | Auto-update updated_at |

---

## 13. Plano de Execucao de Migration

### Fase 1 — Schema base (sem triggers, indices ou constraints)
```sql
-- Ordem: tabelas sem dependencia primeiro
CREATE TABLE users (...);
CREATE TABLE resume_profiles (...);
CREATE TABLE job_applications (...);
CREATE TABLE generated_resumes (...);
CREATE TABLE analysis_reports (...);
CREATE TABLE ai_runs (...);
CREATE TABLE processing_logs (...);
```

### Fase 2 — Constraints (FK apos todas tabelas)
```sql
ALTER TABLE resume_profiles ADD CONSTRAINT ...;
ALTER TABLE job_applications ADD CONSTRAINT ...;
-- etc.
```

### Fase 3 — Indices
```sql
CREATE INDEX ...;  -- 31 indices
```

### Fase 4 — Triggers
```sql
CREATE OR REPLACE FUNCTION ...;
CREATE TRIGGER ...;
-- ordem: functions antes de triggers
```

### Fase 5 — Constraints extras (UNIQUE, CHECK)
```sql
CREATE UNIQUE INDEX idx_generated_resumes_unique_current_version ...;
CREATE UNIQUE INDEX idx_resume_profiles_user_id_title_unique ...;
```

---

*Documento de referencia para implementacao. Qualquer alteracao de schema deve ser versionada via migration (Flyway ou Liquibase) e referenciada neste documento.*
