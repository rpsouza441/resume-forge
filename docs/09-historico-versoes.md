# Histórico e Versionamento

## Estratégia Geral

O sistema mantém histórico completo e imutável de todas as versões de cada currículo gerado. O princípio central é:

> **Cada ação de geração ou edição cria uma nova linha no banco. Nada é sobrescrito.**

Isso permite:
- Audit trail completo (quem gerou, quando, com qual IA, qual prompt).
- Comparação entre versões (funcionalidade futura).
- Rollback para versão anterior (funcionalidade futura).
- Reproducibilidade (saber exatamente qual versão foi baixada como .docx).

---

## Modelo de Versionamento

### Cadeia de Versões (Version Chain)

Cada currículo gerado para um par `(resume_profile_id, job_application_id)` forma uma cadeia de versões:

```
Versão 1 (is_current=false, parent=null)
    ↓ parent_version_id
Versão 2 (is_current=false, parent=v1)
    ↓ parent_version_id
Versão 3 (is_current=true, parent=v2)  ← versão atual
```

- `version_number`: número sequencial (1, 2, 3...).
- `parent_version_id`: UUID da versão anterior (NULL apenas na versão 1).
- `is_current`: true para exatamente uma versão por cadeia (a mais recente).

### Regras de Versionamento

1. **Primeira geração**: `version_number = 1`, `parent_version_id = NULL`, `is_current = true`.
2. **Regeneração via IA**: mesma regra — cria nova versão com `version_number + 1`.
3. **Edição manual**: cria nova versão com `version_number + 1`, `parent_version_id = versão anterior`.
4. **Na criação de nova versão**: a versão anterior recebe `is_current = false`.
5. **Uma versão nunca é deletada** no MVP (soft delete não implementado).
6. **Todas as versões são consultáveis** — o usuário pode ver qualquer versão anterior.

### Por que não usar Soft Delete

Soft delete (flag `deleted_at`) seria útil em um sistema multi-usuário onde um administrador precisa excluir currículos por compliance (LGPD). No MVP single-user, não há esse requisito. A simplicidade de `DELETE` real vence.

---

## Relacionamento entre Entidades

```
users (1) ──┬── (*) resume_profiles
            │
            └── (*) job_applications
                      │
                      └── (*) generated_resumes ── (parent_version_id) ── self
                            │
                            ├── (1:1) analysis_reports
                            │
                            └── (*) ai_runs
```

Cada `generated_resume` aponta para:
- Seu `resume_profile` de origem (não muda entre versões).
- Sua `job_application` (não muda entre versões).
- Sua `analysis_report` (pode mudar entre versões se a IA gerar análise diferente).
- Seu `ai_run` (cada tentativa de geração tem seu próprio log).

---

## O que é Salvo em Cada Versão

| Campo | Preservado entre versões? | Observação |
|-------|--------------------------|------------|
| `content_markdown` | Não — é o conteúdo da versão | Altera a cada versão |
| `content_text` | Não | Altera a cada versão |
| `content_jsonb` | Não | Altera a cada versão |
| `resume_profile_id` | Sim | Sempre aponta para o mesmo currículo base |
| `job_application_id` | Sim | Sempre aponta para a mesma vaga |
| `parent_version_id` | Sim | Aponta para versão anterior |
| `version_number` | Não — é o número da versão | Incrementa a cada versão |
| `is_current` | Não — marca a mais recente | Altera a cada nova versão |
| `analysis_report` | Não — nova análise por versão | Cada versão tem sua análise |

---

## Salvando Contexto de Geração

Cada versão carrega consigo o contexto completo de como foi gerada — não apenas o conteúdo, mas o "porquê":

### O que é salvo no `ai_run` (por tentativa de geração)

```
provider:         "openrouter"
model:            "meta-llama/llama-3-8b-instruct"
prompt_used:      "[system prompt completo + user prompt]"
tokens_used:      1847
cost_usd:         0.00294
duration_ms:      3420
status:           "success"
created_at:       "2026-06-11T14:23:01Z"
```

Isso permite:
- **Auditoria**: saber exatamente qual modelo e provedor foram usados em cada geração.
- **Custo**: estimar quanto foi gasto por usuário/mês.
- **Debugging**: se uma versão ficou ruim, inspecionar o prompt e a resposta.
- **Otimização**: comparar qualidade vs. custo entre provedores.

### O que é salvo no `analysis_report` (por versão)

```
adherence_score:    72
adherence_level:    "medium"
key_strengths:      "Experiência com Java e Spring Boot diretamente relevante..."
gaps:               "Falta experiência com Kubernetes e AWS em produção..."
keyword_map:        {"required": ["Java", "Spring Boot", "PostgreSQL"],
                     "matched":  ["Java", "Spring Boot"],
                     "missing":  ["Kubernetes", "AWS"]}
positioning_strategy: "Destacar experiência com sistemas de alta disponibilidade..."
raw_ai_response:    "[resposta completa da IA em texto]"
```

