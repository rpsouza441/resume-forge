# API REST — Referência Técnica do Backend

Este documento descreve a API REST do sistema de geração de currículos otimizados. Todos os endpoints seguem convenções REST, utilizam JSON como formato de interchange, e requerem autenticação via JWT Bearer Token, exceto onde explicitado.

**Base URL:** `https://api.cvotimizze.com.br/api` (produção)
**Autenticação:** Bearer JWT (exceto `/auth/register` e `/auth/login`)

---

## Autenticação

### POST /api/auth/register

**Finalidade:** Criar uma nova conta de usuário no sistema.

**Autenticação necessária:** Não.

**Request Body:**
```json
{
  "name": "string (required, 2-100 characters)",
  "email": "string (required, valid email format)",
  "password": "string (required, min 8 characters)"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "name": "string",
  "email": "string",
  "createdAt": "ISO 8601 datetime"
}
```

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 400 | Dados inválidos (validação falha) | `{ "error": "validation_error", "details": [...] }` |
| 409 | E-mail já cadastrado | `{ "error": "email_already_exists" }` |

---

### POST /api/auth/login

**Finalidade:** Autenticar o usuário e retornar um JWT para acesso aos demais endpoints.

**Autenticação necessária:** Não.

**Request Body:**
```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

**Response (200 OK):**
```json
{
  "token": "string (JWT)",
  "expiresAt": "ISO 8601 datetime",
  "user": {
    "id": "uuid",
    "name": "string",
    "email": "string"
  }
}
```

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 401 | Credenciais inválidas | `{ "error": "invalid_credentials" }` |
| 400 | Dados ausentes | `{ "error": "validation_error" }` |

---

### POST /api/auth/logout

**Finalidade:** Invalidar o token JWT atual. O token é adicionado a uma blacklist em memória ou em cache.

**Autenticação necessária:** Sim.

**Request Body:** Vazio.

**Response (204 No Content):** Sem corpo.

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 401 | Token ausente ou inválido | `{ "error": "unauthorized" }` |

---

### GET /api/auth/me

**Finalidade:** Retornar os dados do usuário autenticado atualmente.

**Autenticação necessária:** Sim.

**Response (200 OK):**
```json
{
  "id": "uuid",
  "name": "string",
  "email": "string",
  "createdAt": "ISO 8601 datetime"
}
```

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 401 | Token ausente ou inválido | `{ "error": "unauthorized" }` |

---

## Currículos Base (resume_profiles)

### GET /api/resumes

**Finalidade:** Listar todos os currículos base do usuário autenticado.

**Autenticação necessária:** Sim.

**Query Parameters (opcionais):**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `page` | integer | Página desejada (default: 1) |
| `limit` | integer | Itens por página (default: 20, max: 100) |
| `search` | string | Busca por título do currículo |

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "uuid",
      "title": "string",
      "isDefault": "boolean",
      "experienceCount": "integer",
      "skillsCount": "integer",
      "createdAt": "ISO 8601 datetime",
      "updatedAt": "ISO 8601 datetime"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 42,
    "totalPages": 3
  }
}
```

---

### POST /api/resumes

**Finalidade:** Criar um novo currículo base. O conteúdo pode ser enviado como Markdown plain ou como JSON estruturado. O backend converte automaticamente entre os dois formatos e armazena ambos.

**Autenticação necessária:** Sim.

**Request Body:**
```json
{
  "title": "string (required, 1-200 characters)",
  "contentMarkdown": "string (optional, raw Markdown content)",
  "contentJson": {
    "summary": "string (optional)",
    "personalInfo": {
      "name": "string (required)",
      "email": "string (optional)",
      "phone": "string (optional)",
      "location": "string (optional)",
      "linkedin": "string (optional)",
      "portfolio": "string (optional)"
    },
    "experience": [
      {
        "company": "string (required)",
        "title": "string (required)",
        "location": "string (optional)",
        "startDate": "string (YYYY-MM)",
        "endDate": "string (YYYY-MM or 'present')",
        "current": "boolean",
        "highlights": ["string"]
      }
    ],
    "education": [
      {
        "institution": "string (required)",
        "degree": "string (required)",
        "field": "string (optional)",
        "startDate": "string (YYYY)",
        "endDate": "string (YYYY)",
        "grade": "string (optional)"
      }
    ],
    "skills": [
      {
        "category": "string (e.g., 'Languages', 'Frameworks', 'Tools')",
        "items": ["string"]
      }
    ],
    "certifications": [
      {
        "name": "string (required)",
        "issuer": "string (optional)",
        "year": "string (YYYY)",
        "url": "string (optional)"
      }
    ],
    "languages": [
      {
        "language": "string (required)",
        "proficiency": "string (e.g., 'Native', 'Fluent', 'Advanced', 'Intermediate', 'Basic')"
      }
    ]
  },
  "setAsDefault": "boolean (optional, default: false)"
}
```

