# SPEC-05 — Regras do Agente de IA

**Versão**: 1.0
**Data**: 2026-06-01
**Projeto**: Resume Forge
**Status**: Aprovado para Implementação

---

## 1. Proibições Absolutas (11 Regras)

O Agente de IA **nunca** pode, sob nenhuma circunstância, produzir o seguinte conteúdo. Qualquer violação resulta em rejeição imediata da saída e aciona o Fluxo 10 (Camada 3 — Fallback de Saída).

###1.1 Proibições Funcionais

**P1 — Fabricação de Dados Fáticos**
- O Agente não pode inventar nomes de empresas, datas de início/término de emprego, números de telefone, endereços de e-mail, URLs de perfis sociais ou qualquer identificador que não conste no currículo base.
- **Critério**: qualquer nome de empresa, CNPJ, telefone ou URL encontrado na saída deve ser rastreável a um campo presente em `content_jsonb` do currículo base.

**P2 — Fabricação de Qualificações**
- O Agente não pode inventar certificações, diplomas, prêmios, publicações ou competências técnicas que não estejam no currículo base.
- **Exceção controlada**: inferências lógicas permitidas (ver Seção 2b) — mas nunca apresentar inferência como fato.

**P3 — Exageros de Aderência**
- O Agente não pode atribuir `adherence_analysis.score` superior a 85 quando requisitos-chave da vaga não estão presentes no currículo base.
- **Critério**: `score > 85` implica que pelo menos 80% dos requisitos listados em `requirements` estão explicitamente cobertos em `content_jsonb`.

**P4 — Informações Contraditórias**
- O Agente não pode incluir na seção de experiência informações que contradizem o currículo base (ex.: cargo diferente para a mesma empresa, datas sobrepostas sem justificativa).
- **Critério**: todas as informações de experiência no `content_markdown` devem ser consistentes com `content_jsonb.experience[].company`, `content_jsonb.experience[].jobTitle`, `content_jsonb.experience[].startDate` e `content_jsonb.experience[].endDate`.

**P5 — Conteúdo Discriminatório**
- O Agente não pode gerar, inferir ou sugerir características protegidas do candidato: origem étnica, religião, orientação sexual, estado civil, deficiência não declarada, opinião política, sindicalização.
- **Critério**: análise automática (regex) deve registrar alerta em `notes.gaps` se qualquer menção a características protegidas for detectada.

**P6 — Informações Legais Erradas**
- O Agente não pode incluir valores numéricos de salário, benefícios, regimes contratuais (CLT/PJ) ou qualquer afirmação legal que não possa ser verificada no currículo base.
- **Critério**: menções a regime de contratação, faixa salarial ou benefícios devem vir exclusivamente de `content_jsonb` ou estar marcadas como `inferred: true` em `notes.inferences`.

**P7 — Dados de Terceiros**
- O Agente não pode incluir informações sobre pessoas que não sejam o próprio candidato (ex.: cônjuge, referências pessoais com nome completo, contatos de emergência).
- **Critério**: o campo `content_jsonb.contact` (se existir) deve conter apenas o nome e contexto profissional da referência, sem dados pessoais sensíveis.

**P8 — Conteúdo Malicioso**
- O Agente não pode gerar código-fonte, links de download, instruções técnicas, URLs ou qualquer conteúdo que possa ser utilizado para fins maliciosos.
- **Critério**: links externos permitidos apenas se forem `linkedin.com` ou `github.com` presentes no currículo base.

**P9 — Conteúdo Publicitário**
- O Agente não pode incluir jargões de marketing, superlativos não fundamentados ("o melhor desenvolvedor do mundo"), comparações com outros candidatos ou promessas de resultados.
- **Critério**: tom deve ser profissional e factual; usar `tone_guidance` do prompt do usuário.

**P10 — Cópia Literal de Textos de Vaga**
- O Agente não pode copiar parágrafos inteiros da descrição da vaga para o currículo otimizado.
- **Critério**: similaridade textual (Jaccard index) entre qualquer parágrafo do `optimized_resume.markdown` e parágrafos correspondentes da `job_description` deve ser inferior a 0.3.

**P11 — Dados de Contato Falsos**
- O Agente não pode gerar, inferir ou modificar os dados de contato (e-mail, telefone, LinkedIn, GitHub) do candidato.
- **Tratamento**: dados de contato são **reconstruídos localmente** após a geração (ver Seção 11) e **nunca** fazem parte do prompt enviado ao provedor de IA.

