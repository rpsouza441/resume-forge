# Visão Geral do Sistema

## O Problema que o Sistema Resolve

Candidatos a vagas de emprego enfrentam um problema recorrente e custoso em tempo: adaptar o currículo para cada vaga é um processo manual, lento e sem metodologia. O candidato precisa:

1. Reler a descrição da vaga.
2. Identificar palavras-chave e requisitos.
3. Reescrever ou reorganizar o currículo para maximizar aderência.
4. Repete isso para cada candidatura — frequentemente 10, 20 ou 50 vezes.

O resultado é ou um currículo genérico enviado a todas as vagas (baixa conversão) ou horas desperdiçadas em customização manual (baixa produtividade). Além disso, não há registro sistemático de qual versão foi enviada para qual vaga, nem análise de qual abordagem funcionou melhor.

Este sistema resolve o problema ao automatizar a otimização do currículo via IA generativa, mantendo um histórico completo de versões e permitindo exportação profissional em formato Word.

---

## Público-Alvo

### Persona A — Desenvolvedor Júnior em Busca do Primeiro Emprego

- **Perfil**: graduando ou recém-formado, 1-2 anos de experiência informal (projetos pessoais, freelance).
- **Necessidade principal**: preencher lacunas de experiência com projetos e cursos, destacar tecnologias que conhece, não parecer "junior demais".
- **Dor**: não sabe o que incluir ou omitir; tem experiência mas não sabe como apresentá-la de forma atraente.
- **Como o sistema ajuda**: IA identifica keywords da vaga e reorganiza/projects a experiência existente de forma mais direcionada.

### Persona B — Profissional de Carreira Média em Transição

- **Perfil**: 3-7 anos de experiência, mudando de área ou papel (ex.: suporte → desenvolvimento, marketing → produto).
- **Necessidade principal**: reformular completamente a narrativa do currículo para um novo contexto, sem mentir sobre experiência.
- **Dor**: a experiência passada parece desalinhada com a vaga nova; não sabe como conectar os pontos.
- **Como o sistema ajuda**: IA propõe estratégia de posicionamento, destacando competências transferíveis e reinterpretando experiência existente.

### Persona C — Profissional Sênior com Ampla Experiência

- **Perfil**: 8+ anos, currículo longo, múltiplos papéis e tecnologias.
- **Necessidade principal**: selecionar e curar conteúdo relevante para cada vaga, sem omitir o que importa mas sem exceder 2 páginas.
- **Dor**: currículo tem 4 páginas; não sabe o que cortar; manda o mesmo currículo para tudo porque customizar leva tempo.
- **Como o sistema ajuda**: IA filtra e prioriza conteúdo mais relevante para cada vaga, gerando versões curtas e focadas.

---

## Fluxo Geral do Sistema

```
[Criar conta / Login]
        ↓
[Cadastrar currículo base] ───────────────────────────────────┐
        ↓                                                  (pode pular se usar currículo da vaga)
[Criar análise de vaga]                                        │
        ↓                                                      │
[Colar descrição da vaga]                                      │
        ↓                                                      │
[Selecionar currículo base]                                     │
        ↓                                                      │
[Backend: montar prompt + chamar IA]                            │
        ↓                                                      │
[IA retorna: análise de aderência + currículo otimizado]       │
        ↓                                                      │
[Backend: salvar no PostgreSQL]                                │
        ↓                                                      │
[Frontend: exibir currículo + análise]                          │
        ↓                                                      │
[Usuário edita manualmente (opcional)]                         │
        ↓                                                      │
[Salvar nova versão] ◄────────────────────────────────────────┘
        ↓
[Baixar .docx sob demanda]
```

---

## Artefatos Gerados

