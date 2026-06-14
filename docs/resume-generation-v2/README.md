# Resume Generation V2 - Documentacao Tecnica

## Indice

1. [Causa Raiz](#causa-raiz)
2. [Schema da Resposta da IA](#schema-da-resposta-da-ia)
3. [Fonte Unica da Verdade](#fonte-unica-da-verdade)
4. [Fluxo](#fluxo)
5. [Validacao de Evidencias](#validacao-de-evidencias)
6. [Classificacao de Score](#classificacao-de-score)
7. [Estilos DOCX](#estilos-docx)
8. [Testes](#testes)
9. [Riscos Residuais](#riscos-residuais)

---

## Causa Raiz

### Problema Atual

A arquitetura atual do sistema de geracao de curriculos apresenta as seguintes deficiencias:

1. **Frontend exibe Markdown cru em `<pre>`**
   - O preview do curriculo renderiza o conteudo como texto pre-formatado
   - Ausencia de componentes React estruturados para cada secao
   - Impossibilidade de estilizar individualmente cada parte do curriculo
   - Experiencia do usuario comprometida pela falta de formatacao visual

2. **DOCX usa conversao de Markdown**
   - A geracao do documento Word depende de conversao de Markdown para HTML/DOCX
   - Conversoes imperfeitas causam perda de formatacao
   - Falta de controle granular sobre estilos especificos do Word
   - Inconsistencia entre o que e visualizado e o que e exportado

3. **Falta de estrutura usada como fonte da verdade**
   - Markdown e tratado como verdade, não a estrutura de dados
   - Duas fontes de verdade diferentes geram sincronizacao de estado
   - Dificuldade em manter consistencia entre preview e exportacao
   - Impossibilidade de edicao granular por secao

### Solucao Proposta

Adotar uma arquitetura onde a estrutura JSON seja a fonte unica da verdade, com geracao derivativa de Markdown e DOCX a partir dessa estrutura.

---

## Schema da Resposta da IA

### Estrutura Completa do JSON

```json
{
  "optimized_resume": {
    "professional_title": "string",
    "professional_summary": "string",
    "skills": [
      {
        "category": "string",
        "items": ["string"]
      }
    ],
    "experience": [
      {
        "company": "string",
        "official_role": "string",
        "location": "string",
        "start_date": "string",
        "end_date": "string",
        "highlights": ["string"]
      }
    ],
    "previous_experience_summary": ["string"],
    "projects": [
      {
        "name": "string",
        "description": "string",
        "technologies": ["string"]
      }
    ],
    "education": [
      {
        "institution": "string",
        "degree": "string",
        "period": "string"
      }
    ],
    "certifications": [
      {
        "name": "string",
        "issuer": "string",
        "date": "string"
      }
    ],
    "trainings": [
      {
        "name": "string",
        "issuer": "string",
        "date": "string"
      }
    ],
    "languages": [
      {
        "language": "string",
        "level": "string"
      }
    ]
  },
  "adherence_analysis": {
    "score": 0-100,
    "score_before_limiters": 0-100,
    "competitiveness": "baixa|moderada|boa|alta",
    "justification": "string",
    "requirements": [
      {
        "requirement": "string",
        "category": "mandatory|important|desirable",
        "weight": 1-5,
        "state": "matched|partial|missing",
        "evidence": "string",
        "points": 0-100,
        "severity": "high|medium|low"
      }
    ],
    "gaps": [
      {
        "gap": "string",
        "severity": "high|medium|low",
        "impact": "string",
        "recommended_action": "string"
      }
    ],
    "strengths": ["string"],
    "keyword_analysis": {
      "matched": ["string"],
      "missing": ["string"]
    },
    "warnings": ["string"]
  }
}
```

### Descricao dos Campos

#### optimized_resume

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `professional_title` | string | Titulo profissional otimizado para a vaga |
| `professional_summary` | string | Resumo profissional destacado |
| `skills` | array | Habilidades agrupadas por categoria |
| `experience` | array | Experiencias profissionais |
| `previous_experience_summary` | array | Resumo das experiencias anteriores |
| `projects` | array | Projetos relevantes |
| `education` | array | Formacao academica |
| `certifications` | array | Certificacoes profissionais |
| `trainings` | array | Treinamentos relevantes |
| `languages` | array | Idiomas e niveis |

#### adherence_analysis

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `score` | number (0-100) | Score final considerando limitadores |
| `score_before_limiters` | number (0-100) | Score antes de aplicar limitadores |
| `competitiveness` | enum | Nivel de competitividade |
| `justification` | string | Justificativa do score |
| `requirements` | array | Análise de cada requisito |
| `gaps` | array | Lacunas identificadas |
| `strengths` | array | Pontos fortes |
| `keyword_analysis` | object | Analise de palavras-chave |
| `warnings` | array | Avisos importantes |

---

## Fonte Unica da Verdade

### Principio Central

**A estrutura `optimized_resume` e a fonte unica da verdade.**

### Hierarquia de Dados

```
optimized_resume (FONTE)
    |
    +---> markdown (derivado)
    +---> componentes React (derivado)
    +---> DOCX (derivado)
```

### Derivações

1. **Markdown**: Gerado a partir da estrutura JSON
   - Cada secao e convertida para formatacao Markdown
   - Mantem consistencia entre estrutura e renderizacao

2. **Preview React**: Renderiza componentes a partir da estrutura
   - Componentes individuais por secao
   - Estilizacao via CSS/UI framework
   - Interatividade baseada na estrutura

3. **DOCX**: Usa StructuredDocxConverter
   - Consome a estrutura JSON diretamente
   - Aplica estilos pre-definidos
   - Garante fidelidade ao conteudo

### Vantagens da Arquitetura

- **Consistencia**: Uma unica fonte de verdade
- **Manutenibilidade**: Alteracoes em um lugar refletem em todos
- **Testabilidade**: Facilidade em testar cada conversao
- **Extensibilidade**: Facil adicao de novos formatos de saida

---

## Fluxo

### Pipeline de Geracao

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FLUXO COMPLETO                               │
└─────────────────────────────────────────────────────────────────────┘

     ┌──────────┐
     │   IA     │
     │ (Claude) │
     └────┬─────┘
          │
          │ 1. Retorna JSON estruturado
          │
          ▼
┌──────────────────────┐
│     BACKEND          │
│  GenerationService   │
└──────────┬───────────┘
           │
           │ 2. Valida e persiste estrutura
           │
           ▼
┌──────────────────────┐     ┌──────────────────────┐
│      PERSISTENCIA    │     │   DERIVACOES         │
│  GeneratedResume     │────►│   (da estrutura)     │
│  (JSON completo)     │     │                      │
└──────────────────────┘     │   - markdown         │
                             │   - preview React    │
                             │   - DOCX             │
                             └──────────────────────┘
                                   │
                                   ▼
                             ┌──────────────┐
                             │   FRONTEND   │
                             │  ResumeView  │
                             └──────────────┘
```

### Detalhamento do Fluxo

#### Etapa 1: Resposta da IA
```
Input:  Curriculo original + Descricao da vaga
Output: JSON com optimized_resume + adherence_analysis
```

#### Etapa 2: Backend - Validacao e Persistencia
```
Input:  JSON da IA
Process:
  1. Validar schema JSON
  2. Validar evidencias conforme regras
  3. Persistir em GeneratedResume
  4. Armazenar dados estruturados
Output: Estrutura validada e persistida
```

#### Etapa 3: Conversao para Formatos
```
Estrutura JSON
    │
    ├──► MarkdownGenerator ---> arquivo .md
    │
    ├──► ReactComponents ---> Preview visual
    │
    └──► StructuredDocxConverter ---> documento .docx
```

#### Etapa 4: Frontend - Renderizacao
```
Input:  Estrutura JSON da API
Process:
  1. Buscar dados via API
  2. Renderizar componentes React
  3. Exibir preview formatado
Output: Visualizacao do curriculo otimizado
```

---

## Validacao de Evidencias

### Regras por Categoria

#### Regras para Kubernetes

| Evidencia | Validacao | Pontos |
|-----------|-----------|--------|
| Kubernetes (K8s) | Presenca de "kubernetes" ou "k8s" | 20 |
| Helm Charts | "helm" ou "helm charts" | 15 |
| Kustomize | "kustomize" | 10 |
| kubectl | "kubectl" | 5 |
| RBAC | "rbac" ou "role-based" | 10 |
| CNCF | "cncf" ou certificacao | 15 |
| EKS/AKS/GKE | "eks" ou "aks" ou "gke" | 15 |

#### Regras para Cloud

| Evidencia | Validacao | Pontos |
|-----------|-----------|--------|
| AWS | "aws" ou "amazon web services" | 20 |
| Azure | "azure" | 20 |
| GCP | "gcp" ou "google cloud" | 20 |
| Terraform | "terraform" | 15 |
| CloudFormation | "cloudformation" | 15 |
| Serverless | "lambda" ou "serverless" ou "azure functions" | 15 |
| Multi-cloud | Mencoes de 2+ provedores | 10 (bonus) |

#### Regras para Lideranca

| Evidencia | Validacao | Pontos |
|-----------|-----------|--------|
| Times | Tamanho do time > 3 | 15 |
| Mentor | "mentor" ou "mentoria" | 15 |
| Arquiteto | "arquiteto" ou "architecture" | 20 |
| Tech Lead | "tech lead" ou "tech-lead" | 20 |
| SRE | "sre" ou "site reliability" | 15 |
| Agile/Scrum | "agile" ou "scrum" ou "kanban" | 10 |

#### Regras para Plantao

| Evidencia | Validacao | Pontos |
|-----------|-----------|--------|
| On-call | "on-call" ou "plantao" | 15 |
| Incident Response | "incident response" ou "incidentes" | 15 |
| Postmortem | "postmortem" ou "rca" | 10 |
| SLA/SLO | "sla" ou "slo" ou "sls" | 10 |
| Alerting | "alerting" ou "monitoramento" | 10 |

#### Regras para Observabilidade

| Evidencia | Validacao | Pontos |
|-----------|-----------|--------|
| Prometheus | "prometheus" | 15 |
| Grafana | "grafana" | 15 |
| ELK/EFK | "elasticsearch" ou "elk" ou "efk" | 15 |
| Datadog | "datadog" | 15 |
| APM | "apm" ou "new relic" ou "dynatrace" | 15 |
| Logging estruturado | "structured logging" ou "json logs" | 10 |
| Tracing | "tracing" ou "jaeger" ou "zipkin" | 15 |

### Implementacao da Validacao

```java
// Exemplo de validacao de evidencias
public class EvidenceValidator {
    
    public ValidationResult validate(Map<String, Object> optimizedResume) {
        List<RequirementValidation> results = new ArrayList<>();
        
        // Kubernetes
        results.add(validateCategory("kubernetes", 
            List.of("kubernetes", "k8s"),
            Map.of(
                "helm", 15,
                "kustomize", 10,
                "kubectl", 5,
                "rbac", 10,
                "eks", 15,
                "aks", 15,
                "gke", 15
            )
        ));
        
        // Cloud
        results.add(validateCloudProviders(optimizedResume));
        
        // ... outras validacoes
        
        return new ValidationResult(results);
    }
}
```

---

## Classificacao de Score

### Tabela de Classificacao

| Range | Classificacao | Descricao | Acao Recomendada |
|-------|---------------|-----------|------------------|
| 0-30 | **Baixa aderencia** | O curriculo nao atende bem a vaga | Revisar e adicionar competencias |
| 31-50 | **Aderencia moderada** | Atende parcialmente a vaga | Melhorar pontos fracos |
| 51-70 | **Boa aderencia** | Atende bem a vaga | Focar em destaques |
| 71-100 | **Alta aderencia** | Excelente alinhamento | Pronto para aplicacao |

### Logica de Calculo

```java
public class ScoreCalculator {
    
    public int calculateScore(AdherenceAnalysis analysis) {
        double weightedSum = 0;
        int totalWeight = 0;
        
        for (Requirement req : analysis.getRequirements()) {
            int weight = req.getWeight();
            int points = req.getPoints();
            
            // Multiplicadores por categoria
            switch (req.getCategory()) {
                case "mandatory":
                    weight *= 3;
                    break;
                case "important":
                    weight *= 2;
                    break;
                case "desirable":
                    weight *= 1;
                    break;
            }
            
            weightedSum += weight * points;
            totalWeight += weight;
        }
        
        double baseScore = weightedSum / totalWeight;
        
        // Aplicar limitadores
        return applyLimiters(baseScore, analysis);
    }
    
    private int applyLimiters(double baseScore, AdherenceAnalysis analysis) {
        double finalScore = baseScore;
        
        // Limiter 1: Requisitos obrigatorios nao atendidos
        boolean hasMissingMandatory = analysis.getRequirements().stream()
            .anyMatch(r -> r.getCategory().equals("mandatory") 
                         && r.getState().equals("missing"));
        
        if (hasMissingMandatory) {
            finalScore *= 0.7; // Reducao de 30%
        }
        
        // Limiter 2: Gap critico identificado
        boolean hasCriticalGap = analysis.getGaps().stream()
            .anyMatch(g -> g.getSeverity().equals("high"));
        
        if (hasCriticalGap) {
            finalScore *= 0.85;
        }
        
        return (int) Math.round(finalScore);
    }
}
```

### Visualizacao do Score

```
Score: 73/100

┌─────────────────────────────────────────────────────────┐
│  ████████████████████████████████████████████████░░░░░  │
│  0                                              100     │
│  └───┬─────┬───────┬──────────┬────────────────────────►
│     30    50      70         100                      
│     │      │       │          │                       
│  Baixa  Moderada  Boa       Alta                       
│                     ▲                                  
│                  Atual                               
└─────────────────────────────────────────────────────────┘
```

---

## Estilos DOCX

### Especificacoes de Fonte

| Elemento | Fonte | Tamanho | Estilo |
|----------|-------|---------|--------|
| Nome/Titulo | Calibri | 16pt | Bold |
| Subtitulos secao | Calibri | 12pt | Bold, Maiusculas |
| Corpo | Calibri | 11pt | Normal |
| Experiencias | Calibri | 11pt | Normal |
| Listas | Calibri | 10pt | Normal |

### Margens do Documento

```xml
<w:pgMar 
    w:top="1440"    <!-- 1 polegada -->
    w:right="1440"  <!-- 1 polegada -->
    w:bottom="1440" <!-- 1 polegada -->
    w:left="1440"   <!-- 1 polegada -->
    w:header="720"  <!-- 0.5 polegada -->
    w:footer="720"  <!-- 0.5 polegada -->
    w:gutter="0"/>
```

### Hierarquia de Titulos

| Nivel | Estilo | Formatacao |
|-------|--------|------------|
| H1 | Titulo Principal | Calibri 16pt Bold, espacamento antes 0, depois 6pt |
| H2 | Nome da Secao | Calibri 12pt Bold, todas maiusculas, borda inferior |
| H3 | Subtitulo | Calibri 11pt Bold Italic |
| H4 | Sub-subtitulo | Calibri 11pt Italic |

### Espacamento

| Elemento | Espacamento Anterior | Espacamento Posterior | Linha |
|----------|---------------------|----------------------|-------|
| Secao | 12pt | 6pt | Simple |
| Item | 6pt | 3pt | Simple |
| Paragrafo | 0pt | 6pt | 1.15 |
| Lista | 0pt | 3pt | 1.0 |

### Listas Reais

O documento usa listas nativas do Word, nao caracteres especiais:

```xml
<w:numPr>
    <w:ilvl w:val="0"/>
    <w:numId w:val="1"/>
</w:numPr>
```

#### Configuracao de Numeracao

```xml
<w:numbering>
    <w:abstractNum w:abstractNumId="1">
        <w:multiLevelType w:val="hybridMultilevel"/>
        <w:lvl w:ilvl="0">
            <w:start w:val="1"/>
            <w:numFmt w:val="bullet"/>
            <w:lvlText w:val="•"/>
            <w:lvlJc w:val="left"/>
            <w:pPr>
                <w:ind w:left="720" w:hanging="360"/>
            </w:pPr>
        </w:lvl>
    </w:abstractNum>
</w:numbering>
```

### Cores

| Elemento | Cor | Hex |
|----------|-----|-----|
| Titulo principal | Azul escuro | #1F4E79 |
| Nomes de secao | Cinza escuro | #404040 |
| Links | Azul | #0563C1 |
| Destacados | Negrito | - |

---

## Testes

### Backend - Testes Unitarios

#### Validacao de Schema

```java
@Test
void shouldRejectInvalidSchema() {
    String invalidJson = "{ \"optimized_resume\": { \"skills\": null } }";
    
    assertThrows(ValidationException.class, () -> 
        schemaValidator.validate(invalidJson)
    );
}

@Test
void shouldAcceptValidSchema() {
    String validJson = getValidOptimizedResumeJson();
    
    assertDoesNotThrow(() -> schemaValidator.validate(validJson));
}
```

#### Validacao de Evidencias

```java
@Test
void shouldScoreKubernetesExpert() {
    OptimizedResume resume = createResumeWithKubernetes();
    AdherenceAnalysis analysis = validator.validate(resume);
    
    assertThat(analysis.getKeywordAnalysis().getMatched())
        .contains("kubernetes", "eks", "helm");
}

@Test
void shouldIdentifyMissingMandatorySkills() {
    OptimizedResume resume = createResumeWithoutMandatorySkills();
    AdherenceAnalysis analysis = validator.validate(resume);
    
    List<Requirement> missing = analysis.getRequirements().stream()
        .filter(r -> r.getState().equals("missing"))
        .collect(Collectors.toList());
    
    assertThat(missing).isNotEmpty();
}
```

#### Calculo de Score

```java
@Test
void shouldCalculateScoreCorrectly() {
    AdherenceAnalysis analysis = createFullAnalysis();
    
    int score = scoreCalculator.calculateScore(analysis);
    
    assertThat(score).isBetween(0, 100);
}

@Test
void shouldApplyMandatoryLimiter() {
    AdherenceAnalysis analysis = createAnalysisWithMissingMandatory();
    
    int scoreWithLimiter = scoreCalculator.calculateScore(analysis);
    int scoreBeforeLimiter = analysis.getScoreBeforeLimiters();
    
    assertThat(scoreWithLimiter).isLessThan(scoreBeforeLimiter);
}
```

### Frontend - Snapshot Tests

```typescript
describe('ResumePreview', () => {
  it('should render complete resume structure', () => {
    const resume = mockOptimizedResume();
    
    const { container } = render(<ResumePreview data={resume} />);
    
    expect(container).toMatchSnapshot();
  });
  
  it('should render without professional summary', () => {
    const resume = mockOptimizedResumeWithoutSummary();
    
    const { container } = render(<ResumePreview data={resume} />);
    
    expect(container).toMatchSnapshot();
  });
  
  it('should highlight adherence score correctly', () => {
    const resume = mockOptimizedResume();
    
    render(<AdherenceScore score={75} />);
    
    expect(screen.getByText('75')).toHaveClass('score-high');
  });
});
```

### DOCX - Validacao de Estrutura

```java
@Test
void shouldGenerateValidDocxStructure() {
    OptimizedResume resume = createFullResume();
    byte[] docx = docxConverter.convert(resume);
    
    assertThat(docx).isNotEmpty();
    assertThat(isValidZip(docx)).isTrue();
}

@Test
void shouldContainAllSections() {
    OptimizedResume resume = createFullResume();
    DocxContent content = docxConverter.extractContent(docx);
    
    assertThat(content.getSections())
        .containsExactlyInAnyOrder(
            "professional_title",
            "professional_summary", 
            "skills",
            "experience",
            "education",
            "certifications"
        );
}

@Test
void shouldApplyCorrectStyles() {
    OptimizedResume resume = createFullResume();
    DocxContent content = docxConverter.extractContent(docx);
    
    Style titleStyle = content.getStyle("Heading1");
    assertThat(titleStyle.getFontName()).isEqualTo("Calibri");
    assertThat(titleStyle.getFontSize()).isEqualTo(16);
    assertThat(titleStyle.isBold()).isTrue();
}
```

---

## Riscos Residuais

### Risco 1: IA pode nao retornar estrutura

**Probabilidade**: Media  
**Impacto**: Alto

#### Cenario
O modelo de IA pode retornar uma resposta que nao segue o schema esperado, seja por:
- Formato incorreto (Markdown ao inves de JSON)
- Schema incompleto
- Campos ausentes
- JSON mal formado

#### Mitigacoes

1. **Prompt Engineering**
   - Incluir exemplos do schema no prompt
   - Usar instrucoes explícitas sobre formato
   - Implementar few-shot learning

2. **Validacao Robusta no Backend**
   ```java
   public GenerationResult processAIResponse(String rawResponse) {
       try {
           // Tentar parsear como JSON
           JsonNode json = objectMapper.readTree(rawResponse);
           
           // Validar schema
           if (!schemaValidator.isValid(json)) {
               return handleSchemaValidationFailure(json);
           }
           
           return GenerationResult.success(json);
           
       } catch (JsonProcessingException e) {
           return handleMalformedJson(rawResponse, e);
       }
   }
   ```

3. **Fallback para Markdown**
   - Implementar conversao de Markdown para estrutura
   - Manter compatibilidade com formato antigo
   - Registrar ocorrencia para melhoria do prompt

### Risco 2: Fallback para Markdown

**Probabilidade**: Baixa (com mitigacoes)  
**Impacto**: Medio

#### Cenario
Quando a IA nao retorna JSON valido, o sistema pode precisar usar o fallback de Markdown.

#### Mitigacoes

1. **MarkdownParser Avancado**
   ```java
   public OptimizedResume parseMarkdownFallback(String markdown) {
       OptimizedResume resume = new OptimizedResume();
       
       // Parser robust que extrai estrutura de Markdown
       resume.setProfessionalTitle(extractTitle(markdown));
       resume.setProfessionalSummary(extractSummary(markdown));
       resume.setExperience(extractExperience(markdown));
       resume.setSkills(extractSkills(markdown));
       
       return resume;
   }
   ```

2. **Flag de Qualidade**
   - Marcar curriculos gerados via fallback
   - Exibir aviso no frontend
   - Permitir re-geracao sob demanda

3. **Log e Monitoramento**
   ```java
   log.warn("Using markdown fallback for resume generation. " +
            "AI response did not match expected schema. " +
            "CorrelationId: {}", correlationId);
   ```

### Risco 3: Inconsistencia entre Derivacoes

**Probabilidade**: Baixa  
**Impacto**: Medio

#### Cenario
As derivacoes (Markdown, React, DOCX) podem divergir da estrutura original.

#### Mitigacoes

1. **Testes de Integracao**
   ```java
   @Test
   void shouldMaintainConsistencyAcrossDerivations() {
       OptimizedResume original = createResume();
       
       String markdown = markdownGenerator.generate(original);
       OptimizedResume fromMarkdown = markdownParser.parse(markdown);
       
       assertThat(fromMarkdown).isEqualTo(original);
   }
   ```

2. **Validacao Cruzada**
   - Frontend valida estrutura ao receber da API
   - DOCX verifica consistencia antes de gerar
   - Log de discrepancias

---

## Apendices

### A. Exemplo de Resposta Completa

```json
{
  "optimized_resume": {
    "professional_title": "Engenheiro DevOps Sênior | Especialista Kubernetes & Cloud",
    "professional_summary": "Profissional com 8+ anos de experiência em infraestrutura e DevOps, especializado em arquiteturas cloud-native e orquestração Kubernetes. Track record de redução de 40% em custos operacionais através de otimização de infraestrutura.",
    "skills": [
      {
        "category": "Orchestração e Containers",
        "items": ["Kubernetes", "Helm", "Kustomize", "Docker", "Podman"]
      },
      {
        "category": "Cloud Providers",
        "items": ["AWS EKS", "Azure AKS", "GCP GKE"]
      },
      {
        "category": "IaC",
        "items": ["Terraform", "Pulumi", "CloudFormation"]
      }
    ],
    "experience": [
      {
        "company": "TechCorp Brasil",
        "official_role": "Senior DevOps Engineer",
        "location": "São Paulo, SP",
        "start_date": "2021-03",
        "end_date": "presente",
        "highlights": [
          "Liderança de squad de 5 engenheiros",
          "Implementação de Kubernetes em produção com 99.99% uptime",
          "Redução de custos de infraestrutura em R$ 500k/ano"
        ]
      }
    ],
    "education": [
      {
        "institution": "USP",
        "degree": "Bacharel em Ciência da Computação",
        "period": "2012-2016"
      }
    ],
    "certifications": [
      {
        "name": "CKA - Certified Kubernetes Administrator",
        "issuer": "CNCF",
        "date": "2023-06"
      }
    ],
    "languages": [
      {"language": "Português", "level": "Nativo"},
      {"language": "Inglês", "level": "Avançado"}
    ]
  },
  "adherence_analysis": {
    "score": 78,
    "score_before_limiters": 82,
    "competitiveness": "alta",
    "justification": "Candidato bem alinhado com os requisitos da vaga, demonstrando experiência sólida em Kubernetes e arquiteturas cloud-native.",
    "requirements": [
      {
        "requirement": "Kubernetes em produção",
        "category": "mandatory",
        "weight": 5,
        "state": "matched",
        "evidence": "Implementou e mantém cluster Kubernetes com 99.99% uptime",
        "points": 95,
        "severity": "high"
      },
      {
        "requirement": "Experiência com cloud (AWS/Azure/GCP)",
        "category": "mandatory",
        "weight": 5,
        "state": "matched",
        "evidence": "Trabalhou com AWS EKS, Azure AKS e GCP GKE",
        "points": 90,
        "severity": "high"
      }
    ],
    "gaps": [
      {
        "gap": "Experiência limitada com GitOps",
        "severity": "medium",
        "impact": "Pode precisar de curva de adaptação para práticas de GitOps",
        "recommended_action": "Destacar experiência com ArgoCD ou Flux se disponível"
      }
    ],
    "strengths": [
      "Experiência comprovada com Kubernetes em escala",
      "Liderança técnica de squad",
      "Certificação CKA atualizada"
    ],
    "keyword_analysis": {
      "matched": ["kubernetes", "eks", "aks", "terraform", "helm", "devops", "ci/cd"],
      "missing": ["gitops", "argocd", "flux"]
    },
    "warnings": []
  }
}
```

### B. Checklist de Implementacao

- [ ] Schema JSON definido e documentado
- [ ] Prompt de IA atualizado com exemplos
- [ ] Validacao de schema no backend
- [ ] Persistencia da estrutura JSON
- [ ] Conversor Markdown -> estrutura
- [ ] Componentes React para preview
- [ ] StructuredDocxConverter
- [ ] Validacao de evidencias implementada
- [ ] Calculo de score com limitadores
- [ ] Testes unitarios completos
- [ ] Snapshot tests no frontend
- [ ] Testes de integracao DOCX
- [ ] Fallback para Markdown
- [ ] Log e monitoramento

---

*Documento gerado em: 2026-06-11*  
*Versao: 1.0.0*