---

## 2. Diferenciação de Fontes

O Agente classifica todas as informações do currículo otimizado em três categorias. O schema de saída deve refletir essa classificação.

### 2a. Fatos do Currículo Base

Informações diretamente extraídas de `content_jsonb` sem modificação.

| Campo em `content_jsonb` | Uso no Output |
|---|---|
| `personalInfo.fullName` | Reconstruído localmente (não enviado ao provedor) |
| `personalInfo.email` | Reconstruído localmente (não enviado ao provedor) |
| `personalInfo.phone` | Reconstruído localmente (não enviado ao provedor) |
| `personalInfo.location` | Usado diretamente |
| `personalInfo.summary` | Adaptado ao tom da vaga |
| `experience[].company` | Usado diretamente |
| `experience[].jobTitle` | Usado diretamente |
| `experience[].startDate` | Formatação ajustada |
| `experience[].endDate` | Formatação ajustada |
| `experience[].highlights` | Reestruturado para aderência |
| `education[].institution` | Usado diretamente |
| `education[].degree` | Usado diretamente |
| `education[].graduationDate` | Usado diretamente |
| `skills.technical` | Reordenado por relevância |
| `certifications` | Usado diretamente |
| `languages` | Usado diretamente |
| `personalInfo.linkedin` | Reconstruído localmente (não enviado ao provedor) |
| `personalInfo.portfolio` | Reconstruído localmente (não enviado ao provedor) |

### 2b. Inferências Lógicas

Inferências são deduções razonáveis baseadas em fatos do currículo. **Devem ser marcadas** com `inferred: true` no campo `notes.inferences`.

| Inferência | Base Lógica | Exemplo |
|---|---|---|
| `SKILL_FROM_TOOL` | stacklist inclui ferramenta → competência inferida | `content_jsonb.skills.tools` inclui "AWS EC2" → inferir "AWS cloud infrastructure" |
| `TENURE_CALCULATION` | datas de emprego → tempo de experiência | Somar períodos de experiência |
| `LEADERSHIP_INFERRED` | palavras-chave em descrição → nível sênior | "gestão de equipe", "liderou" → inferir habilidade de liderança |
| `DOMAIN_DEPTH` | número de projetos/anos → nível de senioridade | > 5 anos + projetos complexos → "sênior" |
| `CERT_EQUIVALENCE` | certificação internacional → competência equivalente | "AWS Solutions Architect" → kompetensi AWS |
| `EDUCATION_BONUS` | instituição de prestígio → peso adicional | USP, UNICAMP, IBM, Stanford → peso1.2x |

### 2c. Lacunas Identificadas

Lacunas são requisitos da vaga que **não** estão presentes no currículo base. São documentadas em `adherence_analysis.gaps`.

| Tipo de Lacuna | Descrição | Exemplo |
|---|---|---|
| `MISSING_SKILL` | Competência técnica ausente | Requisito: "Kubernetes", Currículo: não possui |
| `MISSING_CERTIFICATION` | Certificação requerida | Requisito: "PMP", Currículo: não possui |
| `MISSING_EXPERIENCE` | Tempo de experiência insuficiente | Requisito: "5 anos", Currículo: "3 anos" |
| `MISSING_EDUCATION` | Formação acadêmica mínima | Requisito: "Superior completo", Currículo: "Técnico" |
| `LANGUAGE_GAP` | Idioma requerido | Requisito: "Inglês avançado", Currículo: "Básico" |
| `EXPERIENCE_DOMAIN_GAP` | Domínio de atuação diferente | Requisito: "fintech", Currículo: "e-commerce" |

---

## 3. Limites de Conteúdo

### 3.1 Extensão do Documento

- **Máximo**: 2 páginas no formato DOCX (equivalente a ~800 palavras de conteúdo principal, ~200 por seção).
- **Cálculo**: o conversor Markdown → DOCX aplica as seguintes regras de truncamento sequencial:
  1. Se `markdown.length > 8000` caracteres: truncar experiências (manter as 3 mais recentes).
  2. Se ainda exceder: remover `notes` e `additional_info`.
  3. Se ainda exceder: truncar cada descrição de experiência em 2 frases.

### 3.2 Conteúdo Irrelevante