| Artefato | Tipo | Onde vive | Quem cria |
|----------|------|-----------|-----------|
| Currículo base | texto, markdown, JSONB | PostgreSQL | Usuário (via formulário ou colagem) |
| Vaga de emprego | texto | PostgreSQL | Usuário (colagem de descrição) |
| Currículo gerado | texto, markdown, JSONB | PostgreSQL | IA + usuário (edição) |
| Análise de aderência | texto + JSONB | PostgreSQL | IA |
| Log de execução da IA | texto + números | PostgreSQL | Backend |
| Arquivo .docx | binário | **Gerado sob demanda** | Backend (Apache POI) |

---

## O Papel do Banco de Dados

O PostgreSQL é a **única fonte da verdade** para todo conteúdo textual do sistema. Isso inclui:

- Dados do usuário (autenticação).
- Currículo base em texto, markdown e JSONB.
- Descrição completa da vaga.
- Currículo gerado em texto, markdown e JSONB.
- Análise de aderência (texto e estrutura).
- Logs de execução da IA (prompt, resposta bruta, tokens, custo, duração).
- Histórico de versões.
- Status de processamento.

O banco **não** armazena arquivos binários (como .docx ou PDF) no MVP.

---

## O Papel do Arquivo Word (.docx)

O arquivo Word é o **entregável final** do sistema — o artefato que o candidato baixa e envia para a vaga. Ele é gerado sob demanda, no momento do download, a partir do conteúdo markdown/texto salvo no banco.

Por que não salvar o .docx:
- O conteúdo já existe no banco em formato editável.
- Se o usuário editar o currículo, o .docx anterior ficaria obsoleto.
- Adicionar storage (S3, MinIO, Supabase) no MVP é complexidade desnecessária.
- O banco já é a fonte da verdade — gerar sob demanda garante que o arquivo sempre reflete o conteúdo atual.

---

## O Que Diferencia Este Sistema de um Prompt Manual em IA

| Aspecto | Prompt manual (ChatGPT, Gemini) | Este sistema |
|---------|--------------------------------|-------------|
| **Prompt engineering** | Ad hoc, varia por tentativa | Sistemático, fixo, otimizado para currículos |
| **Saída estruturada** | Texto livre, variável | Texto + Markdown + JSONB, formato consistente |
| **Versionamento** | Nenhum | Histórico completo por vaga e data |
| **Revisão e edição** | Copiar/colar manualmente | Interface de edição com salvamento de versão |
| **Análise de aderência** | O usuário precisa inferir | Fornecida explicitamente pela IA |
| **Mapa de palavras-chave** | O usuário precisa deduzir | Fornecido explicitamente pela IA |
| **Organização** | Planilhas, anotações mentais | Histórico estruturado no banco |
| **Exportação** | Copiar/colar em template Word | Download .docx ATS-friendly imediato |

O sistema não elimina a IA — ele a **orquestra de forma sistemática**, com prompts estruturados, validações, armazenamento e workflow completo.

---

## Comparação de Estratégias de Persistência

| Aspecto | PostgreSQL | Pasta local (fs) | Bucket (S3/MinIO) |
|---------|-----------|------------------|-------------------|
| Complexidade de setup | Baixa (uma variável DATABASE_URL) | Baixa | Alta (credenciais, SDK, policies) |
| Custo | Custo do banco (já existe) | Zero | Custo de storage + egress (real em produção) |
| Backup | Rotinas nativas de backup do PostgreSQL | Manual (scripts) | Delegado ao provedor, mas precisa de restore procedure |
| Versionamento | Via tabela (is_current, parent_version_id) | Nomenclatura de arquivo (frágil) | Versões de objeto (complexidade adicional) |
| Consistência transacional | ACID garantido pelo PostgreSQL | Nenhuma | Eventual consistency (pode gerar inconsistências) |
| Controle de acesso | Permissões PostgreSQL (RLS futura) | Permissões de arquivo (perigoso) | IAM policies |
| Restore | `pg_restore` único | Scripts manuais por arquivo | Diferente por provider |
| Adequação ao MVP | **Perfeita** | Frágil (um rm -rf e perdemos tudo) | Excesso (MVP não tem volume que justifique) |

