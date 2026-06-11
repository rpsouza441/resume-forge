# Riscos Técnicos e Decisões Pendentes

## Riscos Técnicos

### Risco 1: Qualidade da IA Generativa — Informações Fabricadas

**Severidade**: Alta
**Probabilidade**: Média-Alta

O risco mais crítico do sistema. Modelos de linguagem podem "alucinar" — inventar experiências, certificações ou tecnologias que não existem no currículo base.

**Mitigação**:
- Regras hardcoded no system prompt (documentadas em `05-regras-ia.md`): nunca inventar, nunca transformar básico em avançado, nunca fazer keyword stuffing.
- `raw_ai_response` salvo sempre — mesmo que a saída pareça boa, a resposta original está logada.
- Análise de aderência com `adherence_score` — se o score for muito alto mas o currículo base não sustenta, a análise de lacunas deve indicar isso.
- Interface permite que usuário edite e revise — o produto não é "enviar sem revisar".
- Na dúvida, o sistema deve ser conservador: melhor um currículo levemente desalinhado do que um com informações falsas.

**Residual risk**: não é zero. O sistema deve informar explicitamente ao usuário que "este currículo foi gerado por IA e deve ser revisado antes do envio".

---

### Risco 2: Acoplamento ao Provedor de IA

**Severidade**: Alta
**Probabilidade**: Média

Se o provedor gratuito chosen (Gemini free tier ou OpenRouter free tier) alterar termos, limitar rate limits ou ficar indisponível, o sistema para de funcionar.

**Mitigação**:
- Abstração de provedor (Strategy pattern) — troca de provedor = mudança de configuração, não de código.
- Configuração de `fallbackProvider` — se provedor primário falhar, tentar secundário automaticamente.
- Logs em `ai_runs` com provider, model, status — permite identificar qual provedor está falhando.
- Rate limit handling: retry com backoff exponencial.
- Avaliar regularmente alternativas: se Gemini free tier acabar, migrar para OpenRouter com outro modelo gratuito.

**Residual risk**: migrations entre provedores requerem testes — a qualidade da saída pode variar. Manter histórico de `ai_runs` permite comparar.

---

### Risco 3: Custo de IA Descontrolado

**Severidade**: Média-Alta
**Probabilidade**: Média

Cada geração de currículo consome tokens. Se o usuário fizer 100 regenerações por dia, o custo pode escalar rapidamente dependendo do provedor.

**Mitigação**:
- Logs em `ai_runs` com `cost_usd` — visibilidade de gasto por usuário.
- No MVP não há limite de uso — mas o logging permite entender o padrão antes de implementar limits.
- Fase 4 adiciona limites por plano.
- Começar com provedores com tier gratuito generoso (Gemini 1.5 Flash, OpenRouter free tier).

**Residual risk**: se muitos usuários fizerem uso intensivo, o custo operacional pode superar a receita (quando houver). Monitorar `cost_usd` por `user_id`.

---

### Risco 4: Dados no Banco sem Validação de Schema JSONB

**Severidade**: Baixa no MVP, Média na Fase 3+

**Probabilidade**: Alta no MVP

O campo `content_jsonb` não é validado no MVP. O usuário pode salvar JSONB malformado ou com estrutura inesperada (ex: IA retorna JSON com campos diferentes do schema esperado).

**Mitigação**:
- Frontend pode usar validação Zod antes de enviar.
- Backend não valida schema no MVP — confia no frontend.
- Fase 3 adiciona validação com JSON Schema.
- `raw_ai_response` permite reprocessamento se o parsing falhar.

**Residual risk**: se a IA mudar o formato de resposta, o parsing pode quebrar silenciosamente. Monitorar taxa de `content_jsonb = NULL` em novas gerações.

---

### Risco 5: Geração de .docx Quebra em Conteúdo Extremo

**Severidade**: Média
**Probabilidade**: Baixa

Se o currículo gerado tiver formatação markdown incomum, código inline, tabelas ou outros elementos não suportados pelo conversor, o .docx pode ficar quebrado.

**Mitigação**:
- O prompt de IA deve instruir claramente: "use apenas markdown padrão (headings, bullets, bold). Não use tabelas, código, ou imagens."
- Converter com fallback: se o parser falhar, gerar .docx com texto plano (sem formatação).
- Log de erros de conversão em `processing_logs`.
- No MVP, o template é simples o suficiente para suportar a maioria dos casos.

**Residual risk**: currículos muito longos (> 5 páginas) podem ter problemas de formatação. Limitar a 2 páginas no prompt.

---

### Risco 6: Segurança da Autenticação (JWT)

**Severidade**: Alta
**Probabilidade**: Baixa

JWT com `alg: HS256` pode ser vulnerável se a secret key for fraca ou exposta.

**Mitigação**:
- Secret key lida de variável de ambiente, não de código.
- `alg: RS256` (assimétrico) é mais seguro que `HS256` (simétrico) — avaliar na Fase 2+.
- Expiração curta (1h access token).
- Refresh token com rotação (invalidar refresh token após uso).
- HTTPS obrigatório (validar no Spring Security config).

