# Regras e Diretrizes para o Agente de IA

Este documento define as regras obrigatórias, contratos de dados e estratégias para toda comunicação com provedores de IA no sistema de geração de currículos otimizados. O objetivo é garantir que o sistema mantenha integridade factual, evite alucinações e produza resultados rastreáveis e auditáveis.

---

## 1. Regras Obrigatórias para o Agente de IA

Estas regras são de cumplimiento obrigatório e devem ser incluídas integralmente no **system prompt** de toda chamada ao provedor de IA.

### 1.1 Proibições Absolutas

O agente de IA **NUNCA** deve:

1. **Inventar experiências profissionais** — cargos, empresas, datas de início/término ou descrições de atividades que não estejam presentes no currículo base fornecido.
2. **Inventar empresas ou empregadores** — nenhum nome de empresa pode aparecer no currículo otimizado se não estiver no currículo base.
3. **Inventar cargos ou funções** — títulos de posição, senioridade ou responsabilidades devem derivar exclusivamente do currículo base.
4. **Inventar certificações ou credenciais** — nenhum certificado, curso, formação acadêmica ou licença profissional pode ser adicionado se não constar no currículo base.
5. **Inventar tecnologias** — linguagens, frameworks, ferramentas, bibliotecas e plataformas devem estar presentes no currículo base ou ser explicitamente mencionadas na descrição da vaga.
6. **Inventar anos de experiência** — contagens de anos com tecnologias, em liderança ou em setor devem ser calculadas exclusivamente a partir dos dados fornecidos.
7. **Inventar métricas, KPIs ou resultados quantitativos** — nenhum número de percentual, receita, equipe gerenciada, custo reduzido ou melhoria de performance pode ser inserido se não estiver no currículo base.
8. **Transformar contato básico/iniciante com tecnologia em proficiência "avançada" ou "sênior"** — se o currículo base diz "Python (básico)", o currículo otimizado não pode listar "Python (avançado)". A única exceção é se a descrição da vaga exigir um nível que o candidato ainda não possui e houver um campo explícito para "gap identificado" — nesse caso, deve-se注明 que o candidato tem conhecimento básico e está em processo de crescimento na área.
9. **Fazer keyword stuffing** — não repetir palavras-chave da descrição da vaga de forma excessiva, descontextualizada ou em frequências que comprometam a legibilidade ou a honestidade do documento. O limite aceitável é natural: uma palavra-chave relevante pode aparecer 2 a 4 vezes em um currículo de 2 páginas, desde que inserida organicamente.
10. **Inserir expectativas salariais** — o currículo otimizado não deve conter nenhum valor, faixa ou referência a remuneração, benefícios ou pretensão salarial, independentemente do que conste na descrição da vaga.
11. **Adicionar informações não presentes** — nenhum dado pessoal adicional (telefone, endereço, links de portfólio, perfis sociais) pode ser criado se não estiver no currículo base ou na descrição da vaga.

### 1.2 Diferenciação Obrigatória de Fontes

Para toda informação presente no currículo otimizado, o agente deve categorizar explicitamente a origem:

- **(a) Fatos do currículo base** — informações literalmente presentes nos dados fornecidos. Devem ser preservadas com fidedignidade, incluindo grafias, métricas e datas exatas.
- **(b) Inferências lógicas** — conclusões razoáveis derivadas de fatos conhecidos (ex.: se o candidato foi "Tech Lead" por 3 anos, é razoável inferir experiência em gestão técnica de equipe). Devem ser marcadas como "inferência" no campo `notes.inferences[]` da resposta estruturada e podem aparecer no currículo otimizado como reformulação do fato original.
- **(c) Lacunas identificadas** — informações que a descrição da vaga solicita mas que o currículo base não contém. Devem ser listadas em `notes.absent_information[]` e **não** devem ser inventadas no currículo. Se a aderência calculada for abaixo de 50%, o sistema deve explicitar essa informação no campo `notes.warnings[]`.

### 1.3 Limites de Conteúdo

- O currículo otimizado gerado deve respeitar o limite de **2 páginas** quando exportado para DOCX.
- Conteúdo irrelevante para a vaga específica deve ser removido ou minimizado.
- Experiências anteriores a 10 anos ou não relacionadas à vaga devem ser condensadas ou omitidas, a menos que o candidato explicitly solicite inclusão.
- Para vagas júnior ou estágio, experiências acadêmicas e projetos pessoais têm peso maior e devem ser expandidas.

---

## 2. Dados que Podem Ser Utilizados

O agente de IA pode consumir as seguintes fontes de dados:

### 2.1 Currículo Base do Usuário

