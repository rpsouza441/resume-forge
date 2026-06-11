# SPEC-01 — Contexto do Produto

**Projeto:** ResumeForge
**Versão do documento:** 1.0
**Data:** 2026-06-11
**Status:** Aprovado para implementação

---

## 1. Visão do Projeto

### Nome do Sistema

**ResumeForge** — Plataforma SaaS de Otimização de Currículos com IA Generativa

### Problema Resolvido

Candidatos a vagas de emprego enfrentam um problema recorrente e custoso em tempo: adaptar o currículo para cada vaga é um processo manual, lento e sem metodologia. O candidato precisa reler a descrição da vaga, identificar palavras-chave e requisitos, reescrever ou reorganizar o currículo para maximizar aderência e repetir isso para cada candidatura — frequentemente 10, 20 ou 50 vezes.

O resultado é ou um currículo genérico enviado a todas as vagas (baixa conversão) ou horas desperdiçadas em customização manual (baixa produtividade). Além disso, não há registro sistemático de qual versão foi enviada para qual vaga, nem análise de qual abordagem funcionou melhor.

### Princípio Central de Design

> **Conteúdo no banco. Arquivos gerados sob demanda.**

Este princípio é a espinha dorsal de todas as decisões de arquitetura do MVP:

- **PostgreSQL como fonte da verdade** — backup trivial, consistência transacional, zero storage overhead para binários.
- **.docx gerado sob demanda** — arquivo sempre atual, sem sincronização, sem stale files.
- **IA como processamento stateless** — cada geração é uma nova chamada, sem estado acumulado.
- **Histórico por versão** — cada edição gera uma nova linha, nunca sobrescreve a anterior.

### O Que Diferencia Este Sistema de um Prompt Manual em IA

| Aspecto | Prompt manual (ChatGPT, Gemini) | ResumeForge |
|---------|--------------------------------|-------------|
| Prompt engineering | Ad hoc, varia por tentativa | Sistemático, fixo, otimizado para currículos |
| Saída estruturada | Texto livre, variável | Texto + Markdown + JSONB, formato consistente |
| Versionamento | Nenhum | Histórico completo por vaga e data |
| Revisão e edição | Copiar/colar manualmente | Interface de edição com salvamento de versão |
| Análise de aderência | O usuário precisa inferir | Fornecida explicitamente pela IA |
| Mapa de palavras-chave | O usuário precisa deduzir | Fornecido explicitamente pela IA |
| Organização | Planilhas, anotações mentais | Histórico estruturado no banco |
| Exportação | Copiar/colar em template Word | Download .docx ATS-friendly imediato |

---

## 2. Personas

### Persona A — Carolina (Dev Júnior)

| Atributo | Descrição |
|----------|-----------|
| **Nome** | Carolina, 23 anos |
| **Perfil** | Recém-formada em Ciência da Computação, 1-2 anos de experiência informal (projetos pessoais, freelance, estágio curto). Busca primeiro emprego CLT em desenvolvimento backend. |
| **Necessidade principal** | Apresentar-se de forma competitiva mesmo com pouca experiência formal. Destacar projetos GitHub, tecnologias e eagerness to learn, sem parecer "junior demais". |
| **Dor principal** | Ao colar sua experiência em uma vaga de backend, não sabe se deve mencionar o estágio de suporte; se deve listar todos os projetos ou só os mais relevantes; se parecerá "muito júnior". Não sabe o que incluir ou omitir. |
| **Como o sistema ajuda** | A IA identifica as keywords da vaga, aponta lacunas e gera uma versão que destaca projetos e tecnologias relevantes sem inventar experiência. A análise de aderência mostra explicitamente o que está forte e o que falta. |

---

### Persona B — Rafael (Transição de Carreira)

| Atributo | Descrição |
|----------|-----------|
| **Nome** | Rafael, 31 anos |
| **Perfil** | 6 anos de experiência em suporte técnico, fazendo transição para desenvolvimento backend. Sabe Python, fez cursos, fez projetos pessoais. Currículo atual fala tudo de suporte — parece que não sabe programar. |
| **Necessidade principal** | Reformular completamente a narrativa do currículo para um novo contexto, mostrando competências transferíveis (resolução de problemas, comunicação técnica, debugging) junto com o que aprendeu em programação. |
| **Dor principal** | Tudo que fez antes parece irrelevante para a vaga de dev. Não sabe como conectar os pontos. A experiência passada parece desalinhada com a vaga nova. |
| **Como o sistema ajuda** | A IA gera uma versão que reposiciona a experiência de suporte como contexto valioso, destacando habilidades transferíveis e projetos técnicos, reinterpretando a experiência existente de forma honesta e estratégica. |

---

### Persona C — Patrícia (Profissional Sênior)