**Nota:** Enviar `contentMarkdown` OU `contentJson`, ou ambos. Se ambos forem enviados, `contentJson` tem precedência para normalização.

**Response (201 Created):**
```json
{
  "id": "uuid",
  "title": "string",
  "isDefault": "boolean",
  "contentMarkdown": "string",
  "contentJson": { ... },
  "createdAt": "ISO 8601 datetime"
}
```

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 400 | Validação falha (falta name ou conteúdo vazio) | `{ "error": "validation_error", "details": [...] }` |

---

### GET /api/resumes/{id}

**Finalidade:** Obter um currículo base específico pelo ID.

**Autenticação necessária:** Sim.

**Path Parameters:**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `id` | uuid | Identificador do currículo |

**Response (200 OK):**
```json
{
  "id": "uuid",
  "title": "string",
  "isDefault": "boolean",
  "contentMarkdown": "string",
  "contentJson": { ... },
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 404 | Currículo não encontrado | `{ "error": "not_found" }` |
| 403 | Currículo pertence a outro usuário | `{ "error": "forbidden" }` |

---

### PUT /api/resumes/{id}

**Finalidade:** Atualizar um currículo base existente. Substitui o conteúdo integral — campos omitidos serão apagados.

**Autenticação necessária:** Sim.

**Request Body:** Mesma estrutura do `POST /api/resumes`.

**Response (200 OK):** Mesma estrutura do `GET /api/resumes/{id}` com dados atualizados.

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 404 | Currículo não encontrado | `{ "error": "not_found" }` |
| 403 | Currículo pertence a outro usuário | `{ "error": "forbidden" }` |
| 400 | Validação falha | `{ "error": "validation_error" }` |

---

### DELETE /api/resumes/{id}

**Finalidade:** Excluir um currículo base. Exclui também todos os generated_resumes associados.

**Autenticação necessária:** Sim.

**Path Parameters:**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `id` | uuid | Identificador do currículo |

**Response (204 No Content):** Sem corpo.

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 404 | Currículo não encontrado | `{ "error": "not_found" }` |
| 403 | Currículo pertence a outro usuário | `{ "error": "forbidden" }` |

---

### PUT /api/resumes/{id}/default

**Finalidade:** Marcar um currículo base como padrão. Se o currículo marcado como padrão anteriormente existir, seu flag `isDefault` será removido.

**Autenticação necessária:** Sim.

**Path Parameters:**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `id` | uuid | Identificador do currículo |

**Response (200 OK):**
```json
{
  "id": "uuid",
  "title": "string",
  "isDefault": true,
  "message": "Currículo marcado como padrão com sucesso."
}
```

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 404 | Currículo não encontrado | `{ "error": "not_found" }` |
| 403 | Currículo pertence a outro usuário | `{ "error": "forbidden" }` |

---

## Candidaturas/Vagas (job_applications)

### GET /api/jobs

**Finalidade:** Listar todas as candidaturas/vagas registradas pelo usuário.

**Autenticação necessária:** Sim.

**Query Parameters (opcionais):**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `page` | integer | Página (default: 1) |
| `limit` | integer | Itens por página (default: 20) |
| `company` | string | Filtrar por empresa (busca parcial) |
| `status` | string | Filtrar por status (applied, interviewing, rejected, closed, open) |

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "uuid",
      "company": "string",
      "jobTitle": "string",
      "location": "string",
      "employmentType": "string",
      "seniority": "string",
      "status": "string (open | applied | interviewing | rejected | closed)",
      "generatedResumesCount": "integer",
      "latestAdherenceScore": "integer (0-100, nullable)",
      "createdAt": "ISO 8601 datetime"
    }
  ],
  "pagination": { ... }
}
```

---

### POST /api/jobs

**Finalidade:** Registrar uma nova candidatura ou vaga para análise.

**Autenticação necessária:** Sim.

