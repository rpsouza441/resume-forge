# 08 — Geracao de DOCX

## 1. Estrategia: Geracao Sob Demanda no Momento do Download

### Principio Central

O banco de dados e a unica fonte da verdade para o conteudo do curriculo. O arquivo DOCX nao e persistido em disco nem no banco — ele e gerado sob demanda no instante em que o usuario solicita o download. Essa escolha arquitetural e intencional e deve ser mantida pelo menos ate a Fase 4 do produto.

### Como Funciona

A cada solicitacao de download, o backend executa os seguintes passos:

1. Busca o registro do curriculo gerado no banco (contendo `content_markdown` e metadados da vaga)
2. Converte o markdown para o formato interno da biblioteca DOCX
3. Aplica o template ATS-friendly
4. Escreve o arquivo em um `ByteArrayOutputStream`
5. Retorna o binario diretamente na resposta HTTP com o `Content-Disposition` correto

O arquivo nao e salvo em disco apos a geracao. Cada download gera um arquivo novo a partir dos dados mais recentes.

### Vantagens dessa Estrategia

- **Nenhum arquivo obsoleto**: o conteudo do curriculo no banco e sempre a versao mais atual. Se o usuario edita o curriculo apos gerar, o proximo download reflete as alteracoes automaticamente.
- **Backup trivial**: o backup do banco de dados ja inclui todo o conteudo. Nao ha necessidade de sincronizar arquivos em disco com registros no banco.
- **Sem complexidade de storage**: sem integracao com S3, GCS, ou sistema de arquivos externo. Sem politicas de retencao, sem cleanup de arquivos orfaos.
- **Consistencia**: nao existe o cenario de "o arquivo no disco nao corresponde ao registro no banco".

### Desvantagens Aceitas

- **Latencia no download**: a geracao do DOCX adiciona 1 a 3 segundos ao tempo total de resposta. Para o MVP, isso e aceito — o usuario espera um arquivo para abrir no Word, e alguns segundos sao aceitaveis nesse contexto.
- **Repeticao de processamento**: se o mesmo curriculo for baixado multiplas vezes, o arquivo sera gerado multiplas vezes. Isso e irrelevante no MVP (volume预期的 e baixo) e aceitavel ate a Fase 4.

### Comparacao com Alternativas

| Estrategia | Stale Data | Backup | Custo | Complexidade |
|---|---|---|---|---|
| Salvar binario no banco | Possivel | Dificil (backup grande) | Armazenamento DB | Baixa |
| Salvar em storage externo (S3/GCS) | Possivel | Externo ao DB | Custo mensal | Media |
| Geracao sob demanda (escolhida) | Nao ocorre | Simples | Zero | Baixa |

A coluna "Stale Data" e o fator decisivo. Currculos otimizados podem ser editados apos a geracao. Se o arquivo DOCX for persistido, qualquer edicao subsequente exigira um mecanismo de re-geracao ouo curriculo ficara desatualizado no disco. A estrategia sob demanda elimina esse problema pela raiz.

---

## 2. Biblioteca Recomendada para Java

### Opcao 1: Apache POI (XWPF)

Apache POI e a biblioteca madura para manipulacao de documentos Office. O modulo XWPF专门处理 documentos Word (.docx). E mantida pela Apache Foundation desde2009, com vasta documentacao e exemplos disponiveis. A API e verbosa — criar um documento comPOI exige constructing paragraphs, runs, fonts e estilos programaticamente — mas e previsivel e bem testada.

Vantagens:
- Estavel e amplamente utilizada em producao
- Suporte a estilos, cabecalhos, rodapes, tabelas
- Comunidade ativa e grande volume de exemplos

Desvantagens:
- API verbosa: muito codigo boilerplate para operacoes simples
- Tratamento de memoria pode ser problematico em documentos muito grandes

### Opcao 2: docx4j

docx4j e baseada em JAXB e modela o documento Word como objetos Java que refletem a estrutura XML interna do .docx. E mais expressiva para documentos com estrutura complexa (sumarios, referencias cruzadas, campos complexos).

Vantagens:
- Modelo de objetos mais limpo para documentos complexos
- Suporte nativo a estilos via XML

Desvantagens:
- Dependencia JAXB (pode conflitar com versoes mais recentes do Java)
- Curva de aprendizado mais alta para a equipe

