# Modelo de Dados

## Estratégia de Armazenamento de Currículo

A decisão mais importante do modelo de dados é como armazenar o conteúdo do currículo — que existe em três formatos simultâneos:

- **Texto puro**: conteúdo legível, sem formatação.
- **Markdown**: conteúdo com formatação leve (headings, bullets, bold).
- **JSONB**: conteúdo estruturado em formato machine-readable.

### Comparação das Abordagens

| Aspecto | TEXT puro | Markdown TEXT | JSONB puro | TEXT + JSONB híbrido |
|---------|-----------|---------------|------------|----------------------|
| Simplicidade de schema | Alta | Média | Baixa | Média |
| Flexibilidade | Baixa | Média | Alta | Alta |
| Capacidade de query | LIKE apenas | LIKE + padrão | JSONB queries | Híbrida |
| Facilidade de parsing por IA | Mais difícil (texto livre) | Médio | Fácil | Fácil (JSONB) |
| Suporte à edição manual | Mais difícil (reescrever tudo) | Média (editar texto) | Precisa de UI estruturada | Melhor (dois formatos) |
| Geração de .docx | Precisa converter de volta | Direta (parser Markdown→DOCX) | Precisa serializar | Direta (do Markdown) |
| Custo de migração | Baixo | Baixo | Alto (reescrever tudo) | Médio |
| Adequação ao MVP | **Ruim** | **Razoável** | **Excesso** | **Melhor** |

### Recomendação: TEXT (Markdown) + JSONB Híbrido

**Rationale:**

O formato `TEXT` puro (sem nenhuma estrutura) é inadequado porque:
- Não permite edição programática (só substituição total).
- Não permite diff entre versões (texto completo, sem granularidade).
- Geração de .docx exige parsear estrutura de volta de texto livre.

O formato `JSONB` puro seria excesso para o MVP porque:
- Requer validação de schema (JSON Schema ou similar) — custo de implementação.
- Requer UI de edição estruturada (não apenas um textarea) — tempo de desenvolvimento.
- Validação rigorosa de schema pode bloquear conteúdo legítimo que não segue o schema esperado.

**A abordagem híbrida oferece o melhor de cada mundo:**

- **`content_markdown` (TEXT)**: serve como "view model" — usado para display no frontend e para geração de .docx. É o formato que o usuário vê e edita.
- **`content_jsonb` (JSONB)**: serve como "domain model" — usado para processamento por IA (envio estruturado para o provedor), para funcionalidades futuras de edição estruturada e para análise programática.

O Markdown é escolhido como formato textual porque:
- É legível por humanos e machines.
- Converte bem para .docx sem perdas.
- Editores de texto simples suportam.
- É mais próximo do que a IA gera naturalmente.

---

## Tabela: users

**Finalidade**: armazenar dados de autenticação dos usuários.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| id | UUID | Sim | PK, gerado pelo aplicativo |
| email | VARCHAR(255) | Sim | UNIQUE, login do usuário |
| password_hash | VARCHAR(255) | Sim | BCrypt hash, nunca o texto puro |
| name | VARCHAR(255) | Sim | Nome completo |
| created_at | TIMESTAMPTZ | Sim | Data de criação da conta |
| updated_at | TIMESTAMPTZ | Sim | Data da última atualização |
| last_login_at | TIMESTAMPTZ | Não | Data do último login |
| is_active | BOOLEAN | Sim | Default true; soft delete futuro |

**Índices**:
- UNIQUE INDEX on `email`

**Observações**:
- Não armazenar password em texto puro, nunca.
- `last_login_at` não é crítico no MVP mas é útil para detecção de contas abandonadas.

---

## Tabela: resume_profiles