Todos os campos presentes na tabela `resume_profiles`:
- `title` — título nomeado pelo usuário para o currículo
- `content_markdown` — conteúdo formatado em Markdown
- `content_jsonb` — estrutura JSON com campos normalizados: `personal_info`, `experience[]`, `education[]`, `skills[]`, `certifications[]`, `languages[]`, `summary`
- `user_id` — referência ao dono do currículo (nunca enviado ao provedor de IA)

### 2.2 Descrição da Vaga

Texto integral da tabela `job_applications`:
- `company` — nome da empresa
- `job_title` — título ou denominação do cargo
- `location` — cidade/estado/remote
- `employment_type` — CLT, PJ, estágio, etc.
- `seniority` — júnior, pleno, sênior, especialista
- `raw_description` — texto livre da descrição da vaga
- `source_url` — URL de origem (para referência, não enviado ao provedor)

### 2.3 Histórico de Análises Anteriores

Se existir uma `generated_resumes` prévia para a mesma combinação de `resume_profile_id` + `job_application_id`, o sistema pode enviar à IA:
- A análise de aderência anterior (scores, lacunas identificadas)
- A versão anterior do currículo otimizado

**Objetivo:** Permitir que a IA identifique evolução e evite repetir recomendações já implementadas.

### 2.4 Preferências Explícitas do Usuário (Futuro)

Na versão MVP este campo não existe, mas a arquitetura deve prever o envio de:
- Formato preferido de currículo (funcional, cronológico inverso)
- Extensão desejada (1 página, 2 páginas)
- Foco prioritário (experiência mais recente vs. projetos técnicos)

---

## 3. Dados que Não Podem Ser Inventados

A lista abaixo é exaustiva. Qualquer item que não conste explicitamente no currículo base **não pode** aparecer no currículo otimizado sob nenhuma circunstância:

1. Experiências profissionais — empresas, cargos, datas, descrições de atividades
2. Formação acadêmica — instituições, cursos, níveis, datas de conclusão
3. Certificações — certificações técnicas, cursos livres, workshops
4. Habilidades técnicas — linguagens, frameworks, ferramentas, plataformas
5. Nível de proficiência em qualquer habilidade listada (básico, intermediário, avançado, fluente)
6. Idiomas — nenhum idioma pode ser adicionado além dos presentes no currículo base
7. Métricas quantitativas — números de qualquer natureza (percentuais, valores financeiros, tamanhos de equipe, prazos)
8. Resultados ou conquistas — marcos alcançados, projetos liderados, problemas resolvidos
9. Contato e dados pessoais — e-mail, telefone, LinkedIn, portfólio, site
10.soft skills ou competências comportamentais que não estejam implícitas ou explícitas nos fatos do currículo

Se a descrição da vaga solicitar algo que o currículo base não possui, a resposta correta é listar a lacuna em `notes.absent_information[]` e manter o currículo otimizado focado no que existe — sem compensação inventada.

---

## 4. Contrato de Saída Estruturada

Toda chamada de geração deve produzir uma resposta em JSON com a seguinte estrutura conceitual. O schema deve ser utilizado para validação via `AiProvider.validateOutput()`.

### 4.1 Schema Conceitual

```json
{
  "adherence_analysis": {
    "score": "number (0-100)",
    "level": "string (enum: baixa, moderada, boa, excelente)",
    "summary": "string (2-3 sentences explaining overall fit)",
    "strengths": [
      "string (specific strength derived from resume match)"
    ],
    "gaps": [
      "string (specific gap: what's missing or misaligned)"
    ],
    "keyword_matches": {
      "found": ["string (keyword from job description present in resume)"],
      "missing": ["string (keyword from job description absent in resume)"],
      "partial": ["string (keyword partially matched or at wrong proficiency level)"]
    }
  },
  "optimized_resume": {
    "text": "string (plain text version, suitable for copying)",
    "markdown": "string (full Markdown with formatting)",
    "json": {
      "personal_info": { "name": "string", "contact": {} },
      "summary": "string",
      "experience": [{ "company": "string", "title": "string", "period": "string", "highlights": ["string"] }],
      "education": [{ "institution": "string", "degree": "string", "year": "string" }],
      "skills": [{ "category": "string", "items": ["string"] }],
      "certifications": [{ "name": "string", "issuer": "string", "year": "string" }],
      "languages": [{ "language": "string", "proficiency": "string" }]
    }
  },
  "notes": {
    "inferences": [
      "string (logical conclusion derived from facts, marked as inference)"
    ],
    "absent_information": [
      "string (information requested by job description but not in resume base)"
    ],
    "warnings": [
      "string (e.g., 'Adherence below 50%. Significant gaps identified.')"
    ]
  }
}
```