- Seção `hobbies`, `interests`, `personal philosophy` é **removida** automaticamente.
- Referências pessoais com nome completo são **removidas**.
- Endereço residencial completo é **substituído** por cidade/estado apenas.

### 3.3 Experiências Anteriores a 10 Anos

- Experiências com `end_date` anterior a 10 anos da data atual são **condensadas** em uma linha de resumo:
  ```
  ## Experiência Anterior
  Analista de Sistemas @ Empresa X (2010–2014) | Engenheiro de Dados @ Empresa Y (2008–2010) | Desenvolvedor Jr @ Empresa Z (2006–2008)
  ```
- **Exceção**: experiências anteriores relevantes para a vaga (mesmo domínio) são mantidas com descrição.

### 3.4 Currículos para Vagas Júnior

Quando `job_title` contém indicadores de nível júnior ("Júnior", "Jr.", "Entry", "Pleno" com requisitos explícitos de < 2 anos):

- **Peso maior** para seção acadêmica (manter detalhes de formação).
- **Peso maior** para projetos e portfólio (expandir descrição).
- **Peso maior** para competências técnicas e ferramentas.
- Experiências prévias não relacionadas são condensadas ou removidas.
- Seção `projects` recebe destaque, mesmo que breve no currículo base.

---

## 4. Contrato de Saída (Output Schema)

O provedor de IA deve retornar **exclusivamente** um JSON válido conforme o schema abaixo. Qualquer desvio do schema aciona o Fluxo 10, Camada 3.

```json
{
  "adherence_analysis": {
    "score": 72,
    "matched_requirements": [
      {
        "requirement": "Python",
        "status": "matched",
        "evidence": "resume_data.skills includes 'Python' and experience[2].description mentions 'Python automation'"
      },
      {
        "requirement": "AWS",
        "status": "matched",
        "evidence": "resume_data.skills includes 'AWS EC2' and 'AWS S3'"
      },
      {
        "requirement": "Docker",
        "status": "partial",
        "evidence": "experience[1].description mentions 'containerized applications' but does not mention Docker explicitly"
      }
    ],
    "unmatched_requirements": [
      {
        "requirement": "Kubernetes",
        "status": "missing",
        "evidence": "No mention of Kubernetes in resume_data"
      },
      {
        "requirement": "Terraform",
        "status": "missing",
        "evidence": "No mention of Terraform in resume_data"
      }
    ],
    "gaps": [
      {
        "type": "MISSING_SKILL",
        "description": "Kubernetes é requerido para a posição mas não consta no currículo.",
        "severity": "high",
        "suggestion": "Adicionar 'Kubernetes' à seção de competências, mesmo que a experiência seja limitada."
      },
      {
        "type": "MISSING_EXPERIENCE",
        "description": "A vaga requer 5 anos de experiência em cloud, mas o currículo indica aproximadamente 3 anos.",
        "severity": "medium",
        "suggestion": "Detalhar projetos pessoais ou contribuições open source com infraestrutura cloud."
      }
    ],
    "strengths": [
      "Sólida experiência em Python e automação de processos.",
      "Experiência prática com AWS (EC2, S3, Lambda).",
      "Bom histórico de formação acadêmica em Ciência da Computação."
    ]
  },
  "optimized_resume": {
    "markdown": "string (conteúdo markdown completo do currículo,200-8000 caracteres)",
    "sections": {
      "header": "string (nome e contatos — reconstruído localmente após geração)",
      "professional_summary": "string (2-4 frases)",
      "experience": ["array de entradas formatadas"],
      "education": ["array de entradas formatadas"],
      "skills": ["array de competências por categoria"],
      "certifications": ["array opcional"],
      "projects": ["array opcional"]
    },
    "metadata": {
      "version": "1.0",
      "generated_at": "ISO8601 timestamp",
      "tone": "professional|modern|conservative",
      "language": "PT-BR|EN-US|ES"
    }
  },
  "notes": {
    "inferences": [
      {
        "type": "SKILL_FROM_TOOL",
        "description": "Conhecimento de infraestrutura cloud inferido a partir do uso de AWS EC2.",
        "inferred_field": "cloud_infrastructure",
        "confidence": "medium"
      }
    ],
    "warnings": [
      {
        "code": "GAPS_NOT_REPORTED",
        "message": "Score é inferior a 50 mas o array gaps está vazio. Revisar manualmente."
      }
    ],
    "generation_metadata": {
      "provider": "openai",
      "model": "gpt-4o",
      "tokens_used": {
        "input": 1847,
        "output": 2103,
        "total": 3950
      },
      "cost_estimate_usd": 0.12
    }
  }
}
```

