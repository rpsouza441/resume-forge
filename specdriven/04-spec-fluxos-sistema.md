# SPEC-04 - Fluxos do Sistema

**Projeto:** Resume Forge
**Status:** Corrigido para MVP
**Data:** 2026-06-11

---

## 1. Escopo Obrigatorio dos Fluxos

Estes fluxos descrevem somente o MVP definido em `docs/` e no prompt original.

### Dentro do MVP

- Cadastro e login de usuario com JWT.
- Cadastro manual de curriculo base em texto/Markdown.
- Cadastro manual ou colagem da descricao completa da vaga.
- Geracao sincrona de curriculo otimizado por IA.
- Persistencia do resultado no PostgreSQL.
- Revisao manual e salvamento como nova versao.
- Historico por empresa, vaga e data.
- Download DOCX gerado sob demanda via Java/Apache POI.

### Fora do MVP

- Upload de PDF/DOCX.
- Extracao de texto de arquivos.
- Worker Python.
- Filas RabbitMQ/Redis.
- Storage S3/MinIO/Supabase Storage.
- Persistencia de DOCX binario.
- Billing, creditos, planos, multi-tenant ou colaboracao.

---

## 2. Convencoes

- Todos os recursos pertencem ao usuario autenticado (`user_id`).
- Todo acesso a recurso deve validar ownership.
- Operacoes de escrita devem usar transacao.
- Tabelas principais: `users`, `resume_profiles`, `job_applications`, `generated_resumes`, `analysis_reports`, `ai_runs`, `processing_logs`.
- O campo Markdown canonico para curriculos gerados e `generated_resumes.content_markdown`.
- O DOCX nunca e salvo; `docx_generated_at` registra apenas o ultimo download.

---

## Fluxo 1: Cadastro de Usuario

### Trigger

Usuario envia o formulario de registro em `/register`.

### Pre-condicoes

- Email em formato valido.
- Senha atende a politica minima.
- Nao existe usuario ativo com o mesmo email normalizado.

### Passos

1. Normalizar email com `trim` e lowercase.
2. Validar `name`, `email` e `password`.
3. Consultar `users.email`.
4. Gerar hash da senha com bcrypt.
5. Inserir registro em `users`.
6. Emitir access token JWT.
7. Retornar `201 Created` com dados do usuario e token.

### DB Operations

```sql
SELECT id FROM users WHERE email = :email LIMIT 1;

INSERT INTO users (id, email, password_hash, name, created_at, updated_at, is_active)
VALUES (:id, :email, :password_hash, :name, NOW(), NOW(), TRUE);
```

### Criterios de Aceite

- [ ] Email duplicado retorna `409 conflict`.
- [ ] Senha nunca e salva em texto puro.
- [ ] Token JWT contem `sub`, `email`, `iat` e `exp`.
- [ ] Registro nao cria curriculo, vaga ou geracao automaticamente.

---

## Fluxo 2: Login

### Trigger

Usuario envia credenciais em `/login`.

### Pre-condicoes

- Usuario existe e `is_active = TRUE`.

### Passos

1. Normalizar email.
2. Buscar usuario por email.
3. Comparar senha com `password_hash`.
4. Atualizar `last_login_at`.
5. Emitir access token JWT.
6. Retornar `200 OK` com token e dados basicos do usuario.

### DB Operations

```sql
SELECT id, email, password_hash, name
FROM users
WHERE email = :email AND is_active = TRUE
LIMIT 1;

UPDATE users SET last_login_at = NOW(), updated_at = NOW()
WHERE id = :user_id;
```

### Criterios de Aceite

- [ ] Credenciais invalidas retornam `401 unauthorized`.
- [ ] A resposta nao revela se o email existe.
- [ ] Login atualiza `last_login_at`.

---

## Fluxo 3: Cadastro de Curriculo Base

### Trigger

Usuario autenticado cria curriculo base em `/resumes/new`.

### Pre-condicoes

- JWT valido.
- Conteudo foi digitado ou colado como texto/Markdown.

### Passos

1. Validar ownership pelo JWT.
2. Validar `title`.
3. Receber `contentMarkdown` e opcionalmente `contentJsonb`.
4. Gerar `contentText` removendo marcacao Markdown basica.
5. Se `isDefault = TRUE`, desmarcar outros curriculos default do usuario na mesma transacao.
6. Inserir `resume_profiles`.
7. Retornar o curriculo criado.

### DB Operations

```sql
UPDATE resume_profiles
SET is_default = FALSE, updated_at = NOW()
WHERE user_id = :user_id AND is_default = TRUE AND deleted_at IS NULL;

INSERT INTO resume_profiles (
  id, user_id, title, content_text, content_markdown, content_jsonb, is_default, created_at, updated_at
) VALUES (
  :id, :user_id, :title, :content_text, :content_markdown, :content_jsonb, :is_default, NOW(), NOW()
);
```

### Criterios de Aceite