### 4.2 Regras de Validação da Saída

- `adherence_analysis.score` deve ser um número entre 0 e 100.
- `adherence_analysis.gaps` deve listar todas as lacunas identificadas — nunca retornar array vazio quando a descrição da vaga contém requisitos não atendidos.
- `optimized_resume.markdown` deve ter no máximo 2000 palavras (equivalente a ~2 páginas DOCX).
- `notes.absent_information` nunca deve ser omitido — se não houver lacunas, enviar array vazio `[]`.
- Se `adherence_analysis.score < 50`, o campo `notes.warnings` deve conter ao menos uma entrada explicando a situação.

---

## 5. Modelo Conceitual de Abstração de Provedor

A arquitetura utiliza uma camada de abstração (`AiProvider`) que normaliza a comunicação com provedores de IA. Isso permite trocar o provedor subjacente (Gemini, OpenRouter, OpenAI, Claude) alterando exclusivamente uma configuração, sem impacto no código da aplicação.

### 5.1 Interface do Contrato

```
AiProvider (interface)

  String getProviderId()
    Retorna o identificador único do provedor (ex.: "openai", "anthropic", "gemini").
    Usado para logging, seleção e auditoria.

  String getDefaultModel()
    Retorna o modelo padrão configurado para este provedor.
    Pode ser sobrescrito por opção em tempo de chamada.

  GenerationResult generateResumeAnalysis(prompt: String, options: GenerationOptions): GenerationResult
    Gera a análise de aderência entre currículo base e descrição da vaga.
    prompt: system prompt + user prompt combinados.
    options: temperatura, max_tokens, timeout, modelo override.

  GenerationResult generateOptimizedResume(resumeJson: Object, jobDescription: Object, options: GenerationOptions): GenerationResult
    Gera o currículo otimizado a partir do JSON do currículo base e da descrição da vaga.
    resumeJson: conteúdo do campo content_jsonb.
    jobDescription: campos relevantes da job_application.

  ValidationResult validateOutput(rawResponse: String, schema: OutputSchema): ValidationResult
    Valida se a resposta bruta do provedor conforms ao schema esperado.
    Retorna: válido (true/false), erros de validação, JSON parsed (se válido).

  CostEstimate estimateCost(inputTokens: Integer, outputTokens: Integer): CostEstimate
    Retorna uma estimativa de custo com base no provedor, modelo e contagem de tokens.
    Usado para exibir custo estimado antes da chamada (feature futura).
```

### 5.2 Estrutura GenerationResult

```
GenerationResult
  success: Boolean
  rawResponse: String (resposta原文 do provedor)
  parsedResponse: Object (JSON parseado e validado)
  provider: String
  model: String
  tokensUsed: { input: Integer, output: Integer, total: Integer }
  costEstimate: Decimal
  durationMs: Long
  errorMessage: String (null se success=true)
```

### 5.3 Estrutura ai_runs (persistida no banco)

Cada chamada ao provedor gera um registro em `ai_runs`:
- `id`, `user_id`, `provider`, `model`, `prompt_type` (analysis | resume), `raw_request`, `raw_response`, `tokens_used`, `cost_estimate`, `duration_ms`, `created_at`

---

## 6. Como Lidar com Diferentes Provedores

Cada provedor possui características próprias que a camada de abstração mascara:

| Aspecto | OpenAI | Anthropic (Claude) | Google Gemini | OpenRouter |
|---|---|---|---|---|
| Formato da API | Chat Completions | Messages | generateContent | Varia por modelo |
| Contagem de tokens | Incluída na resposta | Incluída na resposta | Estimada via API | Via parametro |  |
| Limite de rate | 60 req/min (tier) | 50 req/min (tier) | 15 req/min (free) | Varia por modelo |
| Temperatura recomendada | 0.3–0.5 | 0.3–0.5 | 0.3–0.5 | 0.3–0.5 |
|Timeout típico | 60s | 60s | 90s | 120s |

### 6.1 Normalização na Camada de Abstração

A implementação concreta de `AiProvider` para cada provedor é responsável por:
1. Traduzir o formato de entrada (prompt + options) para o formato de API específico do provedor.
2. Extrair `tokens_used`, `cost_estimate` e `duration_ms` da resposta da API ou calcular por estimativa.
3. Mapear erros do provedor (rate limit, timeout, content filter) para exceções normalizadas.
4. Mapear erros 4xx/5xx HTTP para `GenerationResult.success = false` com `errorMessage` descritivo.

### 6.2 Armazenamento para Auditoria e Compliance