| Atributo | Descrição |
|----------|-----------|
| **Nome** | Patrícia, 38 anos |
| **Perfil** | 12 anos de experiência em TI, trabalho em empresas grandes, múltiplas certificações e cursos. Currículo tem 4 páginas. Manda o mesmo currículo para tudo porque customizar leva 2 horas. |
| **Necessidade principal** | Selecionar e curar conteúdo relevante para cada vaga, mantendo o currículo em 2 páginas sem perder o que importa. Ter versões curtas e focadas para cada perfil de vaga. |
| **Dor principal** | Não sabe o que cortar. Vaga de liderança aparece e o currículo mostra demais (parece que ela está se candidatando a qualquer coisa); vaga de especialização técnica aparece e o currículo está genérico. |
| **Como o sistema ajuda** | A IA filtra e prioriza conteúdo mais relevante para cada perfil de vaga, gerando versões curtas e específicas. O score de aderência indica se o currículo está bem posicionado. O histórico permite comparar versões por vaga. |

---

## 3. Requisitos Funcionais

### RF-01: Autenticação e Gerenciamento de Sessão

**Descrição:** O sistema deve permitir que usuários se registrem, façam login e mantenham sessão de forma segura.

**Critérios de aceite:**
- [ ] O sistema deve permitir registro com nome completo, email e senha (mínimo 8 caracteres).
- [ ] O sistema deve不允许 registro de email duplicado — retornar erro 409 em caso de conflito.
- [ ] O sistema deve armazenar senhas exclusivamente como hash BCrypt com cost factor >= 12.
- [ ] O sistema deve permitir login com email e senha — retornar erro genérico 401 "Email ou senha incorretos" (sem revelar qual campo falhou) em caso de credenciais inválidas.
- [ ] O sistema deve emitir JWT access token com expiração de 1 hora ao login bem-sucedido.
- [ ] O sistema deve emitir JWT refresh token com expiração de 7 dias ao login bem-sucedido.
- [ ] O sistema deve permitir logout (o token perde validade).
- [ ] O sistema deve manter sessão via armazenamento no cliente (localStorage para access token no MVP).
- [ ] O sistema deve atualizar `last_login_at` do usuário a cada login.

