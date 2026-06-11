# Fluxos do Sistema

## Fluxo 1: Cadastro de Usuário

**Trigger**: Usuário acessa a página de registro pela primeira vez.

### Passos

1. Usuário acessa `/register`.
2. Frontend exibe formulário com campos: nome completo, email, senha, confirmação de senha.
3. Usuário preenche e clica em "Criar conta".
4. Frontend valida:
   - Email com formato válido.
   - Senha com mínimo de 8 caracteres.
   - Senha e confirmação coincidem.
   - Exibe erros inline se houver.
5. Frontend envia `POST /api/auth/register` com `{name, email, password}`.
6. Backend valida:
   - Email não existe na tabela `users`.
   - Se existir: retorna 409 Conflict.
7. Backend executa `BCrypt.hash(password, cost=12)`.
8. Backend insere registro em `users` com `password_hash`.
9. Backend retorna 201 Created com `{id, email, name}`.
10. Frontend exibe mensagem de sucesso.
11. Frontend redireciona para `/login`.

### Caminho de Erro

- **Email já existe**: Backend retorna 409. Frontend exibe "Este email já está cadastrado."
- **Validação falhou**: Backend retorna 400 com campos inválidos. Frontend mostra erros inline.
- **Erro interno**: Backend retorna 500. Frontend exibe "Erro ao criar conta. Tente novamente."

### DB Operations

```sql
INSERT INTO users (id, email, password_hash, name, created_at, updated_at, is_active)
VALUES (gen_random_uuid(), :email, :passwordHash, :name, NOW(), NOW(), true)
```

---

## Fluxo 2: Login

**Trigger**: Usuário cadastrado acessa a página de login.

### Passos

1. Usuário acessa `/login`.
2. Frontend exibe formulário com campos: email, senha.
3. Usuário preenche e clica em "Entrar".
4. Frontend envia `POST /api/auth/login` com `{email, password}`.
5. Backend busca usuário por email em `users`.
6. Backend verifica `is_active = true`.
7. Backend executa `BCrypt.verify(password, password_hash)`.
8. Se inválido: retorna 401 Unauthorized com mensagem genérica (não revelar se é email ou senha).
9. Se válido: gera JWT access token (1h) + refresh token (7d).
10. Backend atualiza `last_login_at` na tabela `users`.
11. Backend retorna 200 com `{accessToken, refreshToken, user: {id, name, email}}`.
12. Frontend armazena tokens (localStorage para access, httpOnly cookie para refresh — ou ambos em localStorage no MVP se httpOnly não for possível com o frontend).
13. Frontend redireciona para `/dashboard`.

### Caminho de Erro

- **Credenciais inválidas**: Backend retorna 401. Frontend exibe "Email ou senha incorretos."
- **Conta desativada**: Backend retorna 403. Frontend exibe "Conta desativada. Entre em contato com o suporte."
- **Erro interno**: Backend retorna 500. Frontend exibe "Erro ao fazer login. Tente novamente."

### DB Operations

```sql
SELECT id, email, password_hash, name, is_active FROM users WHERE email = :email;
UPDATE users SET last_login_at = NOW() WHERE id = :id;
```

---

## Fluxo 3: Cadastro de Currículo Base

**Trigger**: Usuário clica em "Criar currículo base" no dashboard.

### Passos

1. Usuário acessa `/resumes/new`.
2. Frontend exibe formulário de currículo com duas opções:
   - **Modo estruturado**: campos para dados pessoais, experiência (repeatable), educação (repeatable), skills, certificações.
   - **Modo texto livre**: textarea único para colar currículo existente.
3. Usuário escolhe modo e preenche.
4. No modo estruturado: frontend monta objeto JSON com a estrutura do schema JSONB.
5. Frontend gera `content_markdown` automaticamente a partir dos campos (template simples: heading para cada seção, bullets para items).
6. Usuário clica "Salvar currículo".
7. Frontend envia `POST /api/resumes` com `{title, content_text, content_markdown, content_jsonb}`.
8. Backend valida: `title` não vazio, pelo menos nome ou algum conteúdo presente.
9. Se for o primeiro currículo do usuário (`SELECT COUNT(*) FROM resume_profiles WHERE user_id = :userId` = 0): define `is_default = true`.
10. Backend insere em `resume_profiles`.
11. Backend retorna 201 com currículo criado.
12. Frontend exibe sucesso (toast).
13. Frontend redireciona para `/resumes`.

### Caminho de Erro