**Finalidade**: armazenar o currículo base do usuário (cadastrado uma vez, usado para múltiplas gerações).

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| id | UUID | Sim | PK |
| user_id | UUID | Sim | FK → users.id |
| title | VARCHAR(255) | Sim | Título do currículo (ex: "Currículo geral", "Dev Java") |
| content_text | TEXT | Não | Conteúdo em texto puro (sem formatação) |
| content_markdown | TEXT | Não | Conteúdo em Markdown (para display e DOCX) |
| content_jsonb | JSONB | Não | Conteúdo estruturado (para IA e edição programática) |
| is_default | BOOLEAN | Sim | Se é o currículo padrão do usuário |
| created_at | TIMESTAMPTZ | Sim | Data de criação |
| updated_at | TIMESTAMPTZ | Sim | Data da última atualização |

**Índices**:
- INDEX on `user_id`
- INDEX on `user_id` WHERE `is_default = true` (partial index, se suportado)

**Relacionamentos**:
- `user_id` → `users.id` (um usuário tem muitos perfis de currículo)

**Observações**:
- `is_default` só um por usuário deve ser true — lógica no service layer.
- `content_jsonb` pode ser NULL no MVP se o usuário colar texto livre sem passar pelo formulário estruturado.
- `content_markdown` é gerado automaticamente quando o usuário salva via formulário estruturado.

---

## Tabela: job_applications

**Finalidade**: armazenar a descrição da vaga de emprego que o usuário cola para análise.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| id | UUID | Sim | PK |
| user_id | UUID | Sim | FK → users.id |
| company_name | VARCHAR(255) | Não | Nome da empresa |
| job_title | VARCHAR(255) | Sim | Título da vaga |
| location | VARCHAR(255) | Não | Localidade (cidade/UF, remoto, híbrido) |
| employment_type | VARCHAR(100) | Não | CLT, PJ, freelancer, estágio, trainee |
| seniority | VARCHAR(100) | Não | Júnior, Pleno, Sênior, Lead, Staff |
| job_description_raw | TEXT | Sim | Descrição completa da vaga (colada pelo usuário) |
| source_url | VARCHAR(500) | Não | URL de origem da vaga (LinkedIn, Gupy, etc.) |
| status | VARCHAR(50) | Sim | active, closed, archived |
| created_at | TIMESTAMPTZ | Sim | Data de criação |
| updated_at | TIMESTAMPTZ | Sim | Data da última atualização |

**Índices**:
- INDEX on `user_id`
- INDEX on `company_name`
- INDEX on `created_at` (para ordenação cronológica)
- INDEX on `status` (para filtrar vagas ativas/arquivadas)

**Relacionamentos**:
- `user_id` → `users.id` (um usuário tem muitas vagas)

**Observações**:
- `company_name` e `job_title` são extraídos/informados pelo usuário; `job_description_raw` é o texto completo.
- `seniority` pode ser inferida pela IA no futuro; no MVP é informada pelo usuário.
- `status` permite ao usuário arquivar vagas sem deletar (soft archive).

---

## Tabela: generated_resumes

**Finalidade**: armazenar o currículo gerado pela IA para uma combinação específica de currículo base + vaga. É a tabela central do sistema.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| id | UUID | Sim | PK |
| user_id | UUID | Sim | FK → users.id |
| resume_profile_id | UUID | Sim | FK → resume_profiles.id |
| job_application_id | UUID | Sim | FK → job_applications.id |
| version_number | INTEGER | Sim | Número da versão (1, 2, 3...) |
| parent_version_id | UUID | Não | FK → generated_resumes.id (versão anterior) |
| content_text | TEXT | Não | Currículo gerado em texto puro |
| content_markdown | TEXT | Não | Currículo gerado em Markdown |
| content_jsonb | JSONB | Não | Currículo gerado estruturado |
| docx_generated_at | TIMESTAMPTZ | Não | Quando o .docx foi gerado pela última vez |
| is_current | BOOLEAN | Sim | Se é a versão atual desta linha (resume + vaga) |
| created_at | TIMESTAMPTZ | Sim | Data de geração |
| updated_at | TIMESTAMPTZ | Sim | Data da última edição manual |

**Índices**:
- INDEX on `user_id`
- INDEX on `(resume_profile_id, job_application_id)` — busca de histórico por par
- INDEX on `is_current` — busca da versão atual
- INDEX on `created_at` — ordenação cronológica

