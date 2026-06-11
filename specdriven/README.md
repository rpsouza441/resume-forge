# Spec-Driven Development — Resume Forge

## O que é esta pasta

Esta pasta contém as especificações formais de implementação derivadas da documentação de design em `docs/`. Cada arquivo corresponde a um domínio do sistema.

## Arquivos

| Arquivo | Domínio | Origem |
|---------|---------|--------|
| 01-spec-contexto-produto.md | Personas, requisitos, critérios de aceite | 01-contexto-produto.md |
| 02-spec-arquitetura.md | Stack, módulos, decisões arquiteturais | 02-arquitetura-mvp.md |
| 03-spec-modelo-dados.md | Schema SQL, entidades, índices | 03-modelo-dados.md |
| 04-spec-fluxos-sistema.md | 10 fluxos de usuário com passos | 04-fluxos-sistema.md |
| 05-spec-regras-ia.md | System prompt, interface AiProvider, validação | 05-regras-ia.md |
| 06-spec-api-backend.md | Contratos REST, DTOs, erros | 06-api-backend.md |
| 07-spec-frontend.md | Telas, componentes, estado, rotas | 07-frontend.md |
| 08-spec-geracao-docx.md | Template, conversão, ATS, biblioteca POI | 08-geracao-docx.md |
| 09-spec-historico-versoes.md | Cadeia de versões, queries, rollback | 09-historico-versoes.md |

## Convenções

- `[DECISÃO]` — decisão tomada e validada, implementar conforme documentado
- `[QUESTÃO ABERTA]` — pendente, requer alinhamento antes de implementar
- `[FUTURO]` — feature planejada para fase futura, não no MVP
- `[RISCO]` — ponto que requer atenção durante implementação

## Fluxo de Uso

1. **Implementar** — seguir a spec do domínio correspondente
2. **Revisar** — verificar se a implementação respeita as decisões documentadas
3. **Alterar** — se uma decisão mudar, atualizar primeiro a spec, depois o código
4. **Testar** — critérios de aceite da spec 01 são o checklist final

## Ordem Recomendada de Implementação

Use esta ordem ao pedir implementação para outro agente:

1. `02-spec-arquitetura.md` — criar estrutura base do monorepo, backend, frontend e configuração.
2. `03-spec-modelo-dados.md` — criar schema PostgreSQL/Flyway e entidades principais.
3. `06-spec-api-backend.md` — implementar auth, CRUDs, geração síncrona, versionamento e DOCX.
4. `05-spec-regras-ia.md` — implementar `AiProvider`, prompts, validação e logs em `ai_runs`.
5. `08-spec-geracao-docx.md` — implementar conversão Markdown → DOCX via Apache POI.
6. `07-spec-frontend.md` — implementar telas e integrações com API.
7. `04-spec-fluxos-sistema.md` — validar fluxos ponta a ponta.
8. `09-spec-historico-versoes.md` — revisar consultas de histórico, versões e auditoria.
9. `01-spec-contexto-produto.md` — usar critérios de aceite como checklist final do MVP.

Não implementar upload de arquivos, fila, worker Python, storage externo ou persistência de DOCX no MVP.

## Princípio Central

> **Código implementa a spec. Spec documenta a decisão. Decisão documenta o porquê.**

Use markdown puro para todos os arquivos.
