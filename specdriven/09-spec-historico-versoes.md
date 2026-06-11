# SPEC-09 — Histórico e Versionamento

## 1. Princípio Central
**Cada ação cria uma nova linha. Nada é sobrescrito.**

## 2. Modelo de Versionamento
- Cadeia de versões: `version_number` + `parent_version_id`
- `is_current = true` para exatamente uma versão por par `(resume_profile_id, job_application_id)`
- `parent_version_id = NULL` apenas na versão 1

## 3. Regras de Versionamento
1. Primeira geração: v=1, parent=NULL, is_current=true
2. Regeneração IA: nova versão, v+1, is_current=true na nova
3. Edição manual: nova versão, v+1, parent=anterior
4. Na criação de nova versão: versão anterior tem is_current=false
5. Nenhuma versão é deletada no MVP
6. Todas as versões são consultáveis

## 4. Campos Preservados vs. Não Preservados

| Campo                   | Preservado entre versões? | Observação                                          |
|-------------------------|---------------------------|-----------------------------------------------------|
| resume_profile_id       | Sim                       | Identificador do perfil de origem                   |
| job_application_id      | Sim                       | Identificador da vaga (NULL se sem vaga)           |
| content_markdown        | Não                       | Conteúdo principal — muda a cada geração/edição     |
| generated_by            | Não                       | "AI" ou "MANUAL" — varia por ação                  |
| ai_model_used           | Não                       | Modelo de IA usado na geração (NULL se manual)       |
| job_description         | Sim                       | Descrição da vaga referenciada                     |
| created_at              | Não                       | Timestamp da criação desta versão                   |
| docx_generated_at       | Não                       | Timestamp do último download DOCX desta versão      |
| is_current              | Não                       | Boolean — exatamente uma versão é true por par      |
| version_number          | Não                       | Inteiro sequencial, único por (profile, application)|
| parent_version_id       | Sim                       | Aponta para versão anterior na cadeia              |

## 5. Salvando Contexto de Geração

### Tabela `ai_runs` — por tentativa de geração
Registra cada chamada à API de IA:

| Campo              | Conteúdo salvo                                                        |
|--------------------|-----------------------------------------------------------------------|
| id                 | UUID gerado                                                          |
| generated_resume_id| FK para generated_resumes                                            |
| model              | Nome do modelo utilizado (ex: gpt-4o, claude-3-5-sonnet)             |
| input_tokens       | Contagem de tokens de entrada                                        |
| output_tokens       | Contagem de tokens de saída                                          |
| latency_ms         | Tempo de resposta em milissegundos                                   |
| cost_usd           | Custo estimado em dólares                                            |
| raw_ai_response    | Resposta bruta da IA (JSON string)                                   |
| error_message      | Mensagem de erro se a chamada falhou                                |
| created_at         | Timestamp da tentativa                                                |

### Tabela `analysis_reports` — por versão gerada
Registra o relatório de análise associado a cada versão:

| Campo              | Conteúdo salvo                                                        |
|--------------------|-----------------------------------------------------------------------|
| id                 | UUID                                                                  |
| generated_resume_id| FK para generated_resumes                                            |
| ats_score          | Nota de compatibilidade ATS (0-100)                                   |
| keyword_coverage   | Cobertura de palavras-chave da vaga (%)                               |
| missing_keywords   | Lista de palavras-chave ausentes (JSON array)                        |
| format_issues      | Problemas de formatação detectados (JSON array)                      |
| improvement_tips   | Dicas geradas pela IA (JSON array)                                   |
| created_at         | Timestamp                                                             |

### Por que salvar raw_ai_response
4 razões para persistir a resposta bruta da IA:

1. **Auditoria e compliance** — evidência do que a IA gerou, para fins regulatórios e deLGPD
2. **Debug e reprodutibilidade** — permite investigar por que uma geração específica teve determinado resultado
3. **Reutilização em retry** — em caso de falha deparsing, a resposta está disponível para reprocessamento sem nova chamada à API
4. **Análise histórica de comportamento** — estudo de padrões de outputs por modelo/versionamento do prompt

