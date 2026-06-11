# Documentação Técnica — MVP de Currículo Otimizado por IA

## Visão Geral do Sistema

Este repositório contém a documentação técnica de arquitetura e produto para um sistema SaaS de geração e organização de currículos otimizados por vaga, utilizando inteligência artificial generativa.

O sistema permite que o usuário cadastre um currículo base uma única vez e gere, a partir dele, múltiplas versões otimizadas para diferentes vagas de emprego — cada uma com análise de aderência, mapa de palavras-chave e exportação em formato Word (.docx).

---

## Ordem de Leitura Recomendada

A documentação é organizada em 12 arquivos numerados. A ordem de leitura recomendada é:

| # | Arquivo | Descrição |
|---|---------|-----------|
| — | `README.md` | Este arquivo. Navegação e escopo. |
| 1 | `00-visao-geral.md` | Problema, público-alvo, princípio central de design. |
| 2 | `01-contexto-produto.md` | Personas, jornadas, requisitos funcionais e não-funcionais. |
| 3 | `02-arquitetura-mvp.md` | Arquitetura de sistema, decisões de stack, diagrama. |
| 4 | `03-modelo-dados.md` | Modelo de dados, tabelas, tipos, índices, estratégia TEXT+JSONB. |
| 5 | `04-fluxos-sistema.md` | Fluxos principais do sistema em formato passo a passo. |
| 6 | `05-regras-ia.md` | Regras obrigatórias para o agente de IA, abstração de provedores. |
| 7 | `06-api-backend.md` | Endpoints REST, contratos de request/response. |
| 8 | `07-frontend.md` | Telas, componentes, decisões de estado e UX. |
| 9 | `08-geracao-docx.md` | Estratégia de geração de .docx sob demanda, critérios ATS. |
| 10 | `09-historico-versoes.md` | Estratégia de versionamento, audit trail, comparação. |
| 11 | `10-fases-evolucao.md` | Roadmap de funcionalidades por fase. |
| 12 | `11-riscos-decisoes.md` | Riscos técnicos, decisões pendentes, critérios para mudança. |

---

## Escopo do MVP

### O que está incluído

- Autenticação (registro, login, logout com JWT)
- Cadastro e gerenciamento de currículo base
- Cadastro de vaga de emprego (descrição completa colada pelo usuário)
- Geração de currículo otimizado via IA com análise de aderência
- Revisão e edição manual do currículo gerado
- Versionamento de currículos gerados por vaga
- Histórico de análises por empresa, título e data
- Exportação em formato Word (.docx) sob demanda
- Abstração de provedores de IA (provedor configurável via código/ambiente)
- API REST completa (Spring Boot)
- Frontend Next.js completo

### O que NÃO está incluído

- Upload de arquivos (PDF, DOCX, imagem)
- Extração de texto de PDF
- Pesquisa salarial
- Scraping de sites de emprego
- Multi-tenant (um usuário por conta no MVP)
- Planos e cobrança
- Storage em bucket (S3, MinIO, Supabase Storage)
- Persistência de arquivos binários no banco
- Worker Python (todo o processamento em Java/Spring Boot)
- Geração de PDF
- Integração com ATS externos
- Suporte a múltiplos idiomas
- Colaboração em equipe

---

## Princípio Central de Design

> **Conteúdo no banco. Arquivos gerados sob demanda.**

O PostgreSQL é a única fonte da verdade. O conteúdo textual de todos os currículos, vagas e análises vive exclusivamente no banco. O arquivo .docx é gerado no momento do download a partir do conteúdo salvo — nunca é armazenado como binário.

Este princípio simplifica significativamente o MVP: backup trivial (um dump SQL), zero custo de storage, zero problema de stale files, zero complexidade de sincronização entre banco e storage.

---

## Stack do MVP

| Camada | Tecnologia | Justificativa |
|--------|------------|---------------|
| Frontend | Next.js (React) | SSR/SSG, ecossistema React maduro, deploy simples |
| Backend | Java 17+ / Spring Boot 3 | Type safety, maturidade enterprise, DI robusto |
| Banco | PostgreSQL 15+ | JSONB, full-text search, confiável, madura |
| IA | Abstração configurável | Gemini, OpenRouter, OpenAI, Claude — troca sem mudança de código |
| Geração DOCX | Apache POI (XWPF) | Biblioteca Java nativa, madura, sem dependência externa |
| Auth | JWT (access + refresh) | Stateless, simples, padrão industry |

---

## Como Usar Esta Documentação

Esta documentação é a **entrada para o processo de especificação dirigida por spec (spec-driven development)**.

Fluxo de uso:

1. **Leitura completa** → alinhamento entre stakeholders e equipe técnica
2. **Revisão de decisões** → cada decisão marcada como `[DECISÃO]` deve ser validada
3. **Geração de specs** → cada arquivo vira base para uma spec de implementação dedicada
4. **Prototipagem** → especificações de tela viram wireframes/low-fi
5. **Implementação** → specs detalhadas guiam a implementação
6. **Revisão de segurança** → após implementação, revisar auth, criptografia e acesso a dados

---

## Convenções Usadas nesta Documentação

- `[DECISÃO]` — decisão de design já tomada e validada. Implementar conforme documentado.
- `[QUESTÃO ABERTA]` — decisão pendente. Requer alinhamento antes de implementar.
- `[FUTURO]` — funcionalidade fora do escopo do MVP, documentada para referência.
- `[RISCO]` — ponto que requer atenção durante implementação ou operação.

---

## Terminologia

| Termo | Significado |
|-------|-------------|
| **Currículo base** | O currículo original do usuário, cadastrado uma vez. |
| **Vaga** | O registro da oportunidade (empresa + título + descrição). |
| **Currículo gerado** | A versão otimizada produced pela IA para uma vaga específica. |
| **Análise de aderência** | O diagnóstico da IA sobre compatibilidade entre currículo e vaga. |
| **AI run** | O log de uma chamada individual ao provedor de IA. |
| **Versionamento** | Registro de múltiplas versões de um currículo gerado para a mesma vaga. |