- [ ] Nao ha upload de arquivo neste fluxo.
- [ ] Apenas um curriculo base pode ter `is_default = TRUE` por usuario.
- [ ] Conteudo textual completo fica salvo no PostgreSQL.

---

## Fluxo 4: Criacao de Vaga

### Trigger

Usuario cadastra ou cola uma descricao de vaga em `/jobs/new`.

### Pre-condicoes

- JWT valido.
- `jobTitle`, `companyName` e `jobDescription` preenchidos.

### Passos

1. Validar campos obrigatorios.
2. Associar opcionalmente a um `resumeProfileId`.
3. Salvar empresa, titulo, descricao, localidade, regime e senioridade quando informados.
4. Retornar `jobApplication` criado.

### DB Operations

```sql
INSERT INTO job_applications (
  id, user_id, resume_profile_id, job_title, company_name, job_description,
  job_location, job_type, seniority, status, created_at, updated_at
) VALUES (
  :id, :user_id, :resume_profile_id, :job_title, :company_name, :job_description,
  :job_location, :job_type, :seniority, 'saved', NOW(), NOW()
);
```

### Criterios de Aceite

- [ ] Descricao completa da vaga fica salva em `job_description`.
- [ ] Vaga pertence somente ao usuario autenticado.
- [ ] O sistema nao faz scraping de vaga no MVP.

---

## Fluxo 5: Geracao com IA

### Trigger

Usuario clica em "Gerar curriculo otimizado" apos escolher curriculo base e vaga.

### Pre-condicoes

- JWT valido.
- `resume_profile_id` pertence ao usuario.
- `job_application_id` pertence ao usuario.
- Provedor de IA ativo esta configurado.

### Passos

1. Carregar curriculo base e vaga.
2. Montar system prompt e user prompt conforme `SPEC-05`.
3. Criar registro `ai_runs` com `status = 'started'`, provider, model e prompt.
4. Chamar o provedor de IA de forma sincrona no request.
5. Validar resposta estruturada:
   - nao inventar experiencias, empresas, cargos, certificacoes, tecnologias, anos ou metricas;
   - diferenciar fatos, inferencias e lacunas;
   - retornar Markdown do curriculo e analise de aderencia.
6. Calcular proximo `version_number` para o par curriculo/vaga.
7. Marcar versao atual anterior como `is_current = FALSE`.
8. Inserir `generated_resumes` com `is_current = TRUE`.
9. Inserir `analysis_reports`.
10. Atualizar `ai_runs` com status, resposta bruta, tokens, custo estimado e duracao.
11. Retornar `201 Created` com o curriculo gerado e a analise.

### DB Operations

```sql
SELECT * FROM resume_profiles
WHERE id = :resume_profile_id AND user_id = :user_id AND deleted_at IS NULL;

SELECT * FROM job_applications
WHERE id = :job_application_id AND user_id = :user_id AND deleted_at IS NULL;

UPDATE generated_resumes
SET is_current = FALSE, updated_at = NOW()
WHERE resume_profile_id = :resume_profile_id
  AND job_application_id = :job_application_id
  AND is_current = TRUE
  AND deleted_at IS NULL;

INSERT INTO generated_resumes (
  id, resume_profile_id, job_application_id, version_number, parent_version_id,
  is_current, status, content_text, content_markdown, content_jsonb,
  ai_model, ai_provider, prompt_version, generation_reason,
  word_count, char_count, match_score, created_at, updated_at
) VALUES (
  :id, :resume_profile_id, :job_application_id, :version_number, :parent_version_id,
  TRUE, 'completed', :content_text, :content_markdown, :content_jsonb,
  :ai_model, :ai_provider, :prompt_version, :generation_reason,
  :word_count, :char_count, :match_score, NOW(), NOW()
);
```

### Criterios de Aceite

- [ ] Geracao do MVP e sincrona; nao usa fila nem worker.
- [ ] Toda chamada de IA gera registro em `ai_runs`.
- [ ] Resposta bruta da IA fica salva para auditoria.
- [ ] Falha de IA nao cria curriculo gerado como sucesso.

---

## Fluxo 6: Revisao e Edicao do Curriculo Gerado

### Trigger

Usuario edita um curriculo gerado em `/generated/{id}/edit`.

### Pre-condicoes

- Curriculo gerado existe e pertence ao usuario.
- Conteudo editado nao esta vazio.

### Passos

1. Carregar curriculo gerado atual.
2. Receber `contentMarkdown` editado pelo usuario.
3. Gerar `contentText` e atualizar `contentJsonb` quando possivel.
4. Em transacao, marcar a versao atual como `is_current = FALSE`.
5. Inserir nova linha em `generated_resumes` com:
   - `version_number = anterior + 1`;
   - `parent_version_id = id anterior`;
   - `is_current = TRUE`;
   - `generation_reason = 'manual_edit'`;
   - mesmos `resume_profile_id` e `job_application_id`.
6. Retornar a nova versao.

### Criterios de Aceite