### Recomendacao para o MVP: Apache POI XWPF

Para currculos, que sao documentos com estrutura relativamente simples (paragrafos, listas, titulos), a verbosidade do POI nao e um problema real. A equipe vai escrever um unico metodo de geracao e raramente precisara modifica-lo. A maturidade da biblioteca e a abundancia de exemplos superam a vantagem de expressividade do docx4j para esse caso de uso.

**Bibliotecas que nao devem ser utilizadas**:
- Qualquer library que gere ODT (LibreOffice): o formato nao e lido pela maioria dos ATS
- Bibliotecas HTML-to-DOCX: o resultado e estruturalmente incorreto para ATS, gerando problemas de indexacao

---

## 3. Template ATS-Friendly — Estrutura Obrigatoria

Um curriculo otimizado para ATS (Applicant Tracking System) deve seguir uma estrutura previsivel que os sistemas de rastreamento de candidatos consigam parsear corretamente. A ordem das secoes e fixa e deve ser respeitada em todo documento gerado.

### Secoes Obrigatorias (nesta ordem)

1. **Cabecalho de contato**: nome completo em destaque (tam. maior, possivelmente bold), seguido na mesma linha ou logo abaixo de email, telefone, cidade/UF, e LinkedIn (se houver). LinkedIn aparece como texto, nao como link clicavel — ATS nao segue links.

2. **Resumo profissional**: 3 a 4 linhas logo apos o cabecalho. Este e o unico bloco de texto livre no topo do curriculo e e crucial para que o ATS identifique rapidamente o perfil do candidato. Deve conter as principais keywords do cargo.

3. **Experiencia profissional**: bloco principal do curriculo. Cada posicao segue o formato: cargo em destaque, empresa, periodo (mes/ano — mes/ano). Abaixo, bullets com conquistas尽量量化adas (ex.: "Aumentei a produtividade em 30%"). ATS extrai cargo, empresa e periodo para indexacao.

4. **Formacao academica**: formato direto: grau + area + instituicao + periodo. Apenas oultimo nivel academico (ou os dois mais relevantes se forem de areas distintas).

5. **Skills / Habilidades tecnicas**: lista de competencias tecnicas relevantes para a vaga. Pode ser em formato de bullets ou lista separada por virgula. ATS identifica como bloco de keywords.

6. **Certificacoes**: secao opcional. Formato: nome da certificacao — emissor — ano. Incluir apenas certificacoes vigentes e relevantes.

---

## 4. Criterios ATS — O Que Evitar

Applicant Tracking Systems (como Workday, Greenhouse, Lever, Taleo) parseiam curriculos convertendo-os em texto plain. Qualquer elemento que atrapalhe esse parsing prejudica a pontuacao do candidato e deve ser eliminado do template.

### Elementos Proibidos

- **Tabelas complexas**: a maioria dos ATS le tabelas como valores separados por virgula, perdendo a estrutura. Tabelas com mais de 2 colunas sao especialmente problematicas.
- **Imagens, icones, logos**: nao sao lidos por nenhum ATS mainstream. Nao ha excecao.
- **Layout em multiplas colunas**: curriculos em formato de jornal (duas ou tres colunas) sao desestruturados pelo parser. Layout deve ser estritamente vertical.
- **Cabecalhos e rodapes com informacao critica**: nome e contato no rodape sao frequentemente ignorados pelo parser. Tudo importante deve estar no corpo do documento.
- **Texto em bold simulando titulo**: ATS identifica titulos por estilos de Heading (Heading 1, 2, 3). Texto em bold comum nao e reconhecido como titulo. Usar estilos de Heading reais.
- **Caixas de texto flutuantes / text boxes**: elementos posicionados livremente na pagina sao completamente ignorados ou causam parsing incorreto.
- **Fontes nao-padrao**: fontes exoticas podem nao ser renderizadas no ambiente do recrutador. Usar Arial, Calibri, Times New Roman apenas.
- **Texto em imagem**: muito comum em curriculos com design elaborado — o texto renderizado em imagem e invisivel para ATS. O curriculo deve ser texto puro.
- **Quebras de pagina manuais**: forcar quebras de pagina para controlar o layout atrapalha o parsing. Deixar o conteudo fluir naturalmente.
- **Keywords escondidas**: practice de colocar keywords em texto branco sobre fundo branco, ou em fontes de tamanho 1, e considerada manipulacao e pode resultar em desqualificacao automatica. Nao fazer.