**[DECISÃO] Recomendação: PostgreSQL como fonte da verdade.**

No MVP, o volume de dados é baixo (centenas de usuários, milhares de currículos), e o banco já é a dependency obrigatória para dados relacionais. Adicionar storage de arquivos traz complexidade de configuração, custo monetário e problemas de sincronização sem nenhum benefício tangível neste estágio.

A decisão pode mudar na Fase 4, quando o volume de uploads (PDF, DOCX) justificar o custo e a complexidade de um storage dedicado.

---

## Comparação de Estratégias de Geração de .docx

| Estratégia | Stale data | Backup | Custo | Complexidade |
|------------|-----------|--------|-------|--------------|
| Gerar .docx e não salvar arquivo | **Nunca ocorre** | Simples | Zero | Baixa |
| Gerar .docx e salvar binário no banco | Possível (conteúdo editável depois) | Dump SQL maior | Armazenamento DB | Baixa |
| Gerar .docx e salvar em storage externo | Possível (re-geração necessária) | Externo ao DB | Custo mensal (storage + egress) | Média |

**[DECISÃO] Recomendação: gerar sob demanda e não salvar o arquivo.**

A coluna "Stale data" é o fator decisivo. Currículos otimizados podem ser editados após a geração. Se o arquivo .docx for persistido em qualquer lugar (banco ou storage), qualquer edição subsequente exigirá um mecanismo de re-geração — ou o arquivo no disco ficará desatualizado em relação ao banco. A estratégia sob demanda elimina esse problema pela raiz:

- **Zero storage overhead**: o banco armazena texto, não binários.
- **Arquivo sempre atual**: o .docx baixado reflete o conteúdo mais recente salvo.
- **Backup trivial**: um único dump SQL contém todo o conteúdo.
- **Sem sincronização**: não existe o cenário de "o arquivo no disco não corresponde ao registro no banco".

A única desvantagem é que cada download gera o arquivo novamente (1–3 segundos de CPU). Isso é aceitável no MVP — o volume esperado é baixo e o custo de CPU é insignificante comparado à complexidade de manter arquivos sincronizados.

A decisão de migrar para storage persistente faz sentido quando: volume de downloads > 1.000/dia, tempo médio de geração > 5 segundos, ou necessidade de auditoria do arquivo exato enviado pelo candidato.

---

## Princípio Central de Design

> **Conteúdo no banco. Arquivos gerados sob demanda.**

Este princípio é a espinha dorsal de todas as decisões de arquitetura do MVP:

- PostgreSQL como fonte da verdade → backup trivial, consistência transacional, zero storage overhead.
- .docx gerado sob demanda → arquivo sempre atual, sem sincronização, sem stale files.
- IA como processamento stateless → cada geração é uma nova chamada, sem estado acumulado.
- Histórico por versão → cada edição gera uma nova linha, nunca sobrescreve.

---

## Resumo das Principais Decisões de Design

| Decisão | Escolha | Alternativa descartada |
|---------|---------|----------------------|
| Fonte da verdade | PostgreSQL | Pasta local, bucket S3 |
| Armazenamento de currículo | TEXT (markdown) + JSONB | TEXT puro, JSONB puro |
| Geração de .docx | Sob demanda (POI) | Salvar binário no banco |
| Storage de arquivos | Nenhum no MVP | S3, MinIO, Supabase Storage |
| Abstração de IA | Strategy pattern (interface) | Acoplado a um provedor |
| Backend | Java/Spring Boot | Node.js, Python, Go |
| Frontend | React/Next.js | Vite, Remix, Angular |
| Multi-tenant | Não no MVP | Multi-tenant desde o início |
| Upload de arquivos | Não no MVP | Upload PDF/DOCX |
| Worker Python | Não no MVP | Worker Python para parsing |