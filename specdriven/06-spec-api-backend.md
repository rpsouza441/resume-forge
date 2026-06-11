# SPEC-06 - API REST do Backend

**Projeto:** Resume Forge
**Stack:** Spring Boot 3 + Spring Security + Spring Data JPA + PostgreSQL
**Status:** Corrigido para MVP
**Data:** 2026-06-11

---

## 1. Convencoes da API

### Base URL

```text
/api
```

Nao ha versionamento no path no MVP.

### Autenticacao

- Metodo: JWT Bearer Token.
- Header: `Authorization: Bearer <accessToken>`.
- Access token: validade sugerida de 1 hora.
- Refresh token persistente e tabela de sessoes ficam fora do MVP, salvo decisao posterior.
- Payload minimo: `sub`, `email`, `iat`, `exp`.
- Algoritmo recomendado para MVP: HS256/HS512 com segredo forte em variavel de ambiente.

### Formato de Erro

```json
{
  "error": "validation_error",
  "message": "Campos invalidos.",
  "details": [
    { "field": "email", "message": "Email invalido." }
  ],
  "timestamp": "2026-06-11T15:00:00Z"
}
```

### Codigos Globais

| Status | Codigo | Uso |
|---|---|---|
| 400 | `bad_request` | Request malformado |
| 401 | `unauthorized` | Token ausente/invalido ou credenciais invalidas |
| 403 | `forbidden` | Recurso pertence a outro usuario |
| 404 | `not_found` | Recurso inexistente |
| 409 | `conflict` | Duplicidade ou estado conflitante |
| 422 | `unprocessable_entity` | Dados validos mas insuficientes para gerar |
| 429 | `rate_limited` | Limite de geracao excedido |
| 500 | `internal_error` | Erro inesperado |
| 503 | `service_unavailable` | Provedor de IA indisponivel |

### Paginacao

Query params padrao:

| Parametro | Tipo | Padrao |
|---|---|---|
| `page` | integer | `0` |
| `size` | integer | `20` |
| `sort` | string | `createdAt,desc` |

Envelope:

```json
{
  "data": [],
  "page": 0,
  "size": 20,
  "total": 0,
  "totalPages": 0
}
```

---

## 2. Auth - `/api/auth`

### 2.1 POST `/api/auth/register`

Cria usuario.

**Auth:** publica.

**Request:**

```json
{
  "name": "Joao Silva",
  "email": "joao@example.com",
  "password": "Senha1234"
}
```