- **Conteúdo muito curto** (< 50 caracteres): Backend retorna 400 com aviso (não bloqueia, apenas alerta).
- **Erro de parsing JSONB**: Backend retorna 400. Frontend exibe "Formato de currículo inválido."

### DB Operations

```sql
SELECT COUNT(*) FROM resume_profiles WHERE user_id = :userId;
INSERT INTO resume_profiles (id, user_id, title, content_text, content_markdown, content_jsonb, is_default, created_at, updated_at)
VALUES (gen_random_uuid(), :userId, :title, :contentText, :contentMarkdown, :contentJsonb, :isDefault, NOW(), NOW());
```

---

## Fluxo 4: Criação de Análise de Vaga

**Trigger**: Usuário clica em "Nova análise" no dashboard.

### Passos

1. Usuário acessa `/jobs/new`.
2. Frontend exibe formulário com campos opcionais (empresa, título, localização, regime, senioridade) e campo obrigatório de descrição da vaga.
3. Usuário cola a descrição completa da vaga (mínimo 100 caracteres).
4. Opcionalmente: preenche empresa e título.
5. Usuário clica "Salvar e gerar currículo".
6. Frontend valida: `job_description_raw` com pelo menos 100 caracteres.
7. Frontend envia `POST /api/jobs` com `{companyName, jobTitle, location, employmentType, seniority, jobDescriptionRaw, sourceUrl}`.
8. Backend valida entrada.
9. Backend insere em `job_applications` com `status = 'active'`.
10. Backend retorna 201 com `{id, ...}`.
11. Frontend redireciona para `/generated/new?jobId={id}` (tela de seleção de currículo base e geração — wizard em 3 etapas).

### Caminho de Erro

- **Descrição muito curta**: Backend retorna 400. Frontend exibe "A descrição da vaga deve ter pelo menos 100 caracteres."
- **Erro interno**: Backend retorna 500. Frontend exibe "Erro ao salvar vaga."

### DB Operations

```sql
INSERT INTO job_applications (id, user_id, company_name, job_title, location, employment_type, seniority, job_description_raw, source_url, status, created_at, updated_at)
VALUES (gen_random_uuid(), :userId, :companyName, :jobTitle, :location, :employmentType, :seniority, :jobDescriptionRaw, :sourceUrl, 'active', NOW(), NOW());
```

---

## Fluxo 5: Geração com IA

**Trigger**: Usuário selecionou currículo base e clicou "Gerar currículo otimizado".

### Passos

1. Frontend envia `POST /api/generate` com `{resumeProfileId, jobApplicationId}`.
2. Backend busca `resume_profile` por ID e verifica `user_id` (ownership).
3. Backend busca `job_application` por ID e verifica `user_id` (ownership).
4. Backend monta **system prompt** (fixed, das regras em `05-regras-ia.md`):
   ```
   Você é um assistente especializado em otimização de currículos...
   [regras completas]
   ```
5. Backend monta **user prompt**:
   ```
   CURRÍCULO BASE:
   [content_markdown do resume_profile]

   DESCRIÇÃO DA VAGA:
   [job_description_raw da job_application]
   ```
6. Backend cria registro em `ai_runs` com `status = 'running'`.
7. Backend chama `aiProvider.generateOptimizedResume(systemPrompt, userPrompt, options)`.
8. **Timeout**: 30 segundos. Se Provider não responde → retry 1x com mesmo provider → se falhar novamente, tentar provider fallback se configurado.
9. Backend recebe resposta (texto estruturado em formato combinado: markdown do currículo + JSON da análise).
10. Backend faz parsing da resposta:
    - Extrai `content_markdown` do currículo.
    - Extrai `analysis_report` (score, strengths, gaps, keyword_map, etc.).
    - Armazena `raw_ai_response` completo.
11. Backend gera `content_jsonb` (opcional no MVP — se a IA retornar estrutura, usar; se não, NULL).
12. Backend insere em `generated_resumes` com `version_number = 1`, `is_current = true`, `parent_version_id = NULL`.
13. Backend insere em `analysis_reports`.
14. Backend atualiza `ai_runs`: `status = 'success'`, `tokens_used`, `cost_usd`, `duration_ms`.
15. Backend retorna 200 com `{generatedResumeId, versionNumber, analysisSummary}`.

### Caminho de Erro

- **Provedor retorna erro**: Backend atualiza `ai_runs` com `status = 'error'`, `error_message`. Retorna 502 ao frontend. Exibe "A geração falhou. Tente novamente em alguns minutos."
- **Timeout**: Idem acima, com mensagem "Tempo limite excedido. O provedor de IA está demorando mais que o normal."
- **Parse failure**: Backend salva `raw_ai_response` mesmo assim, marca `ai_runs.status = 'error'`, retorna 500. Útil para debugging.
- **Ownership failure**: Usuário tenta gerar com currículo de outro usuário → 403 Forbidden.

