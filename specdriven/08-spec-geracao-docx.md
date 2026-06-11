# SPEC-08 — Geração de DOCX Sob Demanda

## 1. Princípio Central
**Conteúdo no banco. Arquivos gerados sob demanda.**

## 2. Estratégia de Geração
- Fluxo completo: request → busca content_markdown → parse Markdown → converte para DOCX → stream HTTP
- O arquivo NÃO é salvo em disco nem no banco
- Cada download gera um arquivo novo a partir dos dados mais recentes
- Timestamp docx_generated_at atualizado no banco

## 3. Biblioteca: Apache POI (XWPF)
Justificativa: biblioteca Java nativa, madura, sem dependência externa.
Alternativa rejeitada: docx4j (mais verbosa para o caso de uso).

## 4. Template ATS-Friendly — Estrutura Obrigatória
Seções nesta ordem exata:
1. Header de contato (nome 16pt bold + email|telefone|localização|LinkedIn 10pt)
2. Resumo profissional (3-4 linhas)
3. Experiência profissional (cargo bold, empresa|período, bullets de conquistas)
4. Formação acadêmica
5. Skills / Habilidades técnicas
6. Certificações (se aplicável)

## 5. Critérios ATS — O Que Evitar (elementos proibidos)
- Tabelas complexas
- Imagens, ícones, logos
- Layout em múltiplas colunas
- Text boxes flutuantes
- Fontes não-padrão (usar: Arial, Calibri, Times New Roman)
- Texto em imagem
- Keywords escondidas (white text)

## 6. Conversão Markdown → DOCX
Tabela de mapeamento:

| Markdown            | Elemento DOCX          |
|---------------------|------------------------|
| # Título            | Heading 1              |
| ## Seção            | Heading 2              |
| **negrito**         | XWPFRun setBold(true)  |
| *itálico*           | XWPFRun setItalic(true)|
| - item              | bullet list nativo     |
| [texto](url)        | apenas texto visível   |
| `código`            | monospace              |

## 7. Especificações do Documento
- Tamanho: A4
- Margens: 2.5cm em todos os lados
- Nome: Heading 1, 16pt, bold
- Contato: 10pt, mesma linha ou abaixo do nome
- Títulos de seção: Heading 2, 12pt, bold
- Corpo: 11pt, justificado
- Linguagem: pt-BR

## 8. Nome do Arquivo
Padrão: `[Nome]-otimizado-[Empresa]-[YYYY-MM-DD].docx`

Exemplo: `João-Silva-otimizado-Volkswagen-2026-06-11.docx`

## 9. Gatilhos para Migrar para Storage (Fase 4)
Métricas que disparam migração de geração sob demanda para cache em storage:

| Métrica                                     | Limiar         |
|---------------------------------------------|----------------|
| Downloads/dia                               | > 1.000        |
| Tempo médio de geração                       | > 5 segundos   |
| % de downloads repetidos                    | > 30%          |
| Requisito de auditoria de compliance         | = Sim          |

Quando dois ou mais limiares forem atingidos simultaneamente por mais de 72 horas, migrar para estratégia de cache em object storage (S3/MinIO).

## 10. Versionamento do Template
- O template é código Java versionado via git (não config file)
- Estratégia: manter versões da classe para backwards compatibility
- Cada versão do template = classe numerada (ex: `DocxTemplateV1`, `DocxTemplateV2`)
- Reverter = git revert da classe correspondente
- Tests obrigatórios para o método de geração (`DocxGeneratorServiceTest`)
