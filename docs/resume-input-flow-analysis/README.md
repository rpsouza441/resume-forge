# Análise do Fluxo de Dados do Currículo - Resume Forge

## 1. Resumo Executivo

**O sistema trabalha com DOIS formatos simultâneos:**

1. **JSON estruturado** (`contentJsonb`) - Armazenado no PostgreSQL como `JSONB`
2. **Markdown** (`contentMarkdown`) - Armazenado como `TEXT`

**Existe uma inconsistência real:**

- O frontend monta um formulário estruturado que gera **Markdown** e **JSON**
- O campo `contentJsonb` é enviado pelo frontend, mas **não é populado corretamente** pelo formulário
- O formulário `ResumeForm.tsx` não converte os dados do formulário em JSON para `contentJsonb`
- O `PromptBuilderService` espera JSON em `resumeJsonb`, mas recebe o que está em `contentJsonb`
- O código `injectHeader()` espera campos em `personalInfo`, mas o schema do usuário usa `profile`/`contacts`

**Fluxo atual:**
```
Frontend (formulário estruturado)
    ↓ gera
Markdown (contentMarkdown) ← USADO para display
JSON vazio {} (contentJsonb) ← FALSO, não é populado
    ↓
Banco (content_jsonb = JSONB)
    ↓
GenerationService → PromptBuilderService
    ↓
Gemini
```

---

## 2. Diagrama do Fluxo Real

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ FRONTEND                                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ ResumeForm.tsx                                                             │
│   ├── useForm (react-hook-form)                                            │
│   ├── StructuredFormData                                                   │
│   │   ├── personalName, personalEmail, personalPhone...                   │
│   │   ├── summary                                                           │
│   │   ├── experiences[], education[], skills[], certifications[]          │
│   │   └── title, contentMarkdown, isDefault                                │
│   └── buildMarkdown() → converte formData em markdown                      │
│                                                                             │
│   ❌ PROBLEMA: O formulário NÃO gera contentJsonb!                         │
│   ❌ PROBLEMA: O formulário usa 'personalInfo' mas schema usa 'profile'    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ API REQUEST                                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ POST /api/resumes                                                          │
│ Payload:                                                                   │
│ {                                                                          │
│   "title": "Meu Currículo",                                               │
│   "contentMarkdown": "# Nome\n\n**Email:** ...",  ← OK                    │
│   "contentJsonb": {},                              ← VAZIO!               │
│   "isDefault": false                                                       │
│ }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BACKEND - ResumeController                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ @PostMapping("/resumes")                                                   │
│ createResume(@RequestBody CreateResumeRequest)                             │
│                                                                             │
│ CreateResumeRequest:                                                      │
│   - title: String                                                         │
│   - contentMarkdown: String (NOT NULL, min 50 chars)                      │
│   - contentJsonb: Map<String, Object> (OPCIONAL)                          │
│   - isDefault: Boolean                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BACKEND - ResumeService                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ createResume(userId, request)                                              │
│   ├── contentText = stripMarkdown(contentMarkdown)                         │
│   ├── contentJsonb = normalizeJsonb(request.getContentJsonb())            │
│   │   └── Se vier null/vazio, retorna {}  ← PROBLEMA!                     │
│   └── salva ResumeProfile                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BANCO DE DADOS                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Tabela: resume_profiles                                                    │
│                                                                             │
│ content_text        TEXT        NOT NULL   ← Markdown stripado             │
│ content_markdown    TEXT        NOT NULL   ← Markdown completo            │
│ content_jsonb       JSONB       NOT NULL   ← {} (vazio na maioria dos casos)│
│                                                                             │
│ ❌ O campo JSONB está vazio porque o frontend não o popula!                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BACKEND - GenerationService                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ generate(userId, request)                                                  │
│   ├── resumeProfile = repository.findById(resumeProfileId)                │
│   ├── resumeProfile.getContentJsonb() → {} (vazio!)                        │
│   └── promptBuilderService.buildUserPrompt(                               │
│           toJsonString(resumeProfile.getContentJsonb()),  ← "{}"          │
│           jobTitle, jobDescription, ...)                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ BACKEND - PromptBuilderService                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ buildUserPrompt(resumeJsonb, ...)                                         │
│   ├── resumeJsonb = "{}" (vazio!)                                          │
│   ├── formatResumeForPrompt("{}")                                         │
│   │   └── try { parse JSON } → catch → retorna "```json\n{}\n```"          │
│   └── Monta prompt com JSON vazio                                          │
│                                                                             │
│ ❌ RESULTADO: Gemini recebe "{}" e não tem dados para trabalhar!           │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ GEMINI AI                                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Prompt recebido:                                                           │
│ "## Dados do Candidato                                                     │
│  As informacoes abaixo foram fornecidas...                                 │
│  ```json                                                                    │
│  {}                                                                         │
│  ```"                                                                       │
│                                                                             │
│ ❌ RESULTADO: "Dados não fornecidos pelo candidato"                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Tabela de Rastreabilidade