**Request Body:**
```json
{
  "company": "string (required, 1-200 characters)",
  "jobTitle": "string (required, 1-200 characters)",
  "location": "string (optional)",
  "employmentType": "string (optional, enum: CLT, PJ, Estágio, Trainee, Freelance, temporary)",
  "seniority": "string (optional, enum: Estágio, Júnior, Plêno, Sênior, Especialista, Coordenador, Gerente, Diretor)",
  "status": "string (optional, default: 'open', enum: open, applied, interviewing, rejected, closed)",
  "rawDescription": "string (required, 50-50000 characters)",
  "sourceUrl": "string (optional, valid URL)",
  "appliedAt": "string (optional, ISO 8601 date)"
}
```

**Validação:**
- `rawDescription` deve ter entre 50 e 50.000 caracteres.
- `company` e `jobTitle` são obrigatórios.

**Response (201 Created):**
```json
{
  "id": "uuid",
  "company": "string",
  "jobTitle": "string",
  "location": "string",
  "employmentType": "string",
  "seniority": "string",
  "status": "string",
  "rawDescription": "string (truncado a 500 chars na listagem)",
  "sourceUrl": "string",
  "createdAt": "ISO 8601 datetime"
}
```

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 400 | Validação falha | `{ "error": "validation_error", "details": [...] }` |

---

### GET /api/jobs/{id}

**Finalidade:** Obter detalhes completos de uma vaga.

**Autenticação necessária:** Sim.

**Response (200 OK):**
```json
{
  "id": "uuid",
  "company": "string",
  "jobTitle": "string",
  "location": "string",
  "employmentType": "string",
  "seniority": "string",
  "status": "string",
  "rawDescription": "string (completo)",
  "sourceUrl": "string",
  "appliedAt": "ISO 8601 date",
  "generatedResumes": [
    {
      "id": "uuid",
      "version": "integer",
      "adherenceScore": "integer",
      "createdAt": "ISO 8601 datetime"
    }
  ],
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

---

### PUT /api/jobs/{id}

**Finalidade:** Atualizar uma vaga existente (status, descrição, dados).

**Autenticação necessária:** Sim.

**Request Body:** Mesma estrutura do `POST /api/jobs` (todos os campos opcionais).

**Response (200 OK):** Mesma estrutura do `GET /api/jobs/{id}`.

---

### DELETE /api/jobs/{id}

**Finalidade:** Excluir uma vaga. Exclui também todos os generated_resumes associados.

**Autenticação necessária:** Sim.

**Response (204 No Content).**

---

## Geração de Currículo (generated_resumes)

### POST /api/generate

**Finalidade:** Endpoint principal de geração. Recebe o ID do currículo base e o ID da vaga, dispara a chamada ao provedor de IA, armazena o resultado e retorna imediatamente o ID do currículo gerado junto com a análise de aderência.

**Autenticação necessária:** Sim.

**Request Body:**
```json
{
  "resumeProfileId": "uuid (required)",
  "jobApplicationId": "uuid (required)"
}
```

**Fluxo interno:**
1. Valida que ambos os IDs existem e pertencem ao usuário.
2. Recupera o currículo base (`content_jsonb`) e a vaga (`raw_description`).
3. Remove PII (e-mail, telefone, links) do conteúdo antes de enviar ao provedor.
4. Constrói o prompt com system prompt fixo + user prompt dinâmico.
5. Chama `AiProvider.generateOptimizedResume()`.
6. Armazena resposta em `generated_resumes` + `ai_runs`.
7. Retorna resposta ao cliente.

**Response (202 Accepted):**
```json
{
  "generatedResumeId": "uuid",
  "jobApplicationId": "uuid",
  "resumeProfileId": "uuid",
  "version": 1,
  "adherenceAnalysis": {
    "score": 72,
    "level": "boa",
    "summary": "string",
    "strengths": ["string"],
    "gaps": ["string"],
    "keywordMatches": {
      "found": ["string"],
      "missing": ["string"],
      "partial": ["string"]
    }
  },
  "aiRun": {
    "id": "uuid",
    "provider": "string",
    "model": "string",
    "tokensUsed": { "input": 1200, "output": 3400 },
    "durationMs": 8500,
    "costEstimate": 0.024
  },
  "createdAt": "ISO 8601 datetime"
}
```

**Nota:** O status HTTP 202 indica que a requisição foi aceita para processamento assíncrono. Na MVP, a geração é síncrona (bloqueante), mas este design permite migrar para processamento em background (fila) no futuro sem mudança de API.

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 400 | IDs ausentes ou inválidos | `{ "error": "validation_error" }` |
| 404 | Currículo base ou vaga não encontrados | `{ "error": "not_found" }` |
| 403 | Recurso pertence a outro usuário | `{ "error": "forbidden" }` |
| 502 | Falha na chamada ao provedor de IA | `{ "error": "ai_provider_error", "message": "string" }` |
| 504 | Timeout do provedor de IA (>90s) | `{ "error": "ai_timeout" }` |

---

### GET /api/generated

**Finalidade:** Listar todos os currículos gerados para o usuário.

**Autenticação necessária:** Sim.

**Query Parameters (opcionais):**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `page` | integer | Página (default: 1) |
| `limit` | integer | Itens por página (default: 20) |
| `jobApplicationId` | uuid | Filtrar por vaga específica |
| `minScore` | integer | Filtrar por score mínimo de aderência |
| `from` | ISO date | Filtrar a partir de data |
| `to` | ISO date | Filtrar até data |

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "uuid",
      "resumeProfileId": "uuid",
      "jobApplicationId": "uuid",
      "jobTitle": "string",
      "company": "string",
      "version": "integer",
      "adherenceScore": "integer",
      "adherenceLevel": "string",
      "createdAt": "ISO 8601 datetime"
    }
  ],
  "pagination": { ... }
}
```