---

## 5. Template DOCX Minimo para MVP

O template abaixo define a estrutura exata que o codigo Java deve gerar. Cada secao corresponde a um estilo de paragrafo no documento Word.

```
[NOME DO CANDIDATO — Heading1, fonte16pt, bold]

[EMAIL] | [TELEFONE] | [CIDADE/UF] | [LINKEDIN] — paragrafo normal, fonte 10pt

---

RESUMO — Heading 2, fonte 12pt, bold
[3-4 linhas de resumo profissional] — paragrafo normal, fonte 11pt

EXPERIENCIA PROFISSIONAL — Heading 2, fonte 12pt, bold
[Cargo] — [Empresa] | [Periodo] — paragrafo bold, fonte 11pt
• [Descricao com conquistas mensuraveis quando disponiveis] — bullet, fonte 11pt
• ...
[Proxima experiencia]

FORMACAO ACADEMICA — Heading 2, fonte 12pt, bold
[Grau] em [Area] — [Instituicao] | [Periodo] — paragrafo normal, fonte 11pt

SKILLS — Heading 2, fonte 12pt, bold
• [Skill 1], [Skill 2], [Skill 3] — bullet, fonte 11pt

CERTIFICACOES — Heading 2, fonte 12pt, bold (se aplicavel)
• [Certificacao] — [Emissor] | [Ano] — bullet, fonte 11pt
```

### Detalhes de Implementacao

- Nome: Heading 1, 16pt, bold, centralizado ou justificado a esquerda
- Contato: mesma linha, separado por pipe (`|`), fonte 10pt
- Titulos de secao: Heading 2,12pt, bold, sem numeracao
- Corpo: paragrafo normal, 11pt, justificado
- Margens do documento: 2,5cm em todos os lados (margem estreita o suficiente para curriculos de uma pagina)
- Tamanho de pagina: A4
- Lingua do documento: pt-BR (propriedade do documento Word)

---

## 6. Conversao Markdown para DOCX

O conteudo do curriculo e armazenado no banco como `content_markdown` (string em formato Markdown). A conversao para DOCX nao e uma transformacao 1:1 direta — cada elemento Markdown tem um mapeamento especifico para o formato Word.

### Mapeamento de Elementos

| Elemento Markdown | Representacao no DOCX |
|---|---|
| `# Titulo` | Paragrafo com estilo Heading 1 |
| `## Secao` | Paragrafo com estilo Heading 2 |
| `### Sub-secao` | Paragrafo com estilo Heading 3 |
| `**negrito**` | Run com `setBold(true)` |
| `*italico*` | Run com `setItalic(true)` |
| `- item de lista` | Paragrafo com estilo bullet list (nao tabela) |
| `1. item numerado` | Paragrafo com estilo numbered list |
| `[texto](url)` | Apenas o texto visivel; link ignorado no DOCX |
| `` `codigo` `` | Run com fonte monospace (Courier), sem cor de fundo |
| `---` (horizontal rule) | Quebra de paragrafo ou espacador vertical |

### Pontos Criticos da Conversao

- **Bullets**: nunca usar tabelas do Word para simular bullets. Usar a estrutura de paragrafo com numeracao/bullets nativos do XWPF (`NumberingFormat.BULLET`). Tabelas sao ignoradas ou mal parseadas por ATS.
- **Links**: o link em si e descartado; apenas o texto visivel e preservado. ATS nao segue URLs e a presenca de links em currculos nao agrega.
- **Code blocks**: renders como texto plano em fonte monospace. Currculos nao devem conter blocos de codigo significativos — se houver, devem ser convertidos para texto explicativo.
- **Imagens em markdown**: se o markdown contiver sintaxe de imagem (`![alt](url)`), a imagem e completamente ignorada na geracao do DOCX. Nao ha conversao de markdown de imagem para forma equivalente no Word.

### Biblioteca de Parser

A biblioteca recomendada para parsear Markdown em Java e **Commonmark (org.commonmark)**. Ela gera uma arvore de nodos que pode ser iterada para emitir elementos XWPF correspondentes. Alternatives como marked.js (via transpilacao) ou jtwig markdown engine adicionam dependencia desnecessaria. Commonmark e leve, estavel e suficiente para o subset de markdown usado nos curriculos.