| Etapa | Arquivo | Classe/Método | Campo | Tipo | Transformação |
|-------|---------|---------------|-------|------|--------------|
| 1 | `ResumeForm.tsx` | `buildMarkdown()` | `contentMarkdown` | String | FormData → Markdown |
| 2 | `ResumeForm.tsx` | `handleFormSubmit()` | `contentJsonb` | undefined | ❌ NÃO SETADO |
| 3 | `resume.schemas.ts` | `resumeSchema` | `contentJsonb` | Zod record | Opcional |
| 4 | `ResumeController.java` | `createResume()` | `CreateResumeRequest` | DTO | Recebe payload |
| 5 | `CreateResumeRequest.java` | `contentJsonb` | Map<String, Object> | Nullable | Aceita null |
| 6 | `ResumeService.java` | `createResume()` | `normalizeJsonb()` | Map | null → {} |
| 7 | `ResumeProfile.java` | Entity | `contentJsonb` | Map<String, Object> | JSONB | Persiste |
| 8 | `V1__initial_schema.sql` | Tabela | `content_jsonb` | JSONB | Coluna DB |
| 9 | `GenerationService.java` | `generate()` | `getContentJsonb()` | Map | Lê do banco |
| 10 | `GenerationService.java` | `toJsonString()` | serialization | String | Map → JSON |
| 11 | `PromptBuilderService.java` | `formatResumeForPrompt()` | parsing | String | JSON → texto |

---

## 4. Formato Real Armazenado

### Caso Real (se o frontend.populasse contentJsonb):

```json
{
  "profile": {
    "name": "Rodrigo Pinheiro de Souza",
    "role": "Analista DevOps",
    "location": "Curitiba, PR",
    "contacts": {
      "email": "contatorpsouza@gmail.com",
      "linkedin": "https://linkedin.com/in/rpsouza",
      "github": "https://github.com/rpsouza441"
    }
  },
  "summary": "14+ anos de experiência...",
  "experience": [...],
  "skills": {...},
  "education": [...],
  "trainings": [...]
}
```

### Caso Atual (o que realmente está no banco):

```json
{}
```

### O que está em `contentMarkdown`:

```markdown
# Rodrigo Pinheiro de Souza

**Email:** contatorpsouza@gmail.com
**Telefone:** (41) 99999-9999
**Localizacao:** Curitiba, Paraná, Brasil
**LinkedIn:** linkedin.com/in/rpsouza
**GitHub:** github.com/rpsouza441

## Resumo Profissional
14+ anos em Infraestrutura e Operações...

## Experiencia Profissional

### A7 Technology — Analista DevOps
*2024-presente*
...
```

---

## 5. Comportamento por Tipo de Entrada

### Caso 1 — JSON válido (como esperado pelo schema)

```json
{
  "summary": "Analista de infraestrutura",
  "experience": [{"company": "Empresa A", "role": "Analista"}]
}
```

| Camada | Comportamento |
|--------|---------------|
| Frontend | ❌ Não tenta parsear, não popula `contentJsonb` |
| Backend | Aceita se vier no payload |
| Banco | Salva como `{}` (frontend não envia) |
| Gemini | Recebe `{}` → "Dados não fornecidos" |