---

### GET /api/generated/{id}

**Finalidade:** Obter o currículo gerado completo com análise e conteúdo.

**Autenticação necessária:** Sim.

**Response (200 OK):**
```json
{
  "id": "uuid",
  "resumeProfileId": "uuid",
  "jobApplicationId": "uuid",
  "version": "integer",
  "adherenceAnalysis": {
    "score": "integer",
    "level": "string",
    "summary": "string",
    "strengths": ["string"],
    "gaps": ["string"],
    "keywordMatches": { "found": [], "missing": [], "partial": [] }
  },
  "optimizedResume": {
    "text": "string",
    "markdown": "string",
    "json": { ... }
  },
  "notes": {
    "inferences": ["string"],
    "absentInformation": ["string"],
    "warnings": ["string"]
  },
  "createdAt": "ISO 8601 datetime",
  "updatedAt": "ISO 8601 datetime"
}
```

---

### GET /api/generated/{id}/analysis

**Finalidade:** Obter apenas o relatório de análise de aderência (sem o currículo otimizado). Útil para exibir o painel de análise sem carregar o conteúdo completo.

**Autenticação necessária:** Sim.

**Response (200 OK):** Retorna exclusivamente o objeto `adherenceAnalysis` e `notes` do generated_resume.

---

### PUT /api/generated/{id}

**Finalidade:** Atualizar manualmente o conteúdo de um currículo gerado. Usado quando o usuário edita o currículo no editor frontend. Ao salvar, cria uma nova versão.

**Autenticação necessária:** Sim.

**Request Body:**
```json
{
  "optimizedResume": {
    "markdown": "string (required, conteúdo completo do currículo)",
    "text": "string (optional, gerado a partir do markdown se omitido)"
  },
  "notes": {
    "inferences": ["string (optional)"],
    "warnings": ["string (optional)"]
  }
}
```

**Fluxo interno:**
1. Recebe o conteúdo editado.
2. Cria uma nova versão (incrementa `version`).
3. Mantém a versão anterior no histórico.
4. Atualiza o `updatedAt`.

**Response (200 OK):**
```json
{
  "id": "uuid",
  "version": "integer (novo, ex.: 3)",
  "message": "Currículo atualizado e nova versão criada.",
  "previousVersion": "integer (ex.: 2)"
}
```

---

### GET /api/generated/{id}/versions

**Finalidade:** Obter o histórico completo de versões para uma vaga específica (mesmo `job_application_id`), ordenadas da mais recente para a mais antiga.

**Autenticação necessária:** Sim.

**Response (200 OK):**
```json
{
  "generatedResumeId": "uuid",
  "currentVersion": 3,
  "versions": [
    {
      "version": 3,
      "adherenceScore": "integer",
      "createdAt": "ISO 8601 datetime",
      "createdBy": "manual | ai",
      "contentPreview": "string (primeiros 200 caracteres do markdown)"
    },
    {
      "version": 2,
      "adherenceScore": "integer",
      "createdAt": "ISO 8601 datetime",
      "createdBy": "manual",
      "contentPreview": "string"
    }
  ]
}
```

---

## Exportação

### GET /api/generated/{id}/docx

