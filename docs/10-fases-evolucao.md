# Fases de Evolução do Produto

## Visão Geral do Roadmap

O produto evolui em 4 fases, do MVP funcional até um SaaS completo com multi-tenant, billing e integrações. Cada fase adiciona valor significativo sem reprojetar o que já foi construído.

```
Fase 1 (MVP) ──→ Fase 2 ──→ Fase 3 ──→ Fase 4 (SaaS Completo)
  ~4-8 sem         ~3-4 sem      ~4-6 sem       ~8-12 sem
```

---

## Fase 1 — MVP (Escopo Atual)

**Objetivo**: Entregar um sistema funcional de ponta a ponta que resolva o problema core: gerar currículos otimizados para vagas usando IA.

### Funcionalidades Incluídas

- [x] Autenticação (registro, login, logout com JWT)
- [x] CRUD de currículo base (formulário estruturado + colagem de texto)
- [x] CRUD de vaga de emprego (colagem de descrição)
- [x] Geração de currículo otimizado via IA (abstração configurável)
- [x] Análise de aderência (score, keywords, lacunas, estratégia)
- [x] Edição manual de currículo gerado com versionamento
- [x] Histórico de currículos gerados por empresa/vaga/data
- [x] Exportação em .docx sob demanda (ATS-friendly)
- [x] Logs de execução da IA (prompt, resposta, tokens, custo)
- [x] API REST completa

### Critério de Conclusão da Fase 1

- [x] Usuário consegue se cadastrar, criar currículo base, colar vaga, gerar currículo otimizado, editar, versionar e baixar .docx.
- [x] Currículo baixado passa em validação visual básica (seções corretas, sem tabelas, legível).
- [x] Sistema gera logs de todas as chamadas de IA.
- [x] Provedor de IA pode ser trocado via configuração sem mudança de código.
- [x] Tempo de resposta da API < 200ms (exceto geração com IA).
- [x] Geração com IA completa em < 30 segundos.
- [x] Geração de .docx em < 3 segundos.
- [x] Todas senhas hasheadas com BCrypt (cost >= 12).
- [x] JWT com expiração de 1 hora.

---

## Fase 2 — Pesquisa Salarial e IA Avançada

**Objetivo**: Adicionar valor à análise sem poluir o currículo final, e melhorar a qualidade da IA com melhores prompts.

### Funcionalidades Planejadas

#### 2.1 — Pesquisa Salarial

- **Entrada**: cargo + senioridade + localização (mesmos dados já coletados na vaga).
- **Fontes**: APIs públicas ou scraping leve de sites como:
  - Glassdoor (API oficial ou scraper).
  - Love Mondays / Glassdoor Brasil.
  - Gupy (APIs internas de vagas com informação salarial).
  - Bureau de dados salarial (ex: RAIS, IBGE — dados públicos).
- **Interface**: módulo separado do currículo — relatório salarial visual, não inserido no documento.
- **Dados salvos**:
  - `salary_researches`: {id, user_id, job_application_id, cargo, senioridade, location, source, source_url, salary_min, salary_max, salary_median, confidence_level, fetched_at}
  - `salary_sources`: {id, source_name, base_url, api_key, is_active}
- **Regras**:
  - Não inserir pretensão salarial no currículo (regra da Fase 1 se mantém).
  - Salvar fonte, data, URL e nível de confiança junto com cada dado.
  - Cache: não reconsultar a mesma fonte para o mesmo cargo em menos de 24h.
- **Critérios de implementação**:
  - Usar provedor de busca configurável (mesma abstração da IA).
  - Interface simples: "Ver pesquisa salarial" botão na tela de vaga.
  - Relatório: range salarial com benchmark por nível.

#### 2.2 — IA Avançada

- **Few-shot examples**: adicionar exemplos de prompts+respostas boas no system prompt para melhorar qualidade.
- **Prompt library**: salvar prompts otimizados por tipo de vaga (tech, marketing, executivo).
- **Avaliação de qualidade**: após download, pedir ao usuário uma nota (1-5) — salvar para análise de qualidade por provedor/modelo.

#### 2.3 — Melhorias de UX

- Preview do .docx no browser (sem download) usando docx-preview.js.
- Bulk generation: gerar para múltiplas vagas a partir do mesmo currículo base.
- Template de currículo customizável (cor, fonte) — armazenado como configuração de usuário, não como arquivo.

### Critério de Conclusão da Fase 2

- Relatório salarial funcional com pelo menos uma fonte brasileira.
- Qualidade de currículo gerado mensurável (avaliação do usuário).
- Sem mudanças no modelo de dados que quebrem a Fase 1.

---

## Fase 3 — Upload de Arquivos e Extração

**Objetivo**: Permitir que o usuário carregue currículos existentes (PDF, DOCX) e o sistema extraia o conteúdo automaticamente.

### Funcionalidades Planejadas

#### 3.1 — Upload de Currículo Existente

- Upload de arquivos PDF (currículo escaneado ou digital).
- Upload de arquivos DOCX (currículo Word).
- Suporte a arquivos de até 10MB.
- Interface: drag-and-drop na tela de currículo base.

#### 3.2 — Extração de Texto