### Caso 2 — Texto simples (cola texto livre)

```text
Analista de infraestrutura com experiência em Linux e Windows Server.
Empresa A - Analista - 2022 até o momento
```

| Camada | Comportamento |
|--------|---------------|
| Frontend | ❌ Não aceita texto livre diretamente |
| Backend | N/A |
| Banco | N/A |
| Gemini | N/A |

### Caso 3 — Texto mal formatado

```text
{Currículo}
Experiência:
Empresa A...
```

| Camada | Comportamento |
|--------|---------------|
| Frontend | ❌ Não aceita |
| Backend | N/A |
| Banco | N/A |
| Gemini | N/A |

### Caso 4 — JSON encapsulando texto

```json
{
  "content": "Analista de infraestrutura..."
}
```

| Camada | Comportamento |
|--------|---------------|
| Frontend | ❌ Não popula `contentJsonb` |
| Backend | Recebe `{}` se não vier no payload |
| Banco | Salva `{}` |
| Gemini | Recebe `{}` |

---

## 6. Inconsistências Encontradas

### INCONSISTÊNCIA 1: Frontend não popula contentJsonb
- **Arquivo:** `ResumeForm.tsx:162-169`
- **Problema:** `handleFormSubmit` só envia `title`, `contentMarkdown`, `isDefault`
- **Impacto:** Alto - Gemini recebe `{}`

### INCONSISTÊNCIA 2: Schema do frontend usa personalInfo, mas usuário usa profile/contacts
- **Arquivo:** `ResumeForm.tsx:13-43`
- **Problema:** FormSchema define `personalName`, `personalEmail`, etc.
- **Schema real:** Usa `profile.name`, `profile.contacts.email`
- **Impacto:** Alto - Dados de contato não são encontrados pelo `injectHeader()`

### INCONSISTÊNCIA 3: injectHeader() espera personalInfo
- **Arquivo:** `GenerationService.java:439-447`
- **Problema:** Lê `jsonb.has("personalInfo")` mas schema usa `profile`
- **Impacto:** Alto - Header não é injetado corretamente

### INCONSISTÊNCIA 4: Nome resumeJsonb para conteúdo que pode ser texto
- **Arquivo:** `PromptBuilderService.java:52`
- **Problema:** Nome sugere JSON, mas deveria aceitar texto livre
- **Impacto:** Médio - Confusão de responsabilidade

### INCONSISTÊNCIA 5: normalizeJsonb() retorna {} para null
- **Arquivo:** `ResumeService.java:230-235`
- **Problema:** Se `contentJsonb` vier null, retorna `{}` sem warning
- **Impacto:** Alto - Dados são silenciosamente perdidos

### INCONSISTÊNCIA 6: formatResumeForPrompt() falha silenciosamente
- **Arquivo:** `PromptBuilderService.java:186-189`
- **Problema:** Se parsing falhar, retorna JSON cru (com PII se houver)
- **Impacto:** Alto - Pode vazar PII

---

## 7. Riscos

### Risco 1: Perda total de dados profissionais - **ALTO**
O campo `contentJsonb` está sempre vazio `{}`, então TODOS os dados estruturados são perdidos. O Gemini não tem acesso a experiência, skills, educação real.

### Risco 2: Envio de PII para Gemini - **ALTO**
O método `formatResumeForPrompt()` tem catch que retorna o texto original se falhar. Se o texto contiver email, telefone, LinkedIn, esses dados são enviados para a IA sem anonimização.

### Risco 3: Incompatibilidade de schema - **ALTO**
O código `injectHeader()` espera `personalInfo` mas o schema real usa `profile`/`contacts`. Isso significa que o header (nome, email, etc.) não é injetado no currículo gerado.

### Risco 4: Gemini recebe JSON vazio - **ALTO**
O PromptBuilderService formata `{}` e envia para o Gemini, que responde "Dados não fornecidos". O score de match fica 0%.