### Regras de Preenchimento de `header`

O campo `header` no `optimized_resume.markdown` é **sempre** gerado pelo sistema localmente, **nunca** pelo provedor de IA. O template é:

```markdown
# {full_name}

{phone} | {email} | {location}
{linkedin} | {github}
```

Valores extraídos de `resume_data` e nunca enviados ao provedor.

---

## 5. Regras de Validação da Saída

A validação é executada pelo método `AiProvider.validateOutput(rawResponse, schema)` imediatamente após cada chamada ao provedor.

### 5.1 Regras de Validação Obrigatórias

| Regra | Descrição | Ação se Falhar |
|---|---|---|
| V1 — JSON Válido | `JSON.parse(rawResponse)` não lança exceção | Rejeitar, acionar Fluxo 10 Camada 3 |
| V2 — `score` numérico | `typeof score === 'number'` e `0 <= score <= 100` | Rejeitar, acionar Fluxo 10 Camada 3 |
| V3 — `gaps` não vazio quando `score < 50` | Se `score < 50`, então `gaps.length > 0` | Adicionar warning `GAPS_NOT_REPORTED`, não rejeitar |
| V4 — `markdown` não vazio | `optimized_resume.markdown.length > 0` | Rejeitar, acionar Fluxo 10 Camada 3 |
| V5 — `markdown` dentro do limite | `optimized_resume.markdown.length <= 8000` | Truncar em8000 caracteres, warn |
| V6 — `warnings` obrigatório se `score < 50` | Se `score < 50`, então `notes.warnings.length > 0` | Injetar warning `LOW_SCORE_NO_WARNING` |
| V7 — `gaps[].type` válido | Cada `gap.type` deve pertencer ao enum: `MISSING_SKILL`, `MISSING_CERTIFICATION`, `MISSING_EXPERIENCE`, `MISSING_EDUCATION`, `LANGUAGE_GAP`, `EXPERIENCE_DOMAIN_GAP` | Mapear para `UNKNOWN`, warn |
| V8 — `matched_requirements` não vazio | Se `unmatched_requirements` é vazio, `matched_requirements` deve ter pelo menos 1 item | Warn, não rejeitar |
| V9 — `language` no metadata | `optimized_resume.metadata.language` deve ser `PT-BR`, `EN-US` ou `ES` | Default para `PT-BR`, warn |
| V10 — `generated_at` presente | `optimized_resume.metadata.generated_at` presente e em ISO8601 | Adicionar timestamp local, warn |

### 5.2 Resultado da Validação

```typescript
interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
  correctedOutput: OutputSchema | null; // preenchido se correção automática foi aplicada
}

interface ValidationError {
  rule: string; // ex: "V2"
  message: string;
  path: string; // ex: "adherence_analysis.score"
  severity: "error" | "warning";
}

interface ValidationWarning {
  code: string; // ex: "GAPS_NOT_REPORTED"
  message: string;
  autoCorrected: boolean;
}
```

---

## 6. Interface `AiProvider`

```typescript
interface AiProvider {
  // Identificação do provedor
  getProviderId(): string;
  // Retorna o modelo padrão para este provedor (ex: "gpt-4o" para OpenAI)

  getDefaultModel(): string;

  // Geração de análise de vaga (prompt-only, sem currículo)
  generateJobAnalysis(
    prompt: string,
    options: GenerationOptions
  ): GenerationResult;

  // Geração de currículo otimizado (fluxo principal)
  generateOptimizedResume(
    resumeJson: ResumeData,      // já anonimizado
    jobDescription: string,
    options: GenerationOptions
  ): GenerationResult;

  // Validação da resposta bruta contra o Output Schema
  validateOutput(
    rawResponse: string,
    schema: OutputSchema
  ): ValidationResult;

  // Estimativa de custo antes da chamada
  estimateCost(
    inputTokens: number,
    outputTokens: number
  ): CostEstimate;

  // Health check do provedor
  healthCheck(): Promise<HealthResult>;
}
```

### Contrato de `GenerationOptions`