**Finalidade:** Gerar um arquivo DOCX a partir do currículo otimizado e disponibilizar para download. O DOCX é gerado on-demand no momento da requisição — não é armazenado no banco.

**Autenticação necessária:** Sim.

**Path Parameters:**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `id` | uuid | Identificador do currículo gerado |

**Response (200 OK):**
- `Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- `Content-Disposition: attachment; filename="nome-do-candidato-otimizado-empresa-2026-01-15.docx"`
- Corpo: binário DOCX

**Fluxo interno:**
1. Recupera `optimized_resume.markdown` do registro.
2. Converte Markdown para DOCX via biblioteca (ex.: docx, md-to-docx).
3. Aplica template com formatação profissional (fontes, espaçamento, cabeçalho com nome).
4. Retorna o arquivo.

**Error Responses:**
| Status | Condição | Corpo |
|---|---|---|
| 404 | Currículo gerado não encontrado | `{ "error": "not_found" }` |
| 403 | Pertence a outro usuário | `{ "error": "forbidden" }` |
| 500 | Erro na geração do DOCX | `{ "error": "docx_generation_error", "message": "string" }` |

---

## Execuções de IA (ai_runs)

### GET /api/ai-runs

**Finalidade:** Listar o histórico de chamadas ao provedor de IA para o usuário. Usado para debugging e análise de custos.

**Autenticação necessária:** Sim.

**Query Parameters (opcionais):**
| Parâmetro | Tipo | Descrição |
|---|---|---|
| `page` | integer | Página (default: 1) |
| `limit` | integer | Itens por página (default: 50) |
| `provider` | string | Filtrar por provedor (openai, anthropic, gemini) |
| `promptType` | string | Filtrar por tipo (analysis, resume) |
| `from` | ISO date | Filtrar a partir de data |
| `to` | ISO date | Filtrar até data |

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "uuid",
      "provider": "string",
      "model": "string",
      "promptType": "string",
      "tokensUsed": { "input": 1200, "output": 3400, "total": 4600 },
      "costEstimate": "decimal",
      "durationMs": 8500,
      "success": "boolean",
      "errorMessage": "string (null se success=true)",
      "createdAt": "ISO 8601 datetime"
    }
  ],
  "pagination": { ... }
}
```

---

### GET /api/ai-runs/{id}

**Finalidade:** Obter detalhes completos de uma execução específica, incluindo o prompt enviado e a resposta bruta.

**Autenticação necessária:** Sim.

**Response (200 OK):**
```json
{
  "id": "uuid",
  "provider": "string",
  "model": "string",
  "promptType": "string",
  "rawRequest": "string (prompt completo enviado ao provedor)",
  "rawResponse": "string (resposta原文 do provedor)",
  "tokensUsed": { "input": 1200, "output": 3400, "total": 4600 },
  "costEstimate": "decimal",
  "durationMs": 8500,
  "success": true,
  "errorMessage": null,
  "createdAt": "ISO 8601 datetime"
}
```

---

## Códigos de Erro Globais

Além dos erros específicos por endpoint:

| HTTP Status | Código | Descrição |
|---|---|---|
| 400 | `validation_error` | A requisição contém dados inválidos. Ver campo `details` para lista de erros. |
| 401 | `unauthorized` | Token JWT ausente, expirado ou malformado. |
| 403 | `forbidden` | O recurso existe mas pertence a outro usuário. |
| 404 | `not_found` | Recurso não encontrado. |
| 409 | `conflict` | Conflito de estado (ex.: e-mail já existente). |
| 429 | `rate_limited` | Limite de requisições excedido. Aguarde e tente novamente. |
| 500 | `internal_error` | Erro interno do servidor. |
| 502 | `bad_gateway` | Erro no provedor de IA. |
| 504 | `gateway_timeout` | Timeout na comunicação com o provedor de IA. |

---

## Convenções Gerais

- **Timestamps:** Todos os campos de data/hora usam formato ISO 8601 completo com timezone UTC (ex.: `2026-01-15T14:30:00Z`).
- **UUIDs:** Todos os IDs de recurso usam formato UUID v4.
- **Paginação:** Endpoints de lista usam o mesmo formato de paginação `{ data: [], pagination: { page, limit, total, totalPages } }`.
- **Versionamento:** A API não utiliza versionamento por URL (v1, v2). Breaking changes são anunciadas com antecedência de 3 meses.
- **CORS:** A API permite origens configuradas no backend. Em desenvolvimento, permite `http://localhost:3000`.