Toda resposta bruta (`raw_response`) é armazenada em `ai_runs.raw_response`. Isso serve para:
- Debugging de falhas de parsing ou validação
- Auditoria de compliance para demonstrar que o sistema não alterou respostas da IA
- Análise de padrões de uso e custo por provedor
- Recuperação em caso de inconsistências entre `parsed_response` e `raw_response`

Trocar o provedor equivale a alterar o valor de `AI_PROVIDER` na configuração de ambiente — não exige mudança de código na camada de aplicação.

---

## 7. Estratégia de Engenharia de Prompt

### 7.1 System Prompt (Fixo)

O system prompt é static e contém as regras obrigatórias das seções 1, 2 e 3 deste documento. Ele é enviado em toda chamada e não varia entre requisições. Exemplo de estrutura:

```
Você é um assistente especializado em otimização de currículos para recrutamento.
Seu objetivo é analisar a aderência entre um currículo base e uma descrição de vaga,
e produzir um currículo otimizado que destaque os pontos fortes do candidato.

REGRAS ABSOLUTAS:
[Nicole as 11 regras da seção 1.1]

FONTES DE DADOS PERMITIDAS:
[Nicole as fontes da seção 2]

INFERÊNCIAS VS. FATOS:
[Nicole as categorias da seção 1.2]

FORMATO DE SAÍDA:
[Nicole o schema da seção 4]
```

### 7.2 User Prompt (Dinâmico)

O user prompt varia a cada chamada e é construído pela aplicação com a seguinte estrutura:

```
CURRÍCULO BASE DO CANDIDATO:
[Conteúdo do campo content_jsonb ou content_markdown]

DESCRIÇÃO DA VAGA:
Empresa: [company]
Cargo: [job_title]
Local: [location]
Regime: [employment_type]
Senioridade: [seniority]
Descrição: [raw_description]

[Se houver histórico prévio]
ANÁLISE ANTERIOR:
[Resumo da análise prévia com gaps já identificados]

INSTRUÇÕES DE SAÍDA:
Retorne APENAS o JSON conforme o schema definido no system prompt.
Não inclua texto explicativo fora do JSON.
```

### 7.3 Parâmetros de Geração

Para a geração do currículo otimizado, os seguintes parâmetros devem ser usados:

- **Temperatura:** 0.3 (faixa 0.3–0.5 aceitável). Valor baixo é essencial para garantir factualidade e consistência — a variação criativa é contraproducente em geração de currículos.
- **Max tokens:** 4096 para análise; 8192 para currículo otimizado (margem para conteúdo extenso).
- **Timeout:** 90 segundos.
- **Few-shot examples:** Não incluídos na MVP. A adição de exemplos few-shot é prevista para a Fase 2, mediante análise de qualidade das saídas da Fase 1.

---

## 8. Segurança e Conformidade

### 8.1 Minimização de PII

Antes de enviar qualquer conteúdo ao provedor de IA, a aplicação deve remover ou anonimizar campos de PII (Personally Identifiable Information):
- **Nome completo** — enviado ao provedor apenas como identificação no currículo (necessário para formatação do documento).
- **E-mail, telefone, endereço** — **não enviados** ao provedor na MVP. A estrutura `personal_info.contact` é reconstruída a partir do currículo base após a geração, sem passar pela IA.
- **URLs de redes sociais e portfólios** — são equivalentes a e-mail/telefone; reconstruídos localmente após a geração.

**Justificativa:** Minimizar a exposição de dados pessoais a terceiros (provedores de IA) é uma prática de privacidade recomendada, especialmente em jurisdições que seguem LGPD.

### 8.2 Logging de Auditoria

Cada chamada ao provedor de IA gera um registro completo em `ai_runs`:
- `provider` e `model` — para rastreabilidade
- `created_at` — timestamp UTC
- `tokens_used` (input, output, total) — para controle de custo
- `cost_estimate` — em USD ou BRL conforme configuração
- `duration_ms` — para identificar chamadas lentas ou timeout
- `raw_request` e `raw_response` — para debugging e compliance

### 8.3 Armazenamento de Respostas Brutas

A resposta crua do provedor (`raw_response`) é armazenada integralmente. Nunca se deve armazenar apenas o JSON parseado — divergences entre o parsed e o raw podem indicar problemas de parsing, injeção de contexto ou comportamento inesperado do modelo.

### 8.4 Rate Limiting

A camada de abstração deve implementar rate limiting no lado da aplicação:
- Máximo de 10 chamadas simultâneas por usuário
- Backoff exponencial em caso de erro 429 do provedor
- Fila de prioridades para chamadas de geração vs. análise (geração tem prioridade mais alta)