**Relacionamentos**:
- `user_id` → `users.id`
- `resume_profile_id` → `resume_profiles.id`
- `job_application_id` → `job_applications.id`
- `parent_version_id` → `generated_resumes.id` (auto-relacionamento para cadeia de versões)

**Regra de versionamento**:
- `is_current` é `true` para exatamente uma linha por par `(resume_profile_id, job_application_id)`.
- Quando uma nova versão é criada: a anterior recebe `is_current = false`.
- `version_number` incrementa a cada nova versão.

**Observações**:
- `docx_generated_at` não indica que o arquivo foi salvo — apenas quando foi gerado pela última vez.
- O conteúdo completo de todas as versões é preservado — não há soft delete de versões.

---

## Tabela: analysis_reports

**Finalidade**: armazenar a análise de aderência returned pela IA, separada do currículo gerado. Permite consultar a análise mesmo se o currículo for editado depois.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| id | UUID | Sim | PK |
| generated_resume_id | UUID | Sim | FK → generated_resumes.id |
| adherence_score | INTEGER | Não | Score 0-100 (nullable se análise falhar) |
| adherence_level | VARCHAR(50) | Não | high, medium, low |
| key_strengths | TEXT | Não | Pontos fortes identificados pela IA |
| gaps | TEXT | Não | Lacunas identificadas |
| keyword_map | JSONB | Não | Mapa de keywords: {required: [], preferred: [], missing: [], matched: []} |
| positioning_strategy | TEXT | Não | Estratégia de posicionamento recomendada pela IA |
| raw_ai_response | TEXT | Sim | Resposta bruta completa da IA (texto returned pelo provedor) |
| created_at | TIMESTAMPTZ | Sim | Data de criação |

**Índices**:
- INDEX on `generated_resume_id` (UNIQUE se cada generated_resume tem uma análise)

**Relacionamentos**:
- `generated_resume_id` → `generated_resumes.id` (um para um)

**Observações**:
- `raw_ai_response` é crítico para debugging — sempre salvar o que a IA retornou.
- Se a IA retornar JSON estruturado, o `raw_ai_response` contém o JSON original (antes do parsing).
- `adherence_score` pode ser null se a IA não fornecer score (tratado como "indeterminado").

---

## Tabela: ai_runs

**Finalidade**: log de cada chamada feita ao provedor de IA. Essencial para debugging, billing e auditoria.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| id | UUID | Sim | PK |
| generated_resume_id | UUID | Não | FK → generated_resumes.id (nullable se chamada falhou antes de gerar) |
| provider | VARCHAR(100) | Sim | openai, gemini, claude, openrouter |
| model | VARCHAR(200) | Sim | gpt-4o, gemini-2.0-flash, claude-3-5-sonnet, etc. |
| prompt_used | TEXT | Sim | Prompt completo enviado (system + user) |
| tokens_used | INTEGER | Não | Total de tokens (input + output), se retornado pelo provedor |
| cost_usd | DECIMAL(10,6) | Não | Custo aproximado em USD |
| duration_ms | INTEGER | Não | Tempo de execução em milissegundos |
| status | VARCHAR(50) | Sim | success, error, timeout |
| error_message | TEXT | Não | Mensagem de erro se status != success |
| created_at | TIMESTAMPTZ | Sim | Data/hora da chamada |

**Índices**:
- INDEX on `generated_resume_id`
- INDEX on `provider` (para análise de custo por provedor)
- INDEX on `created_at` (para relatórios)
- INDEX on `status` (para identificar falhas)

**Observações**:
- `cost_usd` é uma estimativa baseada em `tokens_used` e tabela de preços do provedor — não é o valor real cobrado (que pode variar por plano).
- Múltiplas `ai_runs` podem existir por `generated_resume_id` se houve retry.
- `prompt_used` é a versão final (com variáveis substituídas), não o template.

---

## Tabela: processing_logs