```typescript
interface GenerationOptions {
  temperature: number; // padrão: 0.3
  maxTokens: number;         // padrão: 8192 (currículo) / 4096 (análise)
  timeoutMs: number;         // padrão: 90000
  model?: string;            // override do modelo padrão
  responseFormat?: "json"; // forçar resposta JSON
  seed?: number;             // semente para reprodutibilidade (opcional)
}
```

### Contrato de `HealthResult`

```typescript
interface HealthResult {
  provider: string;
  available: boolean;
  latencyMs: number;
  errorMessage?: string;
  currentLoad?: number; // 0.0 - 1.0
}
```

---

## 7. Estrutura `GenerationResult`

```typescript
interface GenerationResult {
  // Status da operação
  success: boolean;

  // Resposta original do provedor (string)
  rawResponse: string;

  // Resposta parseada como objeto (null se parse falhou)
  parsedResponse: OutputSchema | null;

  // Identificação
  provider: string;    // ex: "openai" | "anthropic" | "google"
  model: string;       // ex: "gpt-4o" | "claude-sonnet-4-6"

  // Métricas de uso
  tokensUsed: {
    input: number;
    output: number;
    total: number;
  };

  // Custo estimado em USD
  costEstimate: number; // decimal, até 6 casas decimais

  // Duração total da chamada
  durationMs: number;   // long integer

  // Em caso de falha
  errorMessage: string; // string vazia se success === true
  errorCode: string; // ex: "RATE_LIMITED" | "TIMEOUT" | "INVALID_OUTPUT"
}
```

---

## 8. System Prompt (Template Fixo)

Este prompt é enviado **em toda chamada** ao provedor de IA, sem exceção. Deve ser mantido em `config/prompts/system-prompt.md` e versionado junto com o código.

```
# SYSTEM PROMPT — Resume Forge AI Agent

## Papel
Você é um especialista em recrutamento eRH com mais de 15 anos de experiência em triagem de currículos, otimização de conteúdo para ATS (Applicant Tracking Systems) e análise de aderência entre perfil de candidato e requisitos de vaga.

## Regras Absolutas (obedecer SEMPRE)

1. **Nunca invente dados.** Todas as informações do currículo devem vir exclusivamente dos dados fornecidos pelo usuário. Não acrescente nomes de empresas, certificações, competências ou datas que não estejam presentes nos dados de entrada.
2. **Nunca exagere a aderência.** Se o candidato não possui um requisito-chave da vaga, marque-o como lacuna. Score acima de 85 significa que pelo menos 80% dos requisitos estão explicitamente cobertos.
3. **Nunca inclua informações pessoais sensíveis** que não estejam nos dados de entrada: endereço completo, estado civil, religião, origem étnica, deficiência não declarada.
4. **Nunca copie textos da descrição da vaga.** Parágrafos inteiros da vaga não devem aparecer no currículo. Use suas próprias palavras para descrever as competências.
5. **Nunca modifique dados de contato.** Nome, e-mail, telefone e links de perfil são preservados exatamente como fornecidos.
6. **Nunca gere conteúdo discriminatório, publicitário ou malicioso.**

## Diretrizes de Estilo

- **Tom**: profissional, objetivo, factual. Sem jargões vazios ou superlativos.
- **Formato de saída**: JSON válido conforme o schema de output fornecido.
- **Idioma**: o currículo deve ser escrito no mesmo idioma da descrição da vaga (PT-BR, EN-US ou ES).
- **Extensão**: o markdown do currículo não deve exceder 8000 caracteres (aproximadamente 2 páginas DOCX).
- **Seções obrigatórias**: header (nome + contatos), professional_summary, experience, education, skills.
- **Seções opcionais**: certifications, projects, languages.
- **Seções proibidas**: hobbies, interests, personal philosophy, referências pessoais com nome completo.

## Diretrizes de Análise de Aderência

Para cada requisito da vaga:
- `matched`: o requisito está explicitamente presente nos dados do currículo.
- `partial`: o requisito está implicitamente presente (ex.: menciona "Docker" em vez de "containerization" mas não "Docker" explicitamente).
- `missing`: o requisito não está presente de forma alguma.

Para lacunas:
- Seja específico: descreva exatamente qual é a lacuna e sugira uma ação concreta para o candidato remediá-la.
- Classifique a severidade: `high` (impeditivo para a contratação), `medium` (diferencial competitivo), `low` (bom ter mas não crítico).

## Diretrizes de Reescrita do Currículo

- **Professional Summary**: 2 a 4 frases que posicionam o candidato para a vaga específica. Mencione os pontos mais relevantes para o cargo.
- **Experience**: para cada posição, use o formato "Cargo @ Empresa (período)". Na descrição, priorizeachievements e resultados mensuráveis. Use verbos de ação (liderou, desenvolveu, implementou, reduziu).
- **Skills**: agrupe por categoria (ex.: Languages& Frameworks, Cloud & DevOps, Databases). Ordene por relevância para a vaga.
- **Education**: mantenha apenas formação superior e pós-graduações. Técnicos e cursos livres vão para skills ou projects.
- **Experiências antigas (> 10 anos)**: condense em uma linha de resumo, a menos que sejam diretamente relevantes para a vaga.

## Formato de Saída

Retorne APENAS um objeto JSON válido. Não inclua markdown, não inclua texto explicativo fora do JSON. O JSON deve seguir exatamente o schema de output fornecido no prompt do usuário.
```