### DB Operations

```sql
INSERT INTO ai_runs (id, generated_resume_id, provider, model, prompt_used, status, created_at) VALUES (...);
INSERT INTO generated_resumes (id, user_id, resume_profile_id, job_application_id, version_number, content_markdown, content_jsonb, is_current, created_at, updated_at) VALUES (...);
INSERT INTO analysis_reports (id, generated_resume_id, adherence_score, adherence_level, key_strengths, gaps, keyword_map, positioning_strategy, raw_ai_response, created_at) VALUES (...);
UPDATE ai_runs SET status='success', tokens_used=..., cost_usd=..., duration_ms=... WHERE id = :aiRunId;
```

---

## Fluxo 6: Revisão e Edição do Currículo Gerado

**Trigger**: Usuário visualiza o currículo gerado e decide alterar algo manualmente.

### Passos

1. Usuário acessa `/generated/{id}`.
2. Frontend exibe currículo gerado + análise de aderência.
3. Usuário clica em "Editar".
4. Frontend abre editor com `content_markdown` pré-preenchido.
5. Usuário faz alterações no textarea.
6. Usuário clica em "Salvar".
7. Frontend envia `PUT /api/generated/{id}` com `{contentMarkdown, contentText}`.
8. Backend busca `generated_resume` atual (is_current=true) por ID.
9. Backend atualiza `is_current = false` no registro atual.
10. Backend insere novo registro em `generated_resumes`:
    - `version_number = previous.version_number + 1`
    - `parent_version_id = previous.id`
    - `content_markdown = novo conteúdo`
    - `is_current = true`
11. Backend retorna 201 com nova versão.
12. Frontend exibe toast de sucesso e atualiza a tela para mostrar a nova versão.

### Caminho de Erro

- **Conflito de versão**: Se outro processo salvou entre a leitura e o save (raro no MVP single-user) → retorna 409. Frontend exibe "Esta versão foi alterada por outra ação. Recarregue a página."
- **Conteúdo vazio**: Backend retorna 400. Frontend exibe "O currículo não pode ficar vazio."

### DB Operations

```sql
UPDATE generated_resumes SET is_current = false WHERE id = :currentId;
INSERT INTO generated_resumes (id, user_id, resume_profile_id, job_application_id, version_number, parent_version_id, content_markdown, content_text, is_current, created_at, updated_at)
VALUES (gen_random_uuid(), :userId, :resumeProfileId, :jobApplicationId, :newVersion, :parentId, :newMarkdown, :newText, true, NOW(), NOW());
```

---

## Fluxo 7: Geração de .docx Sob Demanda

**Trigger**: Usuário clica em "Baixar DOCX" na tela de detalhe do currículo gerado.

### Passos