**Residual risk**: se o refresh token for roubado, um atacante pode gerar novos access tokens. Adicionar fingerprinting (user-agent + IP) no refresh token validation.

---

### Risco 7: LGPD e Dados Pessoais

**Severidade**: Alta
**Probabilidade**: Média

O sistema armazena dados pessoais (nome, email, telefone, histórico profissional). Isso é regulado pela LGPD (Lei Geral de Proteção de Dados).

**Mitigação**:
- Não armazenar dados além do necessário.
- `is_active` flag em users permite soft delete (não exclusão real no MVP).
- Em produção: Política de Privacidade, Termos de Uso, botão de exportação/exclusão de dados.
- Dados enviados à IA: incluir no privacy notice que dados podem ser processados por provedores de IA (OpenAI, Google, etc.).
- Não enviar dados desnecessários para o provedor de IA (ex: email, telefone não são necessários para gerar o currículo otimizado — omitir do prompt).

**Residual risk**: compliance total com LGPD requer análise jurídica e possibly designação de DPO. MVP deve ser lançado com notice básico.

---

### Risco 8: Persistência de Dados — Sem Backup Automatizado

**Severidade**: Alta
**Probabilidade**: Média no início, Baixa após setup

Se o PostgreSQL perder dados (falha de disco, erro humano), todo o histórico de currículos e versões é perdido.

**Mitigação**:
- Configurar backup automatizado do PostgreSQL (pg_dump diário + retenção de 30 dias).
- Em produção (VPS/cloud): configurar backup automático do provider (RDS, Cloud SQL, etc.).
- Testar restore procedure periodicamente.

**Residual risk**: se o backup falhar silenciosamente, pode não ser detectado até precisarem restaurar. Monitorar sucesso/falha de backups.

---

## Riscos de Produto

### Risco 9: Usuário Não Revisa o Currículo Gerado

**Severidade**: Alta (reputacional)
**Probabilidade**: Alta

Se o usuário baixar e enviar um currículo com informações fabricadas pela IA sem revisar, pode enfrentar consequências sérias (despedida por falsificação).

**Mitigação**:
- Disclaimer obrigatório na tela de geração: "Revise o currículo antes de enviar. Não nos responsabilizamos por informações incorretas."
- Na Fase 2: avaliação de qualidade após download — se muitos usuários reportam "enviei sem revisar", adicionar step de confirmação "Você revisou este currículo?"
- Interface de edição clara e acessível — não é óbvio que o currículo pode ser editado.

---

### Risco 10: Baixa Adesão por Complexidade de Uso

**Severidade**: Alta
**Probabilidade**: Média

Se o fluxo for muito longo (5+ passos para gerar um currículo), o usuário abandona.

**Mitigação**:
- MVP com fluxo mínimo: cadastrar currículo base → colar vaga → gerar → baixar (4 passos).
- Primeira vaga pode ser criada na mesma tela da geração (não precisa ser fluxo separado).
- Currículo base pode ser criado por colagem de texto em 30 segundos.

---

## Decisões Tomadas

| # | Decisão | Justificativa | Data |
|---|---------|--------------|------|
| D01 | PostgreSQL como fonte da verdade | Simplicidade, custo zero de storage, backup trivial | 2026-06-11 |
| D02 | TEXT (Markdown) + JSONB para currículo | Flexibilidade sem excesso de engenharia | 2026-06-11 |
| D03 | .docx gerado sob demanda | Evita stale files, storage overhead, sincronização | 2026-06-11 |
| D04 | Spring Boot (Java) como backend | Type safety, maturidade, sem segunda runtime | 2026-06-11 |
| D05 | Next.js (App Router) como frontend | SSR, ecossistema, deploy simples | 2026-06-11 |
| D06 | Apache POI (XWPF) para .docx | Biblioteca Java nativa, sem dependência externa | 2026-06-11 |
| D07 | Strategy pattern para IA | Troca de provedor via configuração, sem refatoração | 2026-06-11 |
| D08 | Sem worker Python no MVP | Java é suficiente para todas as etapas do MVP | 2026-06-11 |
| D09 | Sem multi-tenant no MVP | Simplifica auth e modelo de dados | 2026-06-11 |
| D10 | Sem storage externo no MVP | Volume não justifica complexidade adicional | 2026-06-11 |
| D11 | Sem validação de schema JSONB no MVP | Flexibilidade > rigidez; Frontend é a fonte de dados confiáveis | 2026-06-11 |
| D12 | version_number + parent_version_id para versões | Simplicidade, permite audit trail completo sem event sourcing | 2026-06-11 |
| D13 | PostgreSQL como fonte da verdade (vs. pasta local vs. bucket) | Consistência ACID, backup trivial, sem custo adicional de storage | 2026-06-11 |
| D14 | Geração sob demanda de .docx (vs. salvar binário no banco vs. storage) | Elimina stale files, backup simples, zero overhead de storage | 2026-06-11 |