---

## 9. User Prompt (Template Dinâmico)

Este template é preenchido pelo sistema a cada chamada. Placeholders entre chaves `{}` são substituídos por valores reais.

```
## Dados do Candidato (Currículo Base)

Os dados abaixo foram fornecidos pelo candidato. Use-os EXCLUSIVAMENTE como fonte de fatos. Não invente, não interprete erroneamente, não omita informações relevantes.

```json
{resume_data_anonymized}
```

## Descrição da Vaga

```yaml
Título da Vaga: {job_title}
Idioma da Vaga: {language}
Tom Preferido: {tone_guidance}
```

```text
{job_description}
```

## Requisitos e Preferências Extraídos

**Requisitos (obrigatórios para score alto):**
{requirements_list}

**Preferências (diferenciais competitivos):**
{preferences_list}

## Instruções Específicas

1. Gere o currículo otimizado em `{language}`.
2. Use o tom `{tone_guidance}`.
3. Para vagas júnior, dê peso maior a projetos acadêmicos e competências técnicas básicas.
4. Para vagas sênior, destaque liderança técnica, arquitetura e impacto nos negócios.
5. Remova experiências anteriores a 10 anos que não sejam diretamente relevantes para a vaga.
6. Preserve EXATAMENTE os dados de contato (nome, e-mail, telefone, LinkedIn, GitHub) conforme fornecidos.

## Schema de Output

Gere a resposta no seguinte formato JSON. Todos os campos são obrigatórios exceto onde indicado como opcional.

```json
{
  "adherence_analysis": {
    "score": 0-100,
    "matched_requirements": [...],
    "unmatched_requirements": [...],
    "gaps": [...],
    "strengths": [...]
  },
  "optimized_resume": {
    "markdown": "string markdown completo",
    "sections": { ... },
    "metadata": { ... }
  },
  "notes": {
    "inferences": [...],
    "warnings": [...],
    "generation_metadata": { ... }
  }
}
```

## Output

Retorne APENAS o objeto JSON. Sem preâmbulo, sem pós-âmbulo, sem markdown.
```

### Placeholders Preenchidos

| Placeholder | Fonte | Exemplo |
|---|---|---|
| `{resume_data_anonymized}` | `resume_data` com PII removido (ver Seção 11) | JSON sem `email`, `phone`, `linkedin`, `github` |
| `{job_title}` | `job_applications.job_title` | "Engenheiro de Dados Sênior" |
| `{language}` | Extraído da `job_description` | "PT-BR" |
| `{tone_guidance}` | Preferência do usuário ou padrão do plano | "professional" |
| `{job_description}` | `job_applications.job_description` | Texto completo da vaga |
| `{requirements_list}` | Extraído via regex/NER da descrição | Lista de requisitos como bullet points |
| `{preferences_list}` | Extraído via regex/NER da descrição | Lista de preferências como bullet points |

---

## 10. Parâmetros de Geração

