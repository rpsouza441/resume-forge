# Phase 1: DOCX Fix — Specification

**Created:** 2026-06-15
**Updated:** 2026-06-15
**Ambiguity score:** 0.15 (gate: ≤ 0.20)
**Requirements:** 8 locked

## Goal

Corrigir o `StructuredDocxConverter` para gerar DOCX visualmente profissional: header com nome/contatos, fontes hierárquicas corretas (10-11pt corpo), bullets OOXML reais, margens densas, sem Markdown residual, máximo 2 páginas.

## Background

O `StructuredDocxConverter` foi implementado mas o DOCX gerado está visualmente incorreto conforme auditoria detalhada:

| Problema | Severidade | Detalhamento |
|----------|------------|--------------|
| Fontes 21pt corpo vs 10-11pt | 🔴 Crítico | run.setFontSize(21) = 21pt, não 10.5pt |
| Margens 2.5cm vs 1.6-1.8cm | 🔴 Crítico | setDocumentMargins() vazio |
| Header ausente | 🔴 Crítico | Nome, título, contatos não aparecem |
| Markdown residual `**` | 🔴 Crítico | Asteriscos visíveis no documento |
| Sem bullets reais | 🟡 Médio | Parágrafos comuns, não OOXML numPr |
| Sem estilos nomeados | 🟡 Médio | Tudo estilo "Normal" |
| 3 páginas | 🟡 Médio | Excesso de fonte + margens |

O documento atual contém Resumo, Habilidades, Experiências, Formação e Certificações, mas:
- Começa direto na seção "Resumo Profissional" sem header pessoal
- Experiências em parágrafo único: "Cargo | Empresa Período"
- Habilidades com Markdown: "**Infraestrutura:** Windows Server..."
- 26 parágrafos, nenhuma tabela, nenhuma lista real
- Margens ~2.5cm em todos os lados

## Requirements

1. **Header estruturado**: Nome, título e contatos devem aparecer no início do documento.
   - Current: Documento começa em "Resumo Profissional" — header ausente
   - Target: Início do documento: NOME (16-18pt bold) → Título (10.5-11.5pt) → Contatos (9-10pt) → Separador visual
   - Acceptance: DOCX contém parágrafos com nome, título, contatos antes do Resumo. Separador visual (border line) presente.

2. **Fontes hierárquicas corretas**: Corpo 10-11pt, títulos de seção maiores.
   - Current: run.setFontSize(21) define 21pt (invertido — corpo maior que títulos)
   - Target: Corpo = 10-11pt, Seções = 11.5-13pt, Nome = 16-18pt. Half-points corretos no Apache POI.
   - Acceptance: Inspect DOCX internamente — corpo ≤ 11pt, títulos > corpo. Teste estrutural verifica hierarchy.

3. **Margens densas**: 1.5-1.8cm em todos os lados.
   - Current: ~2.5cm (setDocumentMargins() cria sectPr vazio sem CTPageMar)
   - Target: Superior/Inferior: 1.5-1.8cm (~850-1020 twips), Esquerda/Direita: 1.6-1.9cm (~907-1076 twips)
   - Acceptance: CTPageMar com valores corretos. DOCX fits em 2 páginas.

4. **Bullets reais OOXML**: Listas com numeração nativa Word.
   - Current: Texto com "• " no início do parágrafo — não é bullet real
   - Target: Cada highlight como XWPFParagraph com numPr configurado via numbering definition
   - Acceptance: DOCX internamente contém numPr em paragraphs de highlights. Teste valida bulletNumId presente.

5. **Sem Markdown residual**: Campos estruturados sem `**`, `###`, `#`.
   - Current: "**Infraestrutura:**", "**Bacharelado em Sistemas de Informação**" visíveis
   - Target: sanitizeMarkdown remove todos os marcadores Markdown dos campos antes de criar runs
   - Acceptance: DOCX não contém literais `**`, `###`, `#`. Teste valida ausência de Markdown.

6. **Estilos nomeados**: Hierarquia visual via estilos Word.
   - Current: Todos parágrafos estilo "Normal" — sem diferenciação
   - Target: Criar estilos XWPFStyle com nomes: ResumeName, ResumeSectionHeading, ResumeBullet, etc.
   - Acceptance: DOCX contém estilos nomeados (verificável via XML). Hierarquia visual consistente.