**Finalidade**: log geral de operações do sistema, para auditoria e debugging. Complementa `ai_runs` com eventos que não são chamadas de IA.

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| id | UUID | Sim | PK |
| user_id | UUID | Não | FK → users.id (nullable para logs de sistema) |
| operation | VARCHAR(100) | Sim | Nome da operação (ex: RESUME_CREATED, JOB_DELETED) |
| entity_type | VARCHAR(100) | Não | Tipo de entidade afetada (resume_profile, job_application, etc.) |
| entity_id | UUID | Não | ID da entidade afetada |
| log_level | VARCHAR(20) | Sim | INFO, WARN, ERROR |
| message | TEXT | Não | Mensagem legível |
| metadata | JSONB | Não | Dados extras em formato livre |
| created_at | TIMESTAMPTZ | Sim | Data/hora do evento |

**Índices**:
- INDEX on `(user_id, created_at)` — busca de logs por usuário
- INDEX on `operation`
- INDEX on `created_at`

**Observações**:
- Esta tabela é para logs de aplicação, não logs de infraestrutura (use ELK/Datadog para logs de servidor).
- `log_level` permite filtrar apenas erros ou avisos para revisão.
- `metadata` pode conter dados contextuais (ex: {"resumeId": "...", "version": 2}).

---

## Schema JSONB de Referência — content_jsonb

Este schema define a estrutura esperada para o campo `content_jsonb` em `resume_profiles` e `generated_resumes`. **Não é validado no MVP** — é um guia para o parsing da IA e para funcionalidades futuras.

```json
{
  "personalInfo": {
    "name": "string",
    "email": "string",
    "phone": "string",
    "location": "string (cidade/UF)",
    "linkedin": "string (URL)",
    "github": "string (URL)",
    "portfolio": "string (URL)"
  },
  "summary": "string (3-5 linhas)",
  "experience": [
    {
      "company": "string",
      "role": "string",
      "startDate": "string (MM/AAAA)",
      "endDate": "string (MM/AAAA) | null",
      "current": "boolean",
      "description": "string (descrição do papel)",
      "achievements": ["string (conquistas mensuráveis)"]
    }
  ],
  "education": [
    {
      "institution": "string",
      "degree": "string (Bacharel, Tecnólogo, etc.)",
      "field": "string (área do conhecimento)",
      "startDate": "string (AAAA)",
      "endDate": "string (AAAA) | null",
      "status": "string (Concluído, Cursando, Trancado)"
    }
  ],
  "skills": {
    "technical": ["string"],
    "soft": ["string"],
    "languages": [
      {
        "language": "string (ex: Inglês)",
        "level": "string (ex: Avançado, Fluente)"
      }
    ]
  },
  "certifications": [
    {
      "name": "string",
      "issuer": "string",
      "date": "string (AAAA-MM)",
      "url": "string (URL, opcional)"
    }
  ]
}
```

**Nota de implementação**:
- Este schema NÃO é validado no MVP (JSONB aceita qualquer estrutura).
- O frontend pode usar validação Zod para garantir estrutura ao salvar.
- O backend não valida schema — confia no frontend.
- Na Fase 3+, adicionar validação de schema JSONB (JSON Schema ou Bean Validation).

---

## Resumo de Índices Recomendados

| Tabela | Índice | Tipo | Justificativa |
|--------|--------|------|--------------|
| users | `email` | UNIQUE | Busca por login |
| resume_profiles | `user_id` | B-tree | Listar currículos do usuário |
| job_applications | `user_id` | B-tree | Listar vagas do usuário |
| job_applications | `company_name` | B-tree | Filtragem por empresa |
| job_applications | `created_at` | B-tree | Ordenação cronológica |
| generated_resumes | `user_id` | B-tree | Listar gerações do usuário |
| generated_resumes | `(resume_profile_id, job_application_id)` | B-tree | Histórico por par |
| generated_resumes | `is_current` | B-tree | Buscar versão atual |
| analysis_reports | `generated_resume_id` | B-tree | Join com geração |
| ai_runs | `provider` | B-tree | Análise de custo |
| ai_runs | `created_at` | B-tree | Relatórios |
| processing_logs | `(user_id, created_at)` | B-tree | Logs por usuário |