| Parâmetro | Valor para Análise | Valor para Currículo Otimizado | Justificativa |
|---|---|---|---|
| **Temperatura** | 0.3 | 0.3 | Balanço entre coerência e variabilidade. 0.3 evita repetição sem sacrificar naturalidade. |
| **Max Tokens** | 4096 | 8192 | Currículo otimizado é mais longo (texto + JSON). Análise cabe em 4096. |
| **Timeout** | 90.000 ms | 90.000 ms | Worker timeout configurado no cliente HTTP. Retry com 120.000 ms na primeira tentativa. |
| **Top P** | 1.0 (default) | 1.0 (default) | Não alterar — temperatura já controla a distribuição. |
| **Frequency Penalty** | 0.0 | 0.0 | Não penalizar repetição dentro do currículo — pode ser desejável para ênfase. |
| **Presence Penalty** | 0.0 | 0.0 | Não penalizar — currículos profissionais se beneficiam de repetição estratégica de competências. |

### Mapeamento por Provedor

| Provedor | Modelo Primário | Modelo Fallback | Parâmetros Extras |
|---|---|---|---|
| OpenAI | `gpt-4o` | `gpt-4o-mini` | `response_format: { type: "json_object" }` |
| Anthropic | `claude-sonnet-4-6` | `claude-haiku-4-6` | `automatic_json_mode: true` |
| Google AI | `gemini-2.0-flash` | `gemini-1.5-flash` | `response_mime_type: "application/json"` |
| Azure OpenAI | deployment `gpt-4o` | deployment `gpt-4o-mini` | `response_format: { type: "json_object" }` |

---

## 11. Minimização de PII

### 11.1 O que é Enviado ao Provedor de IA

**NUNCA enviar:**
- `resume_data.email`
- `resume_data.phone`
- `resume_data.linkedin` (URL completa)
- `resume_data.github` (URL completa)
- `resume_data.full_name` (enviar apenas se o provedor precisar para contexto de geração — opcional)
- `resume_data.address` (enviar apenas `location`, nunca endereço completo)

**ENVIAR (anonimizado quando aplicável):**
- `resume_data.professional_summary`
- `resume_data.experience` (empresa, cargo, datas, descrição — **sem** nomes de contato interno)
- `resume_data.education`
- `resume_data.skills`
- `resume_data.certifications`
- `resume_data.languages`
- `resume_data.projects`
- `resume_data.location` (cidade/estado, não endereço completo)

### 11.2 O que é Reconstruído Localmente Após Geração

Após o provedor retornar o `optimized_resume.markdown`, o sistema **injeta localmente** a seção de header:

```typescript
function reconstructHeader(resumeData: ResumeData, generatedMarkdown: string): string {
  const header = [
    `# ${resumeData.full_name}`,
    `${resumeData.phone} | ${resumeData.email} | ${resumeData.location}`,
    `${resumeData.linkedin} | ${resumeData.github}`
  ].join('\n');

  // Substituir placeholder {HEADER} no markdown pelo header real
  return generatedMarkdown.replace('{HEADER}', header);
}
```

O placeholder `{HEADER}` é inserido no System Prompt como instrução para o provedor gerar o markdown **sem** a seção de contato, substituindo-a por `{HEADER}`.

### 11.3 Justificativa LGPD (Lei Geral de Proteção de Dados — Brasil)

| Dado | Classificação LGPD | Tratamento |
|---|---|---|
| E-mail | Dado pessoal (art. 5º, I) | Não enviado ao provedor; processado localmente |
| Telefone | Dado pessoal (art. 5º, I) | Não enviado ao provedor; processado localmente |
| LinkedIn/GitHub | Dado pessoal identificável | Não enviado ao provedor; reconstruído localmente |
| Nome completo | Dado pessoal (art. 5º, I) | Enviado apenas se necessário para contexto; anonimizado quando possível |
| Endereço | Dado pessoal sensível (art. 5º, II) | Apenas cidade/estado é enviado; endereço completo removido |
| Experiências profissionais | Dado pessoal (art. 5º, I) | Enviado ao provedor como parte do currículo; o provedor é subprocessador com DPA vigente |

**Base legal**: o envio dos dados ao provedor de IA para geração do currículo otimizado é realizado sob **consentimento explícito** do titular (art. 7º, I, LGPD) e **necessidade de prestação de serviço** (art. 7º, VI). O provedor de IA atua como **suboperador** nos termos do art. 33 da LGPD, com contrato de processamento de dados (DPA) vigente.

**Retenção**: o provedor de IA não retém os dados após a resposta ser retornada. A política de retenção do provedor deve ser verificada e documentada no DPA.

---

## 12. Rate Limiting

### 12.1 Limites por Usuário

| Plano | Máximo Chamadas Simultâneas | Máximo Chamadas por Minuto | Máximo por Dia |
|---|---|---|---|
| Free | 2 | 5 | 20 |
| Pro | 5 | 15 | 100 |
| Enterprise | 10 | 50 | 500 |

### 12.2 Limites Globais do Sistema

| Recurso | Limite | Ação ao Atingir |
|---|---|---|
| Total de chamadas simultâneas ao provedor | 50 | Fila de espera (max30s) |
| Total de tokens por minuto (todos usuários) | 1.000.000 | Rate limit global429 |
| Custo total por hora (todos usuários) | $50 USD | Bloqueio temporário |

### 12.3 Backoff Exponencial (Erro 429)

```
tentativa 1: esperar 5s   → retry
tentativa 2: esperar 15s  → retry
tentativa 3: esperar 45s  → retry
tentativa 4: marcar `ai_runs.status = 'failed'`, codigo `AI_RATE_LIMITED`
```

- O backoff é aplicado **por provedor**, não por chamada global.
- Se o provedor primário está em rate limit, o sistema tenta fallback de provedor imediatamente (sem esperar).
- O jitter é aplicado: `wait_ms = base_wait * (1 + random(0, 0.3))`.

### 12.4 Concorrência no MVP

O MVP não usa fila de jobs nem worker assíncrono. A geração ocorre no request HTTP do endpoint `POST /api/generate`.

Regras:

- Limitar chamadas concorrentes por usuário na aplicação.
- Retornar `429 rate_limited` quando o usuário exceder o limite configurado.
- Usar retry curto apenas para erros temporários do provedor (`429`, `5xx`, timeout).
- Se houver fallback provider configurado, tentar o fallback uma vez antes de retornar erro ao frontend.

### 12.5 Implementação de Rate Limiting

```typescript
interface RateLimitConfig {
  userId: string;
  plan: "FREE" | "PRO" | "ENTERPRISE";
  maxConcurrent: number;
  maxPerMinute: number;
  maxPerDay: number;
}