**Opção A: Biblioteca Java (preferida para MVP da Fase 3)**
- PDF: Apache PDFBox ou iText.
- DOCX: Apache POI (XWPF) — já usado para geração, reutilizado para leitura.
- Prós: tudo em Java, uma única runtime, deploy simples.
- Contras: extração de PDF escaneado depende de OCR (Tesseract via JNI ou serviço externo).

**Opção B: Worker Python (se Java não for suficiente)**
- Usar `pdfplumber` ou `PyMuPDF` para PDF, `python-docx` para DOCX.
- Mais preciso para PDFs escaneados (com OCR nativo).
- Requer: endpoint REST no worker Python, comunicação HTTP entre Spring Boot e worker.
- Justificativa técnica: parsing de PDF com OCR é a única etapa onde Python supera significativamente bibliotecas Java. Se o MVP usar apenas PDFs digitais (não escaneados), Java é suficiente.

**Decisão**: começar com Opção A (Java only). Migrar para Opção B (worker Python) apenas se a qualidade de extração de PDFs escaneados for inaceitável.

#### 3.3 — Armazenamento de Arquivos

- Armazenamento inicial: salvar arquivos no PostgreSQL como `BYTEA` (para arquivos pequenos, < 1MB).
- Migração futura: mover para storage externo (S3/MinIO) quando volume crescer.
- Campos em `resume_profiles`:
  - `original_file_name`: VARCHAR(255)
  - `original_file_type`: VARCHAR(50) — pdf, docx
  - `original_file_data`: BYTEA (MVP) ou VARCHAR (URL do storage externo — fase 4)
  - `extracted_text`: TEXT — texto extraído para debug

#### 3.4 — Validação de Schema JSONB

- Implementar validação de schema JSONB para `content_jsonb`.
- Usar JSON Schema (com library como `everit-org/json-schema` em Java).
- Validar no save: se o JSONB não seguir o schema, salvar anyway (não bloquear) mas logar warning.

### Critério de Conclusão da Fase 3

- Usuário consegue fazer upload de PDF ou DOCX e o conteúdo aparece no formulário de currículo.
- Extração de texto para PDFs digitais com > 95% de accuracy.
- Arquivos salvos no banco ou em storage (se implementado).

---

## Fase 4 — SaaS Completo

**Objetivo**: Transformar o produto em um SaaS multi-tenant com planos, billing e integrações.

### Funcionalidades Planejadas

#### 4.1 — Multi-Tenant

- Isolamento por organização (empresa/equipe).
- Cada organização tem seus próprios usuários, currículos e vagas.
- Implementação: adicionar `organization_id` em todas as tabelas.
- Row-Level Security (RLS) no PostgreSQL para reforçar isolamento.
- Alternativa: schema por organização (mais complexo, melhor isolamento) — avaliar se necessário.

#### 4.2 — Planos e Billing

- Planos: Grátis (X gerações/mês), Pro (Y gerações/mês), Enterprise (ilimitado).
- Limites por plano: quantidade de currículos base, vagas, gerações por mês.
- Billing: integração com gateway de pagamento (Stripe no Brasil com webhook).
- Modelos de cobrança: assinatura mensal/anual.

#### 4.3 — Storage Externo de Arquivos

- Migração de `BYTEA` para S3/MinIO/Supabase Storage.
- Motivação: arquivos grandes (> 1MB) ou alto volume começam a pesar no PostgreSQL.
- Estratégia: manter metadados no banco, binários no storage.
- CDN para arquivos públicos (currículos baixados com frequência).

#### 4.4 — Observabilidade

- Métricas: Prometheus + Grafana.
- Logs agregados: ELK stack ou Datadog.
- Tracing: OpenTelemetry para rastrear requests entre serviços.
- Alertas: quando taxa de erro de IA > 5%, quando tempo de resposta > 10s.

#### 4.5 — Integrações com ATS

- Integração com ATS brasileiros: Gupy, Rhizome, Vagas.com.br.
- Push de currículo diretamente para o ATS (SSO OAuth).
- Webhook para receber atualizações de status da vaga.

#### 4.6 — Templates de Currículo

- Editor visual de template (sem código).
- Múltiplos templates por organização.
- Preview em tempo real.
- Versionamento de templates.

#### 4.7 — Colaboração

- Convite de membros para organização.
- Permissões: admin, editor, viewer.
- Comentários em currículos (para recruiters revisarem).
- Atribuição de versão: "gerado por Maria, revisado por João."

---

## Resumo Comparativo das Fases

| Aspecto | Fase 1 (MVP) | Fase 2 | Fase 3 | Fase 4 |
|--------|-------------|--------|--------|--------|
| Usuários | 1 por conta | Ilimitado | Ilimitado | Multi-tenant |
| Billing | Não | Não | Não | Sim (Stripe) |
| Storage | PostgreSQL | PostgreSQL | PostgreSQL + BYTEA | S3/Storage |
| Worker Python | Não | Não | Opcional | Sim |
| Upload de arquivos | Não | Não | Sim | Sim |
| Pesquisa salarial | Não | Sim | Sim | Sim |
| Templates customizados | Não | Não | Não | Sim |
| Integrações ATS | Não | Não | Não | Sim |
| Observabilidade | Logs básicos | Logs + métricas | Logs + métricas | ELK + tracing |
| Multi-tenant | Não | Não | Não | Sim |