## 6. Queries SQL de Referência

### Listar currículos gerados (página principal)
```sql
SELECT
  gr.id,
  gr.resume_profile_id,
  gr.job_application_id,
  gr.version_number,
  gr.generated_by,
  gr.ai_model_used,
  gr.is_current,
  gr.created_at,
  gr.docx_generated_at,
  rp.full_name    AS candidate_name,
  ja.company      AS job_company,
  ja.job_title    AS job_title
FROM generated_resumes gr
JOIN resume_profiles rp ON rp.id = gr.resume_profile_id
LEFT JOIN job_applications ja ON ja.id = gr.job_application_id
WHERE gr.resume_profile_id = :profileId
  AND (gr.job_application_id = :applicationId OR :applicationId IS NULL)
ORDER BY gr.created_at DESC;
```

### Listar versões de um currículo específico
```sql
SELECT
  gr.id,
  gr.version_number,
  gr.parent_version_id,
  gr.generated_by,
  gr.ai_model_used,
  gr.is_current,
  gr.created_at,
  gr.docx_generated_at,
  ar.cost_usd     AS generation_cost,
  ar.latency_ms   AS generation_latency
FROM generated_resumes gr
LEFT JOIN ai_runs ar ON ar.generated_resume_id = gr.id
WHERE gr.id = :resumeId
   OR gr.parent_version_id = :resumeId
   OR gr.id IN (
       WITH RECURSIVE version_chain AS (
         SELECT id, parent_version_id, version_number
         FROM generated_resumes
         WHERE id = :resumeId
         UNION ALL
         SELECT g.id, g.parent_version_id, g.version_number
         FROM generated_resumes g
         JOIN version_chain vc ON vc.parent_version_id = g.id
       )
       SELECT id FROM version_chain
     )
ORDER BY gr.version_number ASC;
```

### Buscar por empresa (case-insensitive)
```sql
SELECT
  gr.id,
  gr.version_number,
  gr.is_current,
  gr.created_at,
  rp.full_name     AS candidate_name,
  ja.company,
  ja.job_title
FROM generated_resumes gr
JOIN resume_profiles  rp ON rp.id = gr.resume_profile_id
JOIN job_applications ja ON ja.id = gr.job_application_id
WHERE LOWER(ja.company) LIKE LOWER(CONCAT('%', :company, '%'))
  AND gr.is_current = true
ORDER BY gr.created_at DESC;
```

## 7. Comparação entre Versões (Futuro — Fase 2+)
- Client-side diff com diff-match-patch
- Endpoint: `GET /api/generated/{id}/diff?compareWithVersion=2`
- Resposta: `{ "baseVersion": 1, "compareVersion": 2, "diff": "<patch-string>" }`
- Abordagem rejeitada: salvar diffs como colunas (alto custo de storage, baixa utilidade no MVP)

## 8. Rollback (Futuro — Fase 2+)
- Endpoint: `POST /api/generated/{id}/rollback`
- Body: `{ "targetVersionId": "uuid-da-versao-desejada" }`
- Comportamento: marca versão selecionada como is_current=true; versões intermediárias entre a selecionada e a atual recebem is_current=false
- Não deleta versões intermediárias
- Não cria novo registro — usa marcação direta no banco
- Validação: não permite rollback para versão que não pertença à mesma cadeia (mesmo resume_profile_id e job_application_id)

## 9. Versionamento do Template DOCX
- Template = código (não config)
- Estratégia de evolução: nova classe por versão (ex: `DocxTemplateV1`, `DocxTemplateV2`), manter a classe antiga funcional durante período de transição
- Rollback = git revert da classe do template para a versão desejada
- Cada classe de template deve implementar interface `DocxTemplateStrategy`
- Testes de regressão visual (comparação de XML gerado) para cada versão do template