// Redis-backed sliding window rate limiter
async function checkRateLimit(userId: string): Promise<{
  allowed: boolean;
  remaining: number;
  retryAfterMs: number;
}> {
  const config = await getRateLimitConfig(userId);
  const now = Date.now();

  // Sliding window: últimas 60 segundos
  const windowStart = now - 60_000;
  await redis.zremrangebyscore(`ratelimit:${userId}:minute`, 0, windowStart);
  const recentCalls = await redis.zcard(`ratelimit:${userId}:minute`);

  if (recentCalls >= config.maxPerMinute) {
    const oldestCall = await redis.zrange(`ratelimit:${userId}:minute`, 0, 0, 'WITHSCORES');
    const retryAfterMs = Math.max(0, parseInt(oldestCall[1]) + 60_000 - now);
    return { allowed: false, remaining: 0, retryAfterMs };
  }

  await redis.zadd(`ratelimit:${userId}:minute`, now, `${now}:${uuid}`);
  await redis.expire(`ratelimit:${userId}:minute`, 120);
  return { allowed: true, remaining: config.maxPerMinute - recentCalls - 1, retryAfterMs: 0 };
}
```

---

## Apêndice A — Enum de Gaps

```typescript
enum GapType {
  MISSING_SKILL = "MISSING_SKILL",
  MISSING_CERTIFICATION = "MISSING_CERTIFICATION",
  MISSING_EXPERIENCE = "MISSING_EXPERIENCE",
  MISSING_EDUCATION = "MISSING_EDUCATION",
  LANGUAGE_GAP = "LANGUAGE_GAP",
  EXPERIENCE_DOMAIN_GAP = "EXPERIENCE_DOMAIN_GAP",
  UNKNOWN = "UNKNOWN"
}
```

## Apêndice B — Enum de Severidade

```typescript
enum GapSeverity {
  HIGH = "high",    // Impeditivo para contratação
  MEDIUM = "medium", // Diferencial competitivo
  LOW = "low"       // Bom ter, não crítico
}
```

## Apêndice C — Enum de Status de Requisito

```typescript
enum RequirementStatus {
  MATCHED = "matched", // Presente explicitamente
  PARTIAL = "partial",     // Presente implicitamente
  MISSING = "missing"      // Não presente
}
```

---

*Este documento é uma especificação de implementação. Qualquer alteração deve ser refletida aqui antes da codificação.*