- [ ] Edicao nunca sobrescreve a versao anterior.
- [ ] Historico completo continua acessivel.
- [ ] `ai_runs` nao e criado para edicao manual.

---

## Fluxo 7: Geracao de DOCX Sob Demanda

### Trigger

Usuario clica em "Baixar DOCX" em `/generated/{id}`.

### Pre-condicoes

- Curriculo gerado existe, pertence ao usuario e possui `content_markdown`.

### Passos

1. Validar JWT e ownership.
2. Carregar `generated_resumes.content_markdown`, dados do curriculo base e dados da vaga.
3. Converter Markdown para DOCX via Java/Apache POI.
4. Aplicar template ATS-friendly:
   - texto simples;
   - headings claros;
   - bullets nativos;
   - sem tabelas complexas;
   - sem imagens;
   - sem colunas;
   - sem informacao critica em header/footer.
5. Atualizar `generated_resumes.docx_generated_at = NOW()`.
6. Retornar stream HTTP com `Content-Type` de DOCX e `Content-Disposition: attachment`.

### DB Operations

```sql
SELECT gr.*, rp.title AS resume_title, ja.company_name, ja.job_title
FROM generated_resumes gr
JOIN resume_profiles rp ON rp.id = gr.resume_profile_id
LEFT JOIN job_applications ja ON ja.id = gr.job_application_id
WHERE gr.id = :generated_resume_id
  AND rp.user_id = :user_id
  AND gr.deleted_at IS NULL;

UPDATE generated_resumes
SET docx_generated_at = NOW(), updated_at = NOW()
WHERE id = :generated_resume_id;
```

### Criterios de Aceite

- [ ] DOCX nao e salvo em disco, banco ou bucket.
- [ ] Endpoint retorna binario diretamente no response.
- [ ] `docx_generated_at` e apenas timestamp do ultimo download.
- [ ] Falha de conversao registra `processing_logs`.

---

## Fluxo 8: Historico e Versoes

### Trigger

Usuario acessa `/generated` ou `/generated/{id}/versions`.

### Passos

1. Listar curriculos gerados do usuario com filtros opcionais por empresa, vaga e data.
2. Para detalhe de versoes, identificar a cadeia por `resume_profile_id` + `job_application_id`.
3. Ordenar versoes por `version_number DESC`.
4. Exibir versao atual, versoes antigas, data, provedor/modelo e score.

### Criterios de Aceite

- [ ] Historico nao mostra dados de outros usuarios.
- [ ] Filtros por empresa, titulo e data funcionam.
- [ ] Versao antiga pode ser aberta para leitura.

---

## Fluxo 9: Regeneracao de Curriculo

### Trigger

Usuario clica em "Regenerar" no detalhe do curriculo gerado.

### Pre-condicoes

- Curriculo gerado original pertence ao usuario.
- Curriculo base e vaga relacionados ainda existem.

### Passos

1. Carregar curriculo gerado original.
2. Reutilizar `resume_profile_id` e `job_application_id`.
3. Executar novamente o Fluxo 5 com prompts atuais.
4. Criar nova versao no mesmo par curriculo/vaga.
5. Preservar versoes anteriores.

### Criterios de Aceite

- [ ] Regeneracao cria nova versao, nao sobrescreve a anterior.
- [ ] Nova chamada de IA gera novo `ai_runs`.
- [ ] `is_current` passa a apontar para a versao regenerada.

---

## Fluxo 10: Tratamento de Erro da IA

### Trigger

Provedor de IA retorna erro, timeout ou resposta invalida.

### Passos

1. Registrar falha em `ai_runs` com status e mensagem.
2. Para erro temporario (`429`, `5xx`, timeout), aplicar retry curto no mesmo request conforme `SPEC-05`.
3. Se houver fallback provider configurado, tentar uma unica vez com o fallback.
4. Se resposta for invalida, salvar `raw_response`, marcar `ai_runs.status = 'failed'` e retornar erro controlado.
5. Registrar evento em `processing_logs`.
6. Retornar resposta HTTP adequada ao frontend.

### Criterios de Aceite

- [ ] Erros de IA nao vazam stack trace ao usuario.
- [ ] Prompt e raw response sao preservados quando existirem.
- [ ] Fallback troca somente a camada `AiProvider`, sem alterar controller ou dominio.
- [ ] Sem filas, workers ou estados de processamento assíncrono no MVP.

---

## Resumo Fluxo x Tabelas

| Fluxo | Tabelas principais |
|---|---|
| Cadastro | `users` |
| Login | `users` |
| Curriculo base | `resume_profiles` |
| Vaga | `job_applications` |
| Geracao IA | `generated_resumes`, `analysis_reports`, `ai_runs`, `processing_logs` |
| Edicao | `generated_resumes` |
| DOCX | `generated_resumes`, `processing_logs` |
| Historico | `generated_resumes`, `job_applications`, `resume_profiles` |
| Regeneracao | `generated_resumes`, `analysis_reports`, `ai_runs` |
| Erro IA | `ai_runs`, `processing_logs` |