---

## Decisões Pendentes

| # | Questão | Opções | Prioridade | Quem Decide |
|---|---------|--------|-----------|------------|
| P01 | Qual provedor de IA usar no MVP? | Gemini (gratuito, 60 req/min), OpenRouter (gratuito, modelos variados), OpenAI (pago, melhor qualidade) | Alta | Product Owner + Tech Lead |
| P02 | Onde fazer deploy? | Vercel (Next.js) + Railway/Render (Spring Boot) + Supabase (PostgreSQL) vs. tudo em um VPS (DigitalOcean, AWS EC2) | Alta | Tech Lead |
| P03 | Formato de token JWT? | HS256 (simples) vs. RS256 (assimétrico, mais seguro) | Média | Tech Lead |
| P04 | Validação de input no backend? | Bean Validation (JSR-380) vs. manual validation | Baixa | Tech Lead |
| P05 | Frontend state management: React Query suficiente? | React Query + Context vs. Zustand vs. Redux | Baixa | Tech Lead |
| P06 | CSS: Tailwind ou CSS Modules? | Tailwind (mais popular, mais documentação) vs. CSS Modules (mais controlado) | Baixa | Tech Lead |
| P07 | CI/CD: GitHub Actions? | GitHub Actions vs. GitLab CI vs. outro | Baixa | DevOps |

---

## Critérios Objetivos para Considerar o MVP Pronto

O MVP é considerado pronto para produção quando todos os seguintes critérios forem satisfeitos:

### Funcionalidade

- [ ] Usuário consegue se registrar e fazer login com JWT.
- [ ] Usuário consegue criar, editar e excluir um currículo base.
- [ ] Usuário consegue criar uma vaga colando a descrição.
- [ ] Usuário consegue selecionar currículo base + vaga e acionar geração via IA.
- [ ] Sistema retorna currículo otimizado + análise de aderência + mapa de keywords.
- [ ] Usuário consegue editar o currículo gerado e salvar nova versão.
- [ ] Usuário consegue listar histórico filtrando por empresa e data.
- [ ] Usuário consegue baixar currículo em .docx ATS-friendly.
- [ ] Múltiplas versões de um currículo para a mesma vaga são preservadas e acessíveis.

### Técnico

- [ ] Todas as chamadas de IA são logadas em `ai_runs` (prompt, resposta, tokens, custo, provider, modelo).
- [ ] Provedor de IA pode ser trocado alterando apenas `application.yml` (sem mudança de código).
- [ ] Tempo de resposta da API (exceto geração IA): < 200ms para leitura, < 500ms para escrita.
- [ ] Tempo de geração com IA: < 30 segundos (timeout configurável).
- [ ] Tempo de geração de .docx: < 3 segundos.
- [ ] Senhas hasheadas com BCrypt (cost >= 12).
- [ ] JWT com expiração de 1 hora.
- [ ] Todas as comunicações via HTTPS.
- [ ] Backup automatizado do PostgreSQL configurado e testado.
- [ ] Testes unitários para lógica de domínio (serviços de geração, versão, autenticação).
- [ ] Código documentado com Javadoc/Springdoc.

### UX

- [ ] Todos os estados de loading implementados (botão com spinner, campos desabilitados).
- [ ] Mensagens de erro em linguagem de usuário (sem stack traces).
- [ ] Toast notifications para feedback de ações.
- [ ] Estados vazios implementados para todas as telas de lista.
- [ ] Disclaimer visível na tela de resultado da geração: "Revise o currículo antes de enviar."

### Segurança

- [ ] Validação de input em todas as camadas (backend é a source of truth).
- [ ] Verificação de ownership em todos os endpoints (usuário só acessa seus próprios dados).
- [ ] CORS configurado para origens permitidas.
- [ ] Variáveis sensíveis (JWT secret, API keys) lidas de variáveis de ambiente.

### Infraestrutura

- [ ] Deploy automatizado (CI/CD configurado).
- [ ] Backup automatizado do PostgreSQL.
- [ ] Health check endpoint (`/actuator/health`).
- [ ] Logs estruturados para debugging em produção.

---

## Critérios para Mudança de Arquitetura

| Gatilho | Ação |
|---------|------|
| Volume de uploads > 100 arquivos/dia | Avaliar storage externo (S3/MinIO) |
| Tempo médio de geração .docx > 5s | Adicionar cache de .docx ou storage |
| Custo de IA > R$ 0,10 por geração | Trocar para provedor mais barato, adicionar limits por usuário |
| JSONB malformado causing crashes > 1%/dia | Adicionar validação de schema + JSON Schema |
| Tempo de query no histórico > 500ms | Adicionar índices, considerar materialized views |
| Taxa de erro de IA > 10% | Trocar provedor ou modelo |
| LGPD: pedido de exclusão de dados | Implementar hard delete + confirmação por email |