7. **Experiências organizadas**: Cargo|Empresa → Período → Bullets.
   - Current: "Cargo | Empresa Período" em único parágrafo, sem separação
   - Target: "Empresa | Local" + "Cargo | Período" em parágrafos separados, bulleted highlights abaixo
   - Acceptance: DOCX mostra estrutura: Header → Bullets. keepWithNext no header evita quebra.

8. **Densidade 2 páginas**: Curriculums curtos em máximo 2 páginas.
   - Current: ~3 páginas com pouco conteúdo
   - Target: Fontes compactas (10-11pt) + margens densas + espaçamento mínimo = 1-2 páginas
   - Acceptance: Documento de referência gera 2 páginas. QA visual confirma densidade.

## Boundaries

**In scope:**
- StructuredDocxConverter.java — correções de formatação
- DocxGenerationService.java — passagem de dados de header
- Testes estruturais — validação de formatação
- Sanitização de campos estruturados

**Out of scope:**
- Alterar system prompt da IA — isso é fase separada
- Copiar conteúdo do bom-exemplo.docx — apenas referência visual
- Criar novos dados de currículo — usar dados existentes
- Frontend de exibição — problema separado
- Geração de markdown — já existe
- Alterar schema JSON da resposta IA

## Constraints

- Apache POI 5.3.0 (verificado em pom.xml)
- Half-points: setFontSize(21) = 10.5pt, setFontSize(22) = 11pt
- Não usar tabelas complexas (ATS pode ignorar)
- Não usar múltiplas colunas
- Não usar fonte < 9.5pt
- Header dentro do body (não header nativo Word — ATS pode ignorar)
- Twips para margens: 1cm ≈ 567 twips

## Acceptance Criteria

- [ ] Header visível: Nome + Título + Contatos + Separador antes do Resumo
- [ ] Corpo do DOCX usa fonte ≤ 11pt
- [ ] Títulos de seção maiores que o corpo (hierarquia visual)
- [ ] Margens: Superior/Inferior 1.5-1.8cm, Esquerda/Direita 1.6-1.9cm
- [ ] Bullets são numeração OOXML real (numPr presente no XML)
- [ ] DOCX não contém literais Markdown: `**`, `###`, `#`
- [ ] Experiências organizadas: Header → Bullets (não parágrafo único)
- [ ] Documento fit em 2 páginas para currículo de referência
- [ ] Estilos nomeados presentes (ResumeName, ResumeSectionHeading, etc.)
- [ ] Não há fallback para MarkdownToDocxConverter
- [ ] Renderiza corretamente no Word e LibreOffice
- [ ] QA visual aprovado por usuário

## Ambiguity Report

| Dimension          | Score | Min  | Status | Notes                              |
|--------------------|-------|------|--------|------------------------------------|
| Goal Clarity       | 0.90  | 0.75 | ✓      | 8 requisitos específicos           |
| Boundary Clarity   | 0.85  | 0.70 | ✓      | Out of scope claro                |
| Constraint Clarity | 0.90  | 0.65 | ✓      | Half-points, twips, libs definidos|
| Acceptance Criteria| 0.85  | 0.70 | ✓      | 12 checkboxes pass/fail           |
| **Ambiguity**      | 0.15  | ≤0.20| ✓      |                                   |

## Interview Log

| Round | Perspective     | Question summary              | Decision locked                    |
|-------|----------------|-----------------------------|-----------------------------------|
| 1     | Auditoria      | O que foi identificado?      | 8 problemas críticos documentados |
| 2     | Simplifier     | Qual o mínimo viável?        | Header + Fontes + Bullets + 2pg  |
| 3     | Boundary Keeper| O que NÃO é escopo?         | System prompt, frontend, AI schema |
| 4     | Failure Analyst| O que quebra se não corrigir?| Currículo não usável profissional |

---

*Phase: 01-docx-fix*
*Spec created: 2026-06-15*
*Next step: /gsd-discuss-phase 1 — implementation decisions (how to build what's specified above)*