### Por que salvar `raw_ai_response`

A resposta bruta da IA é salva como `TEXT` no campo `raw_ai_response`. Isso é crítico porque:

1. **Debugging**: se o parsing falhar ou produzir resultados inesperados, a resposta original está disponível.
2. **Reprocessamento**: se o schema de parsing mudar no futuro, é possível reprocessar as respostas brutas salvas.
3. **Auditoria de IA**: em casos de contestação (currículo com informação incorreta), a resposta original documenta o que a IA retornou.
4. **Compliance**: em cenários futuros de regulação de IA, ter logging completo é importante.

---

## Comparação entre Versões (Futuro)

A funcionalidade de comparação visual entre versões (diff) não faz parte do MVP, mas é documentada aqui para garantir que o modelo de dados a suporta.

### Dados necessários para diff

- Duas versões de `content_markdown` (já estão salvas).
- Metadados: data, versão, tipo (geração IA ou edição manual).
- IA usada: provedor, modelo, prompt.

### Abordagem técnica para diff futuro

1. Client-side: usar `diff` library (ex: `diff-match-patch`) para calcular diff textual entre dois markdown.
2. Backend endpoint: `GET /api/generated/{id}/diff?compareWithVersion=2` — retorna diff estruturado.
3. Frontend: exibir diff lado a lado (two-column) com highlights em additions (verde) e deletions (vermelho).

### Abordagem rejeitada

- Salvar diffs como colunas no banco: **não** — diffs são derivados dos conteúdos, não dados primitivos. Gerar sob demanda é suficiente.

---

## Rollback (Futuro)

Rollback significa tornar uma versão anterior a versão atual (is_current=true).

### Fluxo de rollback

1. Usuário acessa `/generated/{id}` e clica "Restaurar esta versão".
2. Frontend envia `POST /api/generated/{id}/rollback`.
3. Backend:
   - Busca versão selecionada e todas as versões mais recentes.
   - Marca versão selecionada como `is_current = true`.
   - Marca versões mais recentes como `is_current = false`.
   - Cria um registro de log em `processing_logs`.
4. Retorna sucesso. Frontend atualiza tela.

### Regra de rollback

- Apenas versões criadas a partir da mesma `resume_profile` + `job_application` podem ser restauradas.
- O rollback **não deleta** as versões mais recentes — elas ficam no histórico como não atuais.
- Rollback cria um novo registro? **Não no MVP** — marcação direta de `is_current` é suficiente. Rollback com novo registro seria mais complexo e não é necessário.

---

## Estratégia de Query para Histórico

### Listar currículos gerados (página principal de histórico)

```sql
SELECT
    gr.id,
    gr.version_number,
    gr.created_at,
    gr.is_current,
    ja.company_name,
    ja.job_title,
    ar.adherence_score,
    ar.adherence_level,
    (SELECT COUNT(*) FROM generated_resumes
     WHERE job_application_id = gr.job_application_id
       AND resume_profile_id = gr.resume_profile_id) AS version_count
FROM generated_resumes gr
JOIN job_applications ja ON gr.job_application_id = ja.id
LEFT JOIN analysis_reports ar ON ar.generated_resume_id = gr.id
WHERE gr.user_id = :userId
  AND gr.is_current = true
ORDER BY gr.created_at DESC
LIMIT 20 OFFSET 0;
```

### Listar versões de um currículo específico

```sql
SELECT id, version_number, created_at, is_current
FROM generated_resumes
WHERE job_application_id = :jobApplicationId
  AND resume_profile_id = :resumeProfileId
ORDER BY version_number DESC;
```

### Buscar versões por empresa

```sql
SELECT gr.*, ja.company_name, ja.job_title
FROM generated_resumes gr
JOIN job_applications ja ON gr.job_application_id = ja.id
WHERE gr.user_id = :userId
  AND ja.company_name ILIKE '%' || :company || '%'
ORDER BY gr.created_at DESC;
```

Nota: usar `ILIKE` para busca case-insensitive. Em produção com alto volume, adicionar índice `GIN` em `company_name` ou usar full-text search.

---

## Versionamento do Template DOCX

O template de .docx não é versionado no MVP — é código Java imutável durante uma release.

Se o template precisar mudar (ex: adicionar nova seção "Projetos"):
1. Nova versão da classe `DocxTemplateV2.java` é criada.
2. A versão antiga é deprecada mas mantida.
3. Decisão de qual template usar pode ser por:
   - Configuração global (todas as gerações usam V2).
   - Flag no `generated_resumes` (futuro: `template_version` column).

No MVP: uma única versão do template, atualizada via deploy.