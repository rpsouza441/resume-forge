---
name: SPEC
description: SPEC for DOCX Structured Renderer Fix
phase: 01-docx-fix
status: draft
---

# SPEC: DOCX Structured Renderer Fix

**Phase:** 01-docx-fix  
**Date:** 2026-06-15  
**Status:** Draft

---

## Problema Atual

O `StructuredDocxConverter` foi implementado mas o DOCX gerado está visualmente incorreto:

| Problema | Detalhamento |
|----------|--------------|
| Fontes 20-21pt | Corpo maior que títulos (invertido) |
| Header ausente | Nome, título, contatos não aparecem |
| Markdown residual | `**`, `###` visíveis no documento |
| Sem bullets reais | Simulado com `-` e `•` em texto |
| 3 páginas | Para pouco conteúdo (densidade ruim) |
| Normal style | Todos os parágrafos sem estilo |
| Experiências ruins | Cargo\|Empresa\|Período em parágrafo único |

---

## Auditoria Exigida

### Etapa 1: Confirmar Qual Conversor Foi Usado

Adicionar diagnóstico temporário para confirmar:
- Se `StructuredDocxConverter` foi selecionado
- Se houve fallback para `MarkdownToDocxConverter`
- Versão do renderer
- Presença das seções estruturadas
- Quantidade de experiências/skills/projetos/treinamentos
- Presença do header
- Caminho lógico utilizado

**Log de diagnóstico:**
```text
DOCX_RENDERER=STRUCTURED
schemaVersion=2
hasHeader=true
experiences=3
skillGroups=5
projects=2
trainings=4
markdownFallback=false
```

### Etapa 2: Auditar StructuredDocxConverter

Revisar:
- Constantes de fonte
- Margens
- Estilos
- Espaçamento
- Alinhamento
-Indentação
- Criação de bullets
- Criação do cabeçalho
- Criação de títulos
- Tratamento de Markdown residual
- Controle de páginas
- Lógica de empresa/cargo/período
- Tratamento de campos ausentes

### Etapa 3: Verificar Apache POI Half-Points

**Hipótese prioritária:** Valores como `20` e `21` usados como se fossem half-points.

No Apache POI:
- `run.setFontSize(21)` = 21 pt (NÃO 10.5 pt)
- Para 10.5 pt: `run.setFontSize(21)` (21 half-points)
- Para 11 pt: `run.setFontSize(22)` (22 half-points)

Verificar qual API está sendo usada no projeto.

---

## Solução: Estilos Nomeados

Definir estilos no documento:

| Estilo | Tamanho | Negrito | Propósito |
|--------|---------|---------|-----------|
| `ResumeName` | 16-18 pt | Sim | Nome do candidato |
| `ResumeProfessionalTitle` | 10.5-11.5 pt | Não | Título profissional |
| `ResumeContact` | 9-10 pt | Não | Email, LinkedIn, etc |
| `ResumeSectionHeading` | 11.5-13 pt | Sim | Títulos de seção |
| `ResumeSummary` | 10-11 pt | Não | Resumo profissional |
| `ResumeJobHeader` | 10-11 pt | Sim | Cargo e empresa |
| `ResumeJobPeriod` | 9-10 pt | Não | Período (itálico) |
| `ResumeBullet` | 10-11 pt | Não | Highlights |
| `ResumeSkill` | 9.5-10.5 pt | Não | Skills por categoria |
| `ResumeCompactItem` | 9.5-10.5 pt | Não | Projetos, formação |

---

## Solução: Header Estruturado

**Local:** Início do documento (não header nativo Word - ATS pode ignorar)

**Estrutura visual:**
```
NOME COMPLETO
Título profissional direcionado
Cidade/Estado | email | LinkedIn | GitHub | site
────────────────────────────────────────────
```

**Regras:**
- Nome em destaque (16-18 pt)
- Título em linha própria
- Contatos em tamanho menor (9-10 pt)
- Omitir contatos ausentes
- Separador visual no final

---

## Solução: Experiências Corretas

**Estrutura correta:**
```
Empresa | Localidade
Cargo | Período
• Highlight 1
• Highlight 2
• Highlight 3
```

**Regras:**
- Empresa e/ou cargo em negrito
- Período visualmente secundário
- Cada highlight como bullet real OOXML
- `keepWithNext` no cabeçalho da experiência
- Indentação compacta

---

## Solução: Bullets Reais OOXML

Usar numeração OOXML nativa:
```java
NumberingDefinition numDef = document.createNumberingDefinition(...);
XWAMRParaRpr pr = new XWAMRParaRpr();
pr.setNumId(new BigInteger(numId));
```

Não simular com `-`, `•` ou caracteres especiais.

---

## Solução: Margens e Densidade

| Margem | Valor |
|--------|-------|
| Superior | 1.5-1.8 cm |
| Inferior | 1.5-1.8 cm |
| Esquerda | 1.6-1.9 cm |
| Direita | 1.6-1.9 cm |

**Espaçamento:**
- Linha simples ou 1.05
- `spaceAfter`: 2-4 pt (corpo)
- `spaceBefore`: 5-8 pt (seções)
- Sem linhas vazias artificiais

---

## Solução: Sanitização de Markdown

**Campos estruturados devem conter texto puro.**

Validação pós-resposta:
- Se campos contiverem `**`, `###`, `{HEADER}` → sanitizar
- Não aplicar regex destrutiva sobre currículo inteiro
- Remover apenas marcadores Markdown válidos

**Exceção:** Asteriscos que façam parte de dados técnicos (ex: `C++`, `Bash*`)

---

## Critérios de Aceite

- [ ] Nome, título, contatos visíveis no header
- [ ] Sem Markdown literal (`**`, `###`)
- [ ] Corpo ≤ 11 pt
- [ ] Títulos de seção > corpo
- [ ] Bullets reais OOXML
- [ ] Hierarquia visual consistente
- [ ] ≤ 2 páginas para currículo padrão
- [ ] Sem fallback Markdown
- [ ] Renderiza corretamente no Word e LibreOffice
- [ ] Visualmente comparável ao bom-exemplo.docx
- [ ] Passa nos testes estruturais
- [ ] QA visual aprovado

---

## Restrições

- ❌ Não alterar somente o system prompt
- ❌ Não declarar sucesso apenas por compilação
- ❌ Não copiar conteúdo do bom-exemplo.docx
- ❌ Não remover informações para esconder paginação
- ❌ Não usar fonte < 9.5 pt
- ❌ Não usar tabelas complexas ou múltiplas colunas
- ❌ Não usar Markdown como entrada do renderer
- ❌ Não fazer commit
- ❌ Não registrar PII nos logs

---

## Arquivos a Modificar

1. `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`
2. `backend/src/main/java/com/resumeforge/export/service/DocxGenerationService.java`
3. `backend/src/test/java/com/resumeforge/export/` (testes estruturais)
4. `docs/resume-generation-v2/README.md` (atualizar status)

---

## Referências

- `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`
- `docs/resume-generation-v2/`
- `bom-exemplo.docx` (referência visual)
- Apache POI documentation (half-points)