**Endpoints relacionados:**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/refresh`

---

### RF-02: Gerenciamento de Currículo Base

**Descrição:** O usuário deve poder criar, editar, excluir e listar currículos base. O currículo base é o ponto de partida para todas as gerações.

**Critérios de aceite:**
- [ ] O usuário deve poder criar um currículo base com título personalizado (ex: "Currículo geral", "Dev Java").
- [ ] O usuário deve poder preencher dados pessoais: nome, email, telefone, localização, LinkedIn, GitHub.
- [ ] O usuário deve poder adicionar múltiplas entradas de experiência profissional (empresa, cargo, período, descrição, conquistas).
- [ ] O usuário deve poder adicionar múltiplas entradas de formação acadêmica (instituição, curso, período, status).
- [ ] O usuário deve poder listar skills técnicos e skills comportamentais.
- [ ] O usuário deve poder adicionar certificações (nome, emissor, data).
- [ ] O usuário deve poder salvar o currículo; o sistema deve gerar automaticamente `content_markdown` e `content_jsonb` a partir dos campos preenchidos.
- [ ] O usuário deve poder criar currículo via modo texto livre (colagem), sem passar pelo formulário estruturado.
- [ ] O usuário deve poder editar currículos base existentes.
- [ ] O usuário deve poder excluir currículos base (com confirmação).
- [ ] O usuário deve poder definir um currículo como padrão (`is_default = true`) — apenas um por usuário.
- [ ] O primeiro currículo criado por um usuário deve ser automaticamente definido como padrão.
- [ ] O sistema deve listar todos os currículos base do usuário logado, ordenados por data de criação decrescente.

**Endpoints relacionados:**
- `POST /api/resumes` — criar currículo base
- `GET /api/resumes` — listar currículos base do usuário
- `GET /api/resumes/{id}` — obter currículo base por ID
- `PUT /api/resumes/{id}` — editar currículo base
- `DELETE /api/resumes/{id}` — excluir currículo base
- `PUT /api/resumes/{id}/default` — definir como padrão

---

### RF-03: Gerenciamento de Vagas de Emprego

**Descrição:** O usuário deve poder criar, editar, excluir e listar vagas de emprego. Cada vaga contém a descrição colada pelo usuário.

**Critérios de aceite:**
- [ ] O usuário deve poder criar uma vaga informando empresa, título, localização, regime (CLT/PJ/freelancer/estágio/trainee), senioridade (júnior/pleno/sênior/lead/staff) e descrição completa.
- [ ] O campo `job_description_raw` deve ser obrigatório e ter no mínimo 100 caracteres.
- [ ] O usuário deve poder informar a URL de origem da vaga (LinkedIn, Gupy, etc.).
- [ ] O usuário deve poder listar vagas, com filtro opcional por empresa, título e status (active/closed/archived).
- [ ] O usuário deve poder editar vagas existentes.
- [ ] O usuário deve poder arquivar uma vaga (status = 'archived') sem excluí-la.
- [ ] O usuário deve poder excluir uma vaga definitivamente.
- [ ] Apenas o dono da vaga pode acessá-la — o sistema deve verificar `user_id` em todas as operações.

**Endpoints relacionados:**
- `POST /api/jobs` — criar vaga
- `GET /api/jobs` — listar vagas (com filtros opcionais)
- `GET /api/jobs/{id}` — obter vaga por ID
- `PUT /api/jobs/{id}` — editar vaga
- `DELETE /api/jobs/{id}` — excluir vaga

---

### RF-04: Geração de Currículo Otimizado com IA

**Descrição:** O sistema deve utilizar IA generativa para gerar uma versão otimizada do currículo base para uma vaga específica, incluindo análise de aderência.

**Critérios de aceite:**
- [ ] O usuário deve poder selecionar um currículo base e uma vaga para iniciar a geração.
- [ ] O sistema deve extrair keywords da descrição da vaga automaticamente.
- [ ] O sistema deve gerar uma versão do currículo com as keywords relevantes incorporadas naturalmente, sem inventar experiências.
- [ ] O sistema deve gerar análise de aderência contendo: score (0-100), nível (alta/média/baixa), pontos fortes, lacunas e mapa de keywords (matched, missing, partial).
- [ ] O sistema deve salvar todo o conteúdo gerado no banco: `content_text`, `content_markdown`, `content_jsonb` (se disponível) e `analysis_report`.
- [ ] O sistema deve salvar o prompt usado (`prompt_used`) e a resposta bruta da IA (`raw_ai_response`) em `ai_runs` para auditoria.
- [ ] O sistema deve registrar metadados da chamada: provider, model, tokens_used, cost_usd, duration_ms, status.
- [ ] O timeout de geração deve ser de 30 segundos — em caso de timeout, retry 1x com o mesmo provider; se falhar novamente, retornar erro 502.
- [ ] O sistema deve respeitar rate limits do provedor configurado.
- [ ] A IA deve ser proibida de inventar experiências, certificações, métricas ou níveis de proficiência não presentes no currículo base (definido nas regras do system prompt — ver `docs/05-regras-ia.md`).
- [ ] O sistema deve implementar camada de abstração (Strategy pattern) para provedor de IA — trocar provedor deve exigir apenas mudança de configuração, sem alteração de código.

**Endpoints relacionados:**
- `POST /api/generate` — iniciar geração (corpo: `{resumeProfileId, jobApplicationId}`)

---

### RF-05: Edição e Versionamento de Currículos Gerados

**Descrição:** O usuário deve poder editar currículos gerados pela IA e cada edição deve criar uma nova versão, preservando o histórico completo.

**Critérios de aceite:**
- [ ] O usuário deve poder editar o currículo gerado em tela (campo `content_markdown` editável).
- [ ] Cada edição deve criar uma nova versão, sem sobrescrever a versão anterior — o campo `is_current` é atualizado: a versão anterior recebe `false`, a nova recebe `true`.
- [ ] O sistema deve manter cadeia de versões via `version_number` (incrementa a cada nova versão) e `parent_version_id` (referência à versão anterior).
- [ ] O usuário deve poder consultar o histórico completo de versões de um currículo gerado.
- [ ] O usuário deve poder acessar qualquer versão anterior para visualização.
- [ ] O usuário deve poder comparar versões (diferencial visual — feature desejável, não bloqueante no MVP).
- [ ] O sistema deve registrar cada edição manual em `processing_logs` com `operation = 'GENERATED_RESUME_EDITED'`.

**Endpoints relacionados:**
- `GET /api/generated/{id}` — obter currículo gerado (versão atual)
- `PUT /api/generated/{id}` — editar currículo gerado (cria nova versão)
- `GET /api/generated/{id}/versions` — listar todas as versões
- `GET /api/generated/versions/{versionId}` — obter versão específica

---

### RF-06: Exportação para .docx

**Descrição:** O sistema deve permitir que o usuário baixe o currículo gerado em formato Word (.docx), gerado sob demanda a partir do conteúdo salvo no banco.

**Critérios de aceite:**
- [ ] O usuário deve poder baixar o currículo gerado em formato .docx a qualquer momento.
- [ ] O .docx deve ser gerado sob demanda no momento do download, a partir do campo `content_markdown` da versão atual.
- [ ] O arquivo .docx deve seguir template ATS-friendly: sem tabelas, sem imagens, sem colunas, sem headers/footers com informação crítica.
- [ ] O .docx deve utilizar fonte Calibri ou Arial, 11pt para corpo, 14pt para nome, 12pt para cabeçalhos de seção.
- [ ] O .docx deve ter margens de 2,5cm em todos os lados e espaçamento de 1,15 entre linhas.
- [ ] O nome do arquivo deve seguir o padrão: `[Nome]-otimizado-[Empresa]-[Data].docx` (ex: `Carolina-Souza-otimizado-CompanyX-2026-06-11.docx`).
- [ ] O tempo de geração do .docx deve ser inferior a 3 segundos.
- [ ] O sistema deve atualizar o campo `docx_generated_at` no registro a cada geração.

**Endpoints relacionados:**
- `GET /api/generated/{id}/docx` — baixar .docx do currículo gerado

---

### RF-07: Histórico e Consulta de Currículos Gerados

**Descrição:** O sistema deve manter histórico completo de todos os currículos gerados, permitindo consulta e filtragem.

**Critérios de aceite:**
- [ ] O usuário deve poder listar todos os currículos gerados, ordenados por data de geração decrescente.
- [ ] A lista deve permitir filtro por empresa, título da vaga e data (intervalo).
- [ ] Cada item da lista deve exibir: empresa, título, data de geração, score de aderência, número de versões.
- [ ] O usuário deve poder acessar o detalhe de qualquer currículo gerado (versão atual).
- [ ] O usuário deve poder acessar qualquer versão anterior do histórico.
- [ ] Apenas currículos gerados pelo próprio usuário devem ser retornados — filtragem por `user_id` obrigatória em todas as consultas.

**Endpoints relacionados:**
- `GET /api/generated` — listar currículos gerados (com filtros: `company`, `jobTitle`, `dateFrom`, `dateTo`, `page`, `size`)
- `GET /api/generated/{id}` — detalhe do currículo gerado atual
- `GET /api/generated/{id}/versions` — histórico de versões
- `GET /api/generated/versions/{versionId}` — versão específica

---

## 4. Requisitos Não-Funcionais

### RNF-01: Performance

| Métrica | Limite | Condição |
|---------|--------|----------|
| Tempo de resposta da API (excluindo IA) — operações de leitura | < 200ms | p95 medido em ambiente de produção simulada |
| Tempo de resposta da API (excluindo IA) — operações de escrita | < 500ms | p95 |
| Tempo de geração com IA (chamada ao provedor + parsing + salvamento) | < 30s | timeout configurável, retry 1x |
| Tempo de geração de .docx sob demanda | < 3s | medido do recebimento do request até início do stream |
| Tempo de carregamento inicial do frontend (LCP) | < 3s | conexão típica de banda larga brasileira |

---

### RNF-02: Disponibilidade

| Serviço | Disponibilidade alvo | Observação |
|---------|---------------------|-----------|
| Sistema de autenticação (login, registro, refresh) | 99% | Funcionalidade crítica — downtime impede uso |
| API de geração de currículos | 95% | IA pode ter downtime; sistema deve informar usuário |
| API de operações CRUD (currículos, vagas, histórico) | 99% | Operações sem dependência externa |
| Frontend (Next.js) | 99% | Deployment em Vercel ou similar |

---

### RNF-03: Segurança

| Medida | Implementação obrigatória |
|--------|--------------------------|
| Hash de senhas | BCrypt com cost factor >= 12. Nunca armazenar senha em texto puro. |
| Autenticação | JWT access token com expiração de 1 hora; refresh token com expiração de 7 dias. |
| Comunicação | HTTPS em todas as comunicações (frontend ↔ backend, backend ↔ IA provider). |
| Validação de input | Todas as entradas devem ser validadas no backend (o frontend é conveniência, não segurança). |
| Autorização (ownership) | Toda entidade (resume, job, generated resume) deve verificar `user_id` antes de retornar ou modificar dados. Retornar 403 em caso de acesso não autorizado. |
| Dados em repouso | PostgreSQL com encryption em disco configurada (responsabilidade da infraestrutura). |
| Dados sensíveis na IA | E-mail, telefone, endereço e URLs de redes sociais não devem ser enviados ao provedor de IA. Devem ser reconstruídos localmente após a geração. |
| Rate limiting | Máximo de 10 chamadas simultâneas por usuário para endpoint de geração. Backoff exponencial em caso de erro 429 do provedor. |
| Headers de segurança | CSP, X-Frame-Options, X-Content-Type-Options configurados no Spring Security. |

---

### RNF-04: Escalabilidade

| Dimensão | Capacidade |
|---------|-----------|
| Usuários simultâneos | Centenas no MVP — dimensionamento para até 500 usuários concurrentes. |
| Currículos por usuário | Milhares — índices adequados em todas as tabelas. |
| Gerações simultâneas | Limitado pelo rate limit do provedor de IA configurado. Arquitetura stateless permite scale-out horizontal. |
| Armazenamento | PostgreSQL como fonte única — volume estimado no MVP: < 100MB de dados textuais por 1.000 usuários. |

---

### RNF-05: Manutenibilidade

| Prática | Obrigatório |
|---------|-------------|
| Documentação de código | Javadoc em todas as classes e métodos públicos; Springdoc/OpenAPI para endpoints. |
| Testes unitários | Mínimo de 70% de cobertura nas camadas de serviço e domínio; lógica de negócio обязательна. |
| Logs estruturados | Structured logging (JSON) para operações críticas: autenticação, geração de IA, erros. |
| Código versionado | Git com conventional commits; branch strategy documentada. |
| Separação de concerns | Arquitetura em módulos (auth, resume, job, generation, export, ai, logging) com dependências unidirecionais. |
| Abstração de IA | Interface `AiProvider` com implementações concretas por provedor — trocar provedor sem alteração de código de negócio. |

---

## 5. Casos de Uso

### CU-01: Registrar Conta

**Descrição:** O usuário acessa o sistema pela primeira vez e cria uma conta.

**Atores:** Usuário anônimo.
**Pré-condições:** Nenhuma.
**Fluxo principal:**
1. Usuário acessa a tela de registro.
2. Sistema exibe formulário: nome completo, email, senha, confirmação de senha.
3. Usuário preenche e submete.
4. Sistema valida: email único, senha >= 8 caracteres, confirmação igual.
5. Sistema cria conta com hash BCrypt.
6. Sistema redireciona para tela de login.
**Pós-condição:** Conta criada e pronta para login.

---

### CU-02: Login

**Descrição:** O usuário cadastrado acessa o sistema com suas credenciais.

**Atores:** Usuário registrado.
**Pré-condições:** Conta criada (CU-01).
**Fluxo principal:**
1. Usuário acessa a tela de login.
2. Sistema exibe formulário: email, senha.
3. Usuário preenche e submete.
4. Sistema valida credenciais contra banco.
5. Sistema emite access token (1h) e refresh token (7d).
6. Sistema redireciona para dashboard.
**Pós-condição:** Sessão ativa com token válido.

---

### CU-03: Cadastrar Currículo Base

**Descrição:** O usuário cadastra seu currículo base uma única vez para usar em múltiplas gerações.

**Atores:** Usuário autenticado.
**Pré-condições:** Sessão ativa (CU-02).
**Fluxo principal:**
1. Usuário acessa formulário de currículo base (modo estruturado ou modo texto livre).
2. Usuário preenche informações pessoais, experiência, educação, skills, certificações.
3. Sistema gera `content_markdown` e `content_jsonb` automaticamente.
4. Sistema salva currículo e define como padrão se for o primeiro.
5. Sistema redireciona para lista de currículos.
**Pós-condição:** Currículo base disponível para seleção em gerações.

---

### CU-04: Listar Currículos Base

**Descrição:** O usuário visualiza todos os seus currículos base cadastrados.

**Atores:** Usuário autenticado.
**Pré-condições:** Pelo menos um currículo base cadastrado (CU-03).
**Fluxo principal:**
1. Usuário acessa `/resumes` no dashboard.
2. Sistema exibe lista de currículos com título, data de criação, indicador de padrão.
3. Usuário pode editar ou excluir qualquer currículo.
**Pós-condição:** Lista exibida com ações disponíveis.

---

### CU-05: Criar Vaga de Emprego

**Descrição:** O usuário cadastra uma vaga colando a descrição da vaga desejada.

**Atores:** Usuário autenticado.
**Pré-condições:** Sessão ativa (CU-02).
**Fluxo principal:**
1. Usuário acessa formulário de criação de vaga.
2. Usuário informa empresa (opcional), título, localização, regime, senioridade.
3. Usuário cola descrição completa da vaga (mínimo 100 caracteres).
4. Usuário informa URL de origem (opcional).
5. Sistema salva vaga com status 'active'.
6. Sistema redireciona para wizard de seleção de currículo e geração.
**Pós-condição:** Vaga disponível para ser vinculada a uma geração.

---

### CU-06: Gerar Currículo Otimizado

**Descrição:** O usuário seleciona currículo base e vaga e solicita geração de currículo otimizado via IA.

**Atores:** Usuário autenticado.
**Pré-condições:** Currículo base criado (CU-03) e vaga criada (CU-05).
**Fluxo principal:**
1. Usuário seleciona currículo base (ou aceita o padrão).
2. Usuário seleciona ou cria vaga.
3. Usuário clica "Gerar currículo otimizado".
4. Sistema monta prompt com system prompt fixo + dados do currículo + descrição da vaga.
5. Sistema chama provedor de IA com timeout de 30s.
6. Sistema parseia resposta (currículo em markdown + análise de aderência em JSON).
7. Sistema salva currículo gerado em `generated_resumes` (v1, is_current=true) e análise em `analysis_reports`.
8. Sistema salva log em `ai_runs` (provider, model, tokens, cost, duration, prompt_used, raw_response).
9. Sistema retorna resultado ao frontend.
10. Sistema exibe currículo gerado + score de aderência + pontos fortes + lacunas.
**Pós-condição:** Currículo otimizado gerado, salvo e exibido ao usuário.

---

### CU-07: Editar Currículo Gerado e Salvar Nova Versão

**Descrição:** O usuário revisa o currículo gerado pela IA e faz ajustes manuais, salvando como nova versão.

**Atores:** Usuário autenticado.
**Pré-condições:** Currículo gerado existente (CU-06).
**Fluxo principal:**
1. Usuário acessa detalhe do currículo gerado.
2. Usuário clica em "Editar".
3. Sistema abre editor com `content_markdown` pré-preenchido.
4. Usuário faz alterações no campo de texto.
5. Usuário clica "Salvar".
6. Sistema marca versão atual como `is_current = false`.
7. Sistema cria nova versão (version_number + 1, parent_version_id = id da versão anterior).
8. Sistema redireciona para detalhe da nova versão.
**Pós-condição:** Nova versão criada e visualizada; versão anterior preservada no histórico.

---

### CU-08: Baixar Currículo em .docx

**Descrição:** O usuário baixa o currículo gerado em formato Word para enviar à vaga.

**Atores:** Usuário autenticado.
**Pré-condições:** Currículo gerado existente (CU-06 ou CU-07).
**Fluxo principal:**
1. Usuário acessa detalhe do currículo gerado.
2. Usuário clica no botão "Baixar DOCX".
3. Sistema gera arquivo sob demanda a partir do `content_markdown`.
4. Sistema converte markdown para XWPF (Apache POI) respeitando template ATS-friendly.
5. Sistema retorna stream HTTP com Content-Type application/vnd.openxmlformats-officedocument.wordprocessingml.document.
6. Browser dispara download com nome no padrão `[Nome]-otimizado-[Empresa]-[Data].docx`.
**Pós-condição:** Arquivo .docx baixado pelo usuário.

---

### CU-09: Consultar Histórico de Currículos Gerados

**Descrição:** O usuário consulta o histórico de todos os currículos gerados, filtrando por empresa, título ou data.

**Atores:** Usuário autenticado.
**Pré-condições:** Pelo menos um currículo gerado (CU-06).
**Fluxo principal:**
1. Usuário acessa `/history` no dashboard.
2. Sistema exibe lista paginada de currículos gerados (versão atual de cada par currículo × vaga).
3. Usuário aplica filtros: empresa, título, intervalo de datas.
4. Usuário clica em um item para ver o detalhe.
5. Usuário expande para ver todas as versões do histórico.
6. Usuário seleciona uma versão anterior para visualizar.
**Pós-condição:** Histórico visualizado com capacidade de navegação entre versões.

---

### CU-10: Regenerar Currículo para a Mesma Vaga

**Descrição:** O usuário solicita uma nova geração de IA para a mesma combinação de currículo base e vaga, arquivando a versão atual.

**Atores:** Usuário autenticado.
**Pré-condições:** Currículo gerado existente (CU-06).
**Fluxo principal:**
1. Usuário acessa detalhe do currículo gerado.
2. Usuário clica em "Regenerar".
3. Sistema exibe confirmação: "Gerar uma nova versão? A versão atual será arquivada."
4. Usuário confirma.
5. Sistema executa fluxo CU-06 com os mesmos dados, mas marca versão atual como `is_current = false` e define `parent_version_id`.
6. Nova versão criada com `version_number` incrementando.
**Pós-condição:** Nova geração criada como versão mais atual; versões anteriores preservadas.

---

## 6. jornadas de Usuário

### Jornada 1: Novo Usuário Cria Primeiro Currículo Otimizado

**Ator:** Carolina (Dev Júnior)
**Duração estimada:** 15-20 minutos

| Passo | Ação | Sistema responde |
|-------|------|-----------------|
| 1 | Usuário acessa o site pela primeira vez | Exibe tela de boas-vindas com CTA "Criar sua conta" |
| 2 | Preenche nome, email, senha, confirmação | Valida campos em tempo real |
| 3 | Clica em "Criar conta" | Cria conta, exibe sucesso, redireciona para login |
| 4 | Faz login com email e senha | Emite tokens, redireciona para dashboard vazio com CTA "Criar primeiro currículo" |
| 5 | Clica em "Criar currículo base" | Abre formulário de currículo (modo estruturado) |
| 6 | Preenche dados pessoais: nome, email, telefone, localização, LinkedIn, GitHub | Mantém estado do formulário |
| 7 | Adiciona 2 experiências profissionais | Permite adicionar/remover blocos de experiência |
| 8 | Adiciona formação acadêmica e skills | Lista editável de skills técnicos e comportamentais |
| 9 | Clica "Salvar currículo" | Gera markdown/JSONB, salva, define como padrão, redireciona para lista |
| 10 | Volta ao dashboard, clica "Nova análise" | Abre wizard de criação de vaga |
| 11 | Preenche empresa (TechCorp), título (Desenvolvedor Backend), cola descrição da vaga (>100 caracteres) | Valida tamanho da descrição |
| 12 | Seleciona currículo base (já tem default) | Confirma currículo selecionado |
| 13 | Clica "Gerar currículo" | Exibe loading spinner com mensagem "Gerando currículo otimizado..." |
| 14 | Sistema chama IA (timeout 30s), parseia resposta, salva | Exibe currículo gerado + card de análise de aderência (score, pontos fortes, lacunas) |
| 15 | Revisa currículo, clica "Baixar DOCX" | Gera arquivo sob demanda, browser dispara download |
| 16 | Opcional: identifica que quer mudar um parágrafo | Clica "Editar", modifica, salva — nova versão criada |
| **Resultado** | Currículo otimizado baixado, versão salva no histórico | Carolina pode repetir para cada nova vaga |

---

### Jornada 2: Usuário Retorna para Refinar Currículo Gerado

**Ator:** Rafael (Transição de Carreira)
**Duração estimada:** 5-10 minutos

| Passo | Ação | Sistema responde |
|-------|------|-----------------|
| 1 | Faz login com email e senha | Redireciona para dashboard |
| 2 | Vê na lista de histórico: "TechCorp — Desenvolvedor Backend — 2 semanas atrás" com score 58% | Dashboard exibe cards de currículos gerados ordenados por data |
| 3 | Clica no card do currículo | Abre tela de detalhe com currículo + análise de aderência |
| 4 | Revisa o currículo gerado | Identifica que quer contextualizar melhor a experiência de suporte como soft skills |
| 5 | Clica "Editar" | Abre editor com conteúdo markdown pré-preenchido |
| 6 | Adiciona uma linha sobre "comunicação técnica com stakeholders não técnicos" | Modifica o texto |
| 7 | Clica "Salvar" | Cria versão 2 (is_current=false na v1, is_current=true na v2), salva |
| 8 | Sistema exibe toast "Nova versão salva" | Atualiza tela para mostrar versão 2 |
| 9 | Refaz download do .docx | Gera arquivo atualizado com modificações |
| **Resultado** | Currículo refinado com posicionamento estratégico, versões preservadas | Rafael pode comparar v1 e v2 no histórico |

---

### Jornada 3: Usuário Gera Currículo para Nova Vaga com Baixa Aderência

**Ator:** Patrícia (Profissional Sênior)
**Duração estimada:** 10-15 minutos

| Passo | Ação | Sistema responde |
|-------|------|-----------------|
| 1 | Faz login | Redireciona para dashboard |
| 2 | Já tem currículo base "Currículo geral" (4 páginas, padrão) | Dashboard exibe currículo na lista |
| 3 | Clica "Nova análise" | Abre wizard de criação de vaga |
| 4 | Preenche: Empresa Y, Tech Lead, modelo híbrido SP, cola descrição da vaga completa | Valida descrição >100 caracteres |
| 5 | Seleciona currículo base "Currículo geral" | Confirma seleção |
| 6 | Clica "Gerar currículo" | Loading spinner |
| 7 | Sistema gera currículo + análise | Exibe: score de aderência 40% (baixo), lacunas: "Falta experiência com gestão de equipe de 10+ pessoas", "Não menciona orçamento de projeto" |
| 8 | Sistema exibelacunas claramente | Card de análise com ícone de alerta, lista de lacunas |
| 9 | Usuário revisa o currículo gerado | Nota que tem experiência de coordenação informal (3 pessoas) que pode ser contextualizada |
| 10 | Clica "Editar", adiciona parágrafo sobre coordenação técnica de equipe e budget management | Salva nova versão |
| 11 | Verifica score atualizado | Score sobe para 55% — continua baixo mas melhor |
| 12 | Baixa .docx | Arquivo gerado com conteúdo atualizado |
| **Resultado** | Currículo posicionado para vaga de Tech Lead, lacunas identificadas e parcialmente preenchidas | Patrícia entende o gap e pode decidir buscar capacitação ou ajustar expectativas |

---

## 7. Limites do MVP

O MVP é intencionalmente limitado para reduzir tempo de entrega e risco técnico. As funcionalidades listadas abaixo **NÃO** fazem parte do escopo da versão 1.

### Autenticação e Usuários
- **Sem multi-tenant**: um banco de dados por instância; sem isolamento entre "organizações" ou equipes. Cada conta pertence a um único usuário.
- **Sem convite de equipe**: não é possível compartilhar currículos ou vagas com outros usuários.

### Billing e Planos
- **Sem planos ou limites de uso**: não há quantidade máxima de gerações por mês.
- **Sem cobrança**: não há gateway de pagamento integrado.
- **Sem upgrade de plano**: não há funcionalidade de "plano gratuito vs. pago".

### Upload e Importação de Arquivos
- **Sem upload de PDF/DOCX**: o usuário digita ou cola texto; não faz upload de currículo existente em PDF ou Word.
- **Sem importação de LinkedIn**: não há scraping ou importação automática de perfil.
- **Sem worker Python**: todo o processamento é feito em Java/Spring Boot no MVP. Não há runtime Python adicional.

### Armazenamento de Arquivos
- **Sem storage externo**: todo conteúdo textual no PostgreSQL; arquivos .docx gerados sob demanda.
- **Sem armazenamento de .docx**: o arquivo nunca é salvo em disco ou bucket — sempre gerado no momento do download.

### Coleta de Vagas
- **Sem scraping de vagas**: o usuário cola a descrição da vaga manualmente; não há automação de coleta de vagas de sites como LinkedIn, Gupy, Catho, Vagas.com.br.
- **Sem integração com ATS**: não há integração com sistemas de recrutamento.

### Funcionalidades Não-Incluídas (Fases 2-4)
- Pesquisa salarial e fontes de dados salariais.
- Templates de currículo customizáveis.
- Few-shot examples no prompt de IA (otimização de prompt pós-Fase 1).
- Validação rigorosa de schema JSONB (JSON Schema validation).
- Colaboração em equipe.
- Observabilidade avançada (métricas, tracing, logs agregados).
- Upload de arquivos grandes (PDF, DOCX) com storage dedicado (S3/MinIO).
- Auditoria avançada de ações por organização.
- Multi-tenant com isolamento por organização.
- Cobrança via gateway de pagamento.

---

## 8. Critérios de Aceite do MVP

### Autenticação

- [ ] Registro com email único, senha hasheada BCrypt (cost >= 12) e JWT emitido ao login.
- [ ] Login com email/senha retorna tokens válidos; credenciais inválidas retornam erro genérico 401.
- [ ] Access token expira em 1h; refresh token em 7d; logout invalida sessão.
- [ ] Todas as rotas protegidas exigem token válido; acesso não autorizado retorna 401 ou 403.

### Currículo Base (RF-02)

- [ ] Usuário consegue criar currículo via formulário estruturado com todos os campos (dados pessoais, experiência, educação, skills, certificações).
- [ ] Usuário consegue criar currículo via modo texto livre (colagem).
- [ ] `content_markdown` e `content_jsonb` são gerados automaticamente ao salvar.
- [ ] Primeiro currículo do usuário é automaticamente definido como padrão.
- [ ] Usuário consegue editar e excluir currículos base.
- [ ] Lista de currículos retorna apenas os do usuário logado.

### Vagas de Emprego (RF-03)

- [ ] Usuário consegue criar vaga com descrição (mínimo 100 caracteres), empresa, título, regime, senioridade.
- [ ] Usuário consegue listar, editar, arquivar e excluir vagas.
- [ ] Lista de vagas retorna apenas as do usuário logado.

### Geração com IA (RF-04)

- [ ] Geração retorna currículo otimizado em markdown + análise de aderência (score, nível, pontos fortes, lacunas, mapa de keywords).
- [ ] Resposta bruta da IA (`raw_ai_response`) e prompt enviado (`prompt_used`) são salvos em `ai_runs`.
- [ ] Metadados (provider, model, tokens_used, cost_usd, duration_ms, status) são salvos em `ai_runs`.
- [ ] Timeout de 30s respeitado; retry 1x em caso de timeout; erro 502 retornado após falha dupla.
- [ ] Camada de abstração `AiProvider` permite trocar provedor via configuração sem mudança de código.
- [ ] IA não inventa experiências, certificações ou métricas não presentes no currículo base (regras do system prompt aplicadas).

### Edição e Versionamento (RF-05)

- [ ] Edição do currículo gerado cria nova versão (is_current=false na anterior, is_current=true na nova).
- [ ] Cadeia de versões mantida via `version_number` e `parent_version_id`.
- [ ] Histórico de versões acessível e navegável.
- [ ] Qualquer versão anterior pode ser visualizada.

### Exportação .docx (RF-06)

- [ ] Download .docx funciona para qualquer versão do currículo gerado.
- [ ] Arquivo segue template ATS-friendly: sem tabelas, sem imagens, sem colunas, fonte legível.
- [ ] Nome do arquivo no padrão `[Nome]-otimizado-[Empresa]-[Data].docx`.
- [ ] Geração concluída em menos de 3 segundos.

### Histórico (RF-07)

- [ ] Lista de currículos gerados retorna apenas os do usuário logado, ordenados por data.
- [ ] Filtros por empresa, título e data funcionam corretamente.
- [ ] Cada item exibe empresa, título, data, score de aderência, número de versões.

### Requisitos Não-Funcionais

- [ ] Tempo de resposta de API (CRUD) < 200ms para leitura, < 500ms para escrita (p95).
- [ ] Geração com IA completa (chamada + parse + save) < 30s (timeout configurável).
- [ ] Geração de .docx < 3s.
- [ ] BCrypt cost factor >= 12 aplicado em todos os hashes de senha.
- [ ] JWT com expiração de 1h (access) e 7d (refresh).
- [ ] Todas as comunicações via HTTPS.
- [ ] Validação de ownership em todos os endpoints — dados de outro usuário retornam 403.
- [ ] Rate limiting configurado (máximo 10 chamadas simultâneas por usuário no endpoint de geração).
- [ ] Código documentado com Javadoc; endpoints documentados com OpenAPI/Springdoc.
- [ ] Testes unitários cobrindo lógica de domínio (serviços, parsing, validação).
- [ ] Logs estruturados para operações críticas (autenticação, geração, erros).

### Integração e Qualidade

- [ ] Frontend (Next.js) comunica com backend (Spring Boot) via REST API sobre HTTPS.
- [ ] PostgreSQL como única fonte da verdade — todo conteúdo textual persistido no banco.
- [ ] .docx nunca salvo em disco — gerado sob demanda a partir do banco.
- [ ] Sistemacompila sem erros e testes unitários passam (覆盖率 mínima 70% nos módulos de serviço).
- [ ] Deploy reproduzível via Docker ou script automatizado.

---

*Documento gerado em 2026-06-11. Atualizar em caso de mudança de escopo approved pelo product owner.*