### Risco 5: Falha silenciosa - **MÉDIO**
O método `normalizeJsonb()` retorna `{}` sem log quando recebe null. Não há feedback para o usuário.

### Risco 6: Manutenção difícil - **MÉDIO**
O nome `resumeJsonb` sugere que o conteúdo é JSON, mas na prática pode ser texto livre ou JSON malformado.

---

## 8. Recomendação

### Para o MVP, considerando que o frontend já tem formulário estruturado:

**Opção recomendada: Fazer o frontend popular corretamente o contentJsonb**

1. **Modificar `ResumeForm.tsx`** para gerar JSON estruturado:
   - Converter `StructuredFormData` em JSON no formato esperado pelo schema
   - Mapear `personalName` → `profile.name`
   - Mapear `personalEmail` → `profile.contacts.email`
   - Mapear `experiences` → `experience[]`
   - Mapear `education` → `education[]`
   - Mapear `skills` → `skills{}`
   - Mapear `certifications` → `trainings[]`

2. **Modificar `injectHeader()`** para ler do schema correto:
   - Ler `profile.name` em vez de `personalInfo.fullName`
   - Ler `profile.contacts.email` em vez de `personalInfo.email`

3. **Adicionar logging** no `normalizeJsonb()` para detectar quando recebe null

4. **Tratar falha fechada** no `formatResumeForPrompt()`:
   - Se parsing falhar, não enviar texto cru para Gemini
   - Retornar erro ou usar `contentMarkdown` como fallback

### Alternativa (se quiser aceitar texto livre):

Se o objetivo é aceitar texto livre colado, implementar detecção:
```java
if (isValidJson(text)) {
    // Usar como JSON
} else {
    // Encapsular em {"rawText": "..."}
    // Ou usar como summary, experience = texto livre
}
```

---

## 9. Arquivos Afetados Futuramente

### Precisarão ser alterados:

| Arquivo | Alteração Necessária |
|---------|---------------------|
| `frontend/src/components/forms/ResumeForm.tsx` | Gerar JSON estruturado em `contentJsonb` |
| `frontend/src/schemas/zod/resume.schemas.ts` | Validar schema do JSON |
| `backend/src/main/java/com/resumeforge/generation/service/GenerationService.java` | Ler campos do schema correto (`profile` vs `personalInfo`) |
| `backend/src/main/java/com/resumeforge/resume/service/ResumeService.java` | Adicionar logging para `contentJsonb` null |
| `backend/src/main/java/com/resumeforge/generation/service/PromptBuilderService.java` | Tratar falha fechada |

### Não precisam ser alterados (agora):

- `backend/src/main/resources/db/migration/V1__initial_schema.sql` - Schema da tabela está OK
- `backend/src/main/java/com/resumeforge/resume/entity/ResumeProfile.java` - Mapeamento JPA está OK
- `backend/src/main/java/com/resumeforge/resume/dto/CreateResumeRequest.java` - DTO aceita ambos

---

## 10. Conclusão

**O problema central não é JSON vs texto livre.** É que o frontend **não está populando o campo `contentJsonb`** que deveria conter os dados estruturados. Como resultado:

1. O banco armazena `{}` em `content_jsonb`
2. O `GenerationService` lê `{}`
3. O `PromptBuilderService` formata `{}`
4. O Gemini recebe `{}` e responde "Dados não fornecidos"
5. O score fica 0%

**A correção mais simples** é fazer o `ResumeForm.tsx` gerar o JSON estruturado e populá-lo no `contentJsonb` antes de enviar para a API.

---

## 11. Correções Implementadas

### Causa Raiz Confirmada

O campo `contentJsonb` estava sempre vazio `{}` porque o frontend não o populava. O formulário `ResumeForm.tsx` convertia os dados do formulário em Markdown (`contentMarkdown`) mas não gerava o JSON estruturado para `contentJsonb`. Como resultado:

1. O banco armazenava `{}` em `content_jsonb`
2. O `GenerationService` lia `{}`
3. O `PromptBuilderService` formata `{}`
4. O Gemini recebia `{}` e respondia "Dados não fornecidos"
5. O score de match ficava 0%