**Response `201 Created`:**

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "name": "Joao Silva",
    "email": "joao@example.com"
  }
}
```

**Validacoes:**

- `name`: obrigatorio, 2 a 100 caracteres.
- `email`: obrigatorio, formato valido, unico.
- `password`: obrigatoria, minimo 8 caracteres.

### 2.2 POST `/api/auth/login`

Autentica usuario.

**Auth:** publica.

**Request:**

```json
{
  "email": "joao@example.com",
  "password": "Senha1234"
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "name": "Joao Silva",
    "email": "joao@example.com"
  }
}
```

### 2.3 POST `/api/auth/logout`

Logout logico no cliente.

**Auth:** obrigatoria.

**Response:** `204 No Content`.

**Nota:** como o MVP nao exige tabela de sessoes/refresh token persistido, logout invalida o token no cliente. Blacklist de JWT fica fora do MVP.

### 2.4 GET `/api/auth/me`

Retorna usuario autenticado.

**Auth:** obrigatoria.

**Response `200 OK`:**

```json
{
  "id": "uuid",
  "name": "Joao Silva",
  "email": "joao@example.com",
  "createdAt": "2026-06-11T15:00:00Z",
  "lastLoginAt": "2026-06-11T15:00:00Z"
}
```

---

## 3. Curriculos Base - `/api/resumes`

Todos os endpoints exigem JWT.

### 3.1 GET `/api/resumes`

Lista curriculos base do usuario.

**Query:** `page`, `size`, `sort`, `title`, `isDefault`.

**Response `200 OK`:**

```json
{
  "data": [
    {
      "id": "uuid",
      "title": "Curriculo Full Stack",
      "isDefault": true,
      "createdAt": "2026-06-11T15:00:00Z",
      "updatedAt": "2026-06-11T15:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1,
  "totalPages": 1
}
```

### 3.2 POST `/api/resumes`

Cria curriculo base digitado ou colado em texto/Markdown.

**Request:**

```json
{
  "title": "Curriculo Full Stack",
  "contentMarkdown": "# Joao Silva\n\nDesenvolvedor...",
  "contentJsonb": {},
  "isDefault": true
}
```

**Response:** `201 Created` com o curriculo criado.

**Validacoes:**

- `title`: obrigatorio, 1 a 255 caracteres.
- `contentMarkdown`: obrigatorio, minimo 50 caracteres.
- Upload de arquivo nao e aceito no MVP.
- Se `isDefault = true`, backend deve desmarcar outros defaults do usuario.

### 3.3 GET `/api/resumes/{id}`

Retorna curriculo base completo.

### 3.4 PUT `/api/resumes/{id}`

Atualiza curriculo base.

**Request:** mesmos campos de `POST /api/resumes`; todos opcionais.

### 3.5 DELETE `/api/resumes/{id}`

Soft delete de curriculo base.

**Regra MVP:** se existirem `generated_resumes` ativos dependentes, retornar `409 conflict`. O usuario deve manter o curriculo base para preservar historico e reproducibilidade.

### 3.6 PUT `/api/resumes/{id}/default`

Define curriculo como padrao e desmarca os demais do usuario.

---

## 4. Vagas - `/api/jobs`

Todos os endpoints exigem JWT.

### 4.1 GET `/api/jobs`

Lista vagas/analisadas cadastradas pelo usuario.

**Query:** `companyName`, `jobTitle`, `status`, `dateFrom`, `dateTo`, `page`, `size`.

### 4.2 POST `/api/jobs`

Cria vaga por formulario ou colagem da descricao completa.

**Request:**

```json
{
  "resumeProfileId": "uuid | null",
  "companyName": "Acme",
  "jobTitle": "Desenvolvedor Java",
  "jobDescription": "Descricao completa da vaga...",
  "jobUrl": "https://example.com/vaga",
  "jobLocation": "Remoto",
  "jobType": "remote",
  "seniority": "mid"
}
```

**Response:** `201 Created`.

**Validacoes:**

- `companyName`, `jobTitle`, `jobDescription`: obrigatorios.
- `jobDescription`: minimo 100 caracteres.
- `resumeProfileId`, quando enviado, deve pertencer ao usuario.
- Salario pode existir na descricao, mas nao deve ser inserido no curriculo gerado.

### 4.3 GET `/api/jobs/{id}`

Retorna vaga completa.

### 4.4 PUT `/api/jobs/{id}`

Atualiza dados da vaga.

### 4.5 DELETE `/api/jobs/{id}`

Soft delete da vaga.

**Regra MVP:** se existirem `generated_resumes` ativos vinculados, retornar `409 conflict` para preservar historico.

---

## 5. Geracao com IA - `/api/generate`

### 5.1 POST `/api/generate`

Gera curriculo otimizado para uma vaga.

**Auth:** obrigatoria.

**Caracteristica MVP:** endpoint sincrono. O usuario aguarda a chamada ao provedor de IA terminar. Nao usar fila, worker, RabbitMQ, Redis Queue ou processamento assíncrono no MVP.

**Request:**

```json
{
  "resumeProfileId": "uuid",
  "jobApplicationId": "uuid",
  "extraInstructions": "Opcional: enfatizar backend Java e cloud."
}
```

**Response `201 Created`:**

```json
{
  "id": "uuid",
  "resumeProfileId": "uuid",
  "jobApplicationId": "uuid",
  "versionNumber": 1,
  "isCurrent": true,
  "status": "completed",
  "contentMarkdown": "# Joao Silva\n\n...",
  "contentText": "Joao Silva...",
  "contentJsonb": {},
  "analysis": {
    "id": "uuid",
    "adherenceScore": 82,
    "summary": "Boa aderencia para backend Java.",
    "keywordMap": {
      "matched": ["Java", "Spring Boot", "PostgreSQL"],
      "missing": ["Kubernetes"]
    },
    "gaps": []
  },
  "aiRun": {
    "id": "uuid",
    "provider": "gemini",
    "model": "gemini-2.0-flash",
    "status": "succeeded"
  },
  "createdAt": "2026-06-11T15:00:00Z"
}
```

**Fluxo interno obrigatório:**

1. Validar ownership de curriculo e vaga.
2. Montar prompt conforme `SPEC-05`.
3. Criar `ai_runs`.
4. Chamar `AiProvider`.
5. Validar output.
6. Criar nova linha em `generated_resumes`.
7. Criar `analysis_reports`.
8. Atualizar `ai_runs`.

**Erros:**

| Status | Codigo | Condicao |
|---|---|---|
| 403 | `forbidden` | Curriculo ou vaga pertence a outro usuario |
| 404 | `not_found` | Curriculo ou vaga nao existe |
| 422 | `insufficient_data` | Curriculo ou vaga nao tem dados suficientes |
| 422 | `invalid_ai_output` | IA retornou estrutura invalida |
| 503 | `ai_provider_unavailable` | Provedor indisponivel |

---

## 6. Curriculos Gerados - `/api/generated`

Todos os endpoints exigem JWT.

### 6.1 GET `/api/generated`

Lista historico de curriculos gerados.

**Query:** `companyName`, `jobTitle`, `resumeProfileId`, `dateFrom`, `dateTo`, `isCurrent`, `page`, `size`.

**Response `200 OK`:**

```json
{
  "data": [
    {
      "id": "uuid",
      "resumeProfileId": "uuid",
      "jobApplicationId": "uuid",
      "companyName": "Acme",
      "jobTitle": "Desenvolvedor Java",
      "versionNumber": 2,
      "isCurrent": true,
      "adherenceScore": 82,
      "aiProvider": "gemini",
      "aiModel": "gemini-2.0-flash",
      "createdAt": "2026-06-11T15:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1,
  "totalPages": 1
}
```

### 6.2 GET `/api/generated/{id}`

Retorna curriculo gerado completo, analise e metadados.

### 6.3 GET `/api/generated/{id}/analysis`

Retorna apenas a analise de aderencia.

### 6.4 PUT `/api/generated/{id}`

Salva edicao manual como nova versao.

**Request:**

```json
{
  "contentMarkdown": "# Joao Silva\n\nConteudo editado...",
  "contentJsonb": {}
}
```

**Response `201 Created`:**

```json
{
  "id": "uuid-nova-versao",
  "previousVersionId": "uuid-versao-anterior",
  "versionNumber": 3,
  "isCurrent": true,
  "generationReason": "manual_edit"
}
```

**Regra:** este endpoint nao chama IA e nao cria `ai_runs`.

### 6.5 POST `/api/generated/{id}/regenerate`

Regenera por IA usando o mesmo curriculo base e a mesma vaga.

**Response:** mesmo contrato de `POST /api/generate`.

### 6.6 GET `/api/generated/{id}/versions`

Lista versoes do mesmo par `resume_profile_id` + `job_application_id`.

**Response `200 OK`:**

```json
{
  "data": [
    {
      "id": "uuid",
      "versionNumber": 3,
      "isCurrent": true,
      "generationReason": "manual_edit",
      "aiProvider": "gemini",
      "aiModel": "gemini-2.0-flash",
      "createdAt": "2026-06-11T15:00:00Z"
    }
  ]
}
```

### 6.7 GET `/api/generated/{id}/docx`

Gera e retorna DOCX sob demanda.

**Response `200 OK`:**

Headers:

```text
Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document
Content-Disposition: attachment; filename="Joao-Silva-otimizado-Acme-2026-06-11.docx"
```

Body: binario DOCX.

**Regras:**

- Gerar via Java/Apache POI.
- Usar `generated_resumes.content_markdown`.
- Atualizar `docx_generated_at`.
- Nao salvar arquivo no banco, disco ou bucket.
- Retornar `409 conflict` se o curriculo ainda nao estiver em estado exportavel.

---

## 7. AI Runs - `/api/ai-runs`

Todos os endpoints exigem JWT.

### 7.1 GET `/api/ai-runs`

Lista chamadas de IA do usuario para auditoria.

**Query:** `generatedResumeId`, `provider`, `model`, `status`, `dateFrom`, `dateTo`.

### 7.2 GET `/api/ai-runs/{id}`

Retorna detalhes de uma chamada de IA.

**Regra de seguranca:** retornar prompt e resposta bruta apenas para o dono do recurso. Em ambiente publico, considerar mascaramento de PII.

---

## 8. Contratos de Ownership

Todo endpoint autenticado deve aplicar uma destas regras:

- Recurso com `user_id`: comparar diretamente com usuario do JWT.
- `generated_resumes`: validar via join com `resume_profiles.user_id`.
- `analysis_reports`: validar via `generated_resumes -> resume_profiles -> user_id`.
- `ai_runs`: validar via `generated_resumes -> resume_profiles -> user_id` ou `user_id` direto se a tabela possuir coluna.

---

## 9. Decisoes Fixadas por Esta Spec

- API de geracao e sincrona no MVP.
- Nao ha fila, worker ou estado `PROCESSING` obrigatorio.
- Upload de arquivo nao existe nos endpoints do MVP.
- DOCX e streamado diretamente no download.
- Exclusao de curriculo/vaga com geracoes vinculadas retorna `409 conflict`.
- Edicao manual cria nova versao sem chamar IA.