---

## 7. Quando Fariam Sentido Salvar o Arquivo em Storage (Fase 4)

A estrategia de geracao sob demanda e a escolha correta para o MVP e para a Fase 2-3. Existem, contudo, condicoes em que migrar para storage persistente faz sentido.

### Gatilhos para Migracao

**Volume de downloads**: quando o sistema atingir mais de 1.000 downloads por dia, a latencia acumulada de geracao sob demanda comecara a ser perceptivel no tempo medio de resposta do endpoint de download. Esse e o momento de medir e avaliar.

**Tempo de geracao**: se o tempo medio de geracao de um DOCX ultrapassar 5 segundos (medido em producao, nao em ambiente de desenvolvimento), a experiencia do usuario comeca a degradar. Isso pode acontecer se os curriculos ficarem significativamente mais longos ou se a infra for limitada.

**Auditoria de arquivo enviado**: se houver exigencia de rastrear exatamente qual arquivo foi enviado para qual vaga (para compliance ou evidentiary purposes), e necessario persistir o binario no momento do download para ter um registro imutavel.

**Conteudo estavel**: quando o produto evoluir e curriculos gerados deixarem de ser editados frequentemente (ou seja, o usuario considera o curriculo gerado como "enviado" e imutavel), persistir o arquivo faz sentido porque o custo de re-geracao nao e mais recuperado por beneficios de atualizacao.

### Critrios Quantitativos para Decisao

| Metrica | Limiar |
|---|---|
| Downloads por dia | > 1.000 |
| Tempo medio de geracao DOCX | > 5 segundos |
| Percentual de downloads repetidos do mesmo curriculo | > 30% |
| Requisito de auditoria de arquivo enviado | Sim |

Se dois ou mais desses criterios forem atingidos simultaneamente, a migracao para storage (S3 ou disco local com path no banco) deve ser planejada na Fase4.

---

## 8. Versionamento de Template

### O Template e Codigo, Nao Dado

O template de geracao DOCX e uma classe Java (`ResumeDocxGenerator` ou similar) que live no codebase da aplicacao. Ele nao e um arquivo de configuracao, nem um registro no banco, nem um template em linguagem de template (como Freemarker ou Thymeleaf). Ele e codigo versionado via git.

### Implicacoes dessa Decisao

- **Revisao de codigo**: qualquer mudanca no template passa por pull request e code review como qualquer outra mudanca.
- **Versionamento semantico**: mudancas no template seguem versao semantica. Se a estrutura do documento muda de forma que currculos antigos nao seriam mais regenerados de forma identica, isso e uma quebra de compatibilidade e deve ser documentado.
- **Rollback trivial**: se um template causar problemas em producao, reverter e uma questao de `git revert`.
- **Testes**: o metodo de geracao deve ter testes unitarios que verificam a saida DOCX (ou pelo menos o conteudo parsed) para inputs conhecidos.

### O Que Nao Fazer no MVP

- **Template dinamico via config**: ter o template carregado de um arquivo YAML/JSON permite alteracoes sem deploy, mas adiciona complexidade de validacao, tipagem e testes que nao se justifica no MVP.
- **Multiplos templates por perfil**: ter templates diferentes para diferentes tipos de vaga (tech, gestao, operacional) e uma funcionalidade da Fase 3+. No MVP, um unico template cobre todos os casos.
- **Personalizacao por usuario**: permitir que o usuario escolha o template ou altere cores/fontes do DOCX esta fora do escopo do MVP e da Fase 2.

### Estrategia de Evolucao do Template

Quando o template precisar mudar (ex.: adicionar secao de idiomas, alterar ordem das secoes, suportar mais secoes de experiencia):

1. Criar nova classe ou metodo com versao incrementada
2. Manter o metodo antigo funcionando para regeneracao de versoes antigas
3. Nova geracao usa o template novo
4. Testes cobrem ambas as versoes
5. Documentar a mudanca no changelog do release

Essa abordagem evita quebrar currculos ja gerados e permite que a equipe faca auditabilidade de qual template foi usado em cada geracao (armazenar `template_version` no registro do generated resume).