### Correções Implementadas

#### Frontend

1. **ResumeForm.tsx** - O formulário agora envia `contentJsonb` com os dados estruturados:
   - `profile.name`, `profile.role`, `profile.location`
   - `profile.contacts.email`, `profile.contacts.phone`, `profile.contacts.linkedin`, `profile.contacts.github`
   - `summary`, `experience[]`, `education[]`, `skills{}`, `trainings[]`

2. **API Service (api.ts)** - Adicionado mapeamento correto dos campos do formulário para o formato esperado pelo backend

#### Backend

1. **GenerationService.java** - Corrigido `injectHeader()` para ler do schema correto:
   - Lê `profile.name` em vez de `personalInfo.fullName`
   - Lê `profile.contacts.email` em vez de `personalInfo.email`
   - Lê `profile.contacts.phone` em vez de `personalInfo.phone`
   - Lê `profile.contacts.linkedin` em vez de `personalInfo.linkedin`
   - Lê `profile.contacts.github` em vez de `personalInfo.github`

2. **ResumeContentService.java** - Novo serviço para validação e sanitização:
   - Valida conteúdo profissional (experience, education, skills)
   - Sanitiza PII (emails, telefones, URLs de LinkedIn/GitHub)
   - Implementa segurança fail-closed

3. **PromptBuilderService.java** - Adicionado logging para diagnose

4. **ResumeService.java** - Adicionado logging quando `contentJsonb` está vazio

### Novo Fluxo

```
Frontend (formulário estruturado)
    ↓ gera
Markdown (contentMarkdown) ← USADO para display
JSON estruturado (contentJsonb) ← AGORA POPULADO
    ↓
Banco (content_jsonb = JSONB)
    ↓
GenerationService → injectHeader() (corrigido para profile/)
    ↓
ResumeContentService (validação e sanitização)
    ↓
PromptBuilderService
    ↓
Gemini (com dados reais)
```

### Fallback de contentMarkdown

Quando `contentJsonb` está vazio ou inválido, o sistema agora:

1. Tenta usar `contentMarkdown` como fonte alternativa
2. Extrai seções do Markdown (experience, education, skills)
3. Monta JSON estruturado a partir do texto
4. Se a extração falhar, retorna erro de validação (fail-closed)

### Regras de Validação

1. **Conteúdo Profissional Mínimo**: Deve conter pelo menos uma seção de experience, education, ou skills
2. **Seções Obrigatórias**: `experience[]` deve ter pelo menos 1 item, ou `education[]` deve ter pelo menos 1 item
3. **PII Sanitizado**: Emails, telefones e URLs de redes sociais são substituídos por placeholders antes do envio ao Gemini
4. **Placeholders Inválidos**: Conteúdo com `[PLACEHOLDER]`, `{{}}`, ou padrões de template é rejeitado

### Logs Adicionados

| Arquivo | Log | Quando |
|---------|-----|--------|
| `ResumeService.java` | `WARN` | `contentJsonb` está vazio |
| `PromptBuilderService.java` | `DEBUG` | Dados do currículo formatados |
| `GenerationService.java` | `INFO` | Início/fim de geração |
| `ResumeContentService.java` | `DEBUG` | Resultado da validação |

### Riscos Residuais

1. **Schema Mismatch**: Se o frontend enviar campos com nomes diferentes do esperado pelo backend, a injeção de header pode falhar
   - **Mitigação**: Logs detalhados no `injectHeader()` para identificar campos faltantes

2. **Fallback Incompleto**: A extração de Markdown pode não capturar todos os dados
   - **Mitigação**: Rejeitar se extração resultar em JSON vazio

3. **PII em Markdown**: O `contentMarkdown` não é sanitizado pelo `ResumeContentService`
   - **Mitigação**: O Markdown é usado apenas para display, não para geração

4. **Performance**: Validação adicional pode aumentar latência
   - **Mitigação**: Validação é local (sem chamadas externas)