1. Usuário clica no botão "Baixar DOCX" em `/generated/{id}`.
2. Frontend mostra loading state no botão: "Gerando arquivo...".
3. Frontend envia `GET /api/generated/{id}/docx`.
4. Backend busca `generated_resume` por ID e verifica ownership.
5. Backend obtém `content_markdown`.
6. Backend chama `MarkdownToDocxConverter.convert(markdown)`:
   - Parser: percorre markdown line by line.
   - Headings (#, ##) → XWPFParagraph com HeadingStyle.
   - Bullets (-) → XWPFParagraph com indentation e bullet symbol.
   - Bold (**text**) → XWPFRun com setBold(true).
   - Texto normal → XWPFParagraph simples.
   - Links: removidos (não são lidos por ATS).
7. Backend aplica template ATS-friendly:
   - Font: Calibri ou Arial, 11pt body, 14pt name, 12pt section headers.
   - Margins: 2.5cm em todos os lados.
   - Espaçamento: 1.15 entre linhas.
8. Backend atualiza `docx_generated_at` no `generated_resumes`.
9. Backend retorna response como stream:
   - `Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document`
   - `Content-Disposition: attachment; filename="Nome-Otimitado-Empresa-2026-06-11.docx"`
10. Browser dispara download.

### Critérios ATS Implementados no Template

- Sem tabelas (APENAS texto e bullets).
- Sem imagens, ícones ou logos.
- Sem colunas.
- Cabeçalhos de seção em formato semântico (Heading 1/2).
- Bullets com símbolo padrão (•).
- Dados de contato na primeira linha (não em header).
- Sem header/footer com informação crítica.

### Caminho de Erro

- **Template error**: Backend retorna 500. Frontend exibe "Erro ao gerar arquivo. Tente novamente."
- **Conteúdo vazio**: Backend retorna 400. Frontend exibe "Currículo vazio. Edite o conteúdo antes de baixar."

---

## Fluxo 8: Histórico e Versões

**Trigger**: Usuário acessa a tela de histórico para consultar currículos gerados anteriormente.

### Passos

1. Usuário acessa `/history`.
2. Frontend envia `GET /api/generated` com filtros opcionais (company, jobTitle, dateFrom, dateTo).
3. Backend executa query join entre `generated_resumes` e `job_applications`:
   ```sql
   SELECT gr.*, ja.company_name, ja.job_title, ja.created_at as job_created_at,
          ar.adherence_score, ar.adherence_level
   FROM generated_resumes gr
   JOIN job_applications ja ON gr.job_application_id = ja.id
   LEFT JOIN analysis_reports ar ON ar.generated_resume_id = gr.id
   WHERE gr.user_id = :userId AND gr.is_current = true
   ORDER BY gr.created_at DESC
   ```
4. Backend retorna lista paginada.
5. Frontend exibe tabela/cards com colunas: Empresa, Título, Data, Score, Versões.
6. Usuário clica em uma linha para expandir versões.
7. Frontend envia `GET /api/generated/{id}/versions`:
   ```sql
   SELECT id, version_number, created_at, is_current
   FROM generated_resumes
   WHERE job_application_id = :jobApplicationId AND resume_profile_id = :resumeProfileId
   ORDER BY version_number DESC
   ```
8. Frontend exibe lista de versões. Clique em versão abre `/generated/{versionId}`.

---

## Fluxo 9: Regeneração de Currículo

**Trigger**: Usuário quer uma nova geração (não uma edição) para a mesma vaga.

### Passos

1. Usuário acessa `/generated/{id}` e clica em "Regenerar".
2. Frontend exibe confirmação: "Gerar uma nova versão? A versão atual será arquivada."
3. Usuário confirma.
4. Frontend envia `POST /api/generate` com `{resumeProfileId, jobApplicationId}`.
5. Backend executa mesmo processo do Fluxo 5, mas:
   - Antes de inserir novo `generated_resumes`: marca o atual como `is_current = false`.
   - Nova versão recebe `parent_version_id = id_da_versão_atual`.
   - `version_number = versão_atual + 1`.
6. Retorna nova versão. Frontend redireciona para `/generated/{novaVersaoId}`.

---

## Fluxo 10: Tratamento de Erro da IA

**Trigger**: Provedor de IA retorna erro ou timeout durante a geração.

### Comportamento

1. **Timeout (30s sem resposta)**:
   - Retry 1x imediato com o mesmo provider.
   - Se retry succeeds: continuar normalmente.
   - Se retry fails: seguir para passo 2.

2. **Erro retornado pelo provider** (HTTP 4xx/5xx do provider):
   - Se provider configurado tem `fallbackProvider` definido:
     - Chamar fallback provider com o mesmo prompt.
     - Log em `ai_runs` com `provider` = fallback.
   - Se não há fallback ou fallback também falha: seguir para passo 3.

3. **Todas as tentativas falharam**:
   - Backend insere/atualiza `ai_runs` com `status = 'error'`, `error_message = mensagem do erro`.
   - Backend insere log em `processing_logs` com `level = ERROR`.
   - Backend retorna 502 Bad Gateway ao frontend com mensagem amigável.
   - Frontend exibe: "A geração falhou. O provedor de IA está temporariamente indisponível. Tente novamente em alguns minutos."
   - O usuário pode clicar "Tentar novamente" — o mesmo fluxo é reiniciado.

4. **Retry manual pelo usuário**:
   - Frontend guarda o estado (resumeProfileId + jobApplicationId).
   - Botão "Tentar novamente" resubmete o mesmo request.
   - Sem limite de retries no MVP (rate limit do provider é respeitado pelo backend).

### Logging

```java
// Log de erro em processing_logs
logService.log(ProcessingLog.builder()
    .userId(userId)
    .operation("AI_GENERATION_FAILED")
    .entityType("generated_resume")
    .entityId(generatedResumeId)
    .logLevel("ERROR")
    .message("AI generation failed after retries")
    .metadata(Map.of(
        "provider", provider,
        "errorMessage", errorMessage,
        "attempts", attempts
    ))
    .build());
```