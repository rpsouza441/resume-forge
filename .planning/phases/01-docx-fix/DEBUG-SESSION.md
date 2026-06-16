# 01-docx-fix: Debug Session Notes

## Problema Inicial
Preview e DOCX não mostravam dados do candidato. Campos vinham vazios ou em branco.

## Diagnóstico

### 1. Estrutura JSON da AI
```sql
-- Query para verificar estrutura no banco:
docker exec resumeforge-postgres psql -U postgres -d resumeforge -c "
SELECT content_jsonb FROM generated_resumes ORDER BY created_at DESC LIMIT 1;"
```

**Encontrado:** AI retornava:
```json
{
  "optimized_resume": {
    "sections": {
      "skills": ["Windows Server", ...],        // ERRADO: array de strings
      "summary": "texto..."                      // ERRADO: "summary" não "professional_summary"
    }
  }
}
```

**Esperado pelo código:**
```json
"sections": {
  "skills": [{"category": "string", "items": ["string"]}],  // Objetos com category/items
  "professional_summary": "string",                          // "professional_summary"
  "experience": [{"company": "string", ...}]
}
```

### 2. Validação falhando
```
java.lang.IllegalArgumentException: At least one section is required in the resume structure
    at StructuredDocxConverter.validateInput(StructuredDocxConverter.java:214)
```

O `fromJson()` detectava `sections` mas os dados dentro estavam em formato errado.

### 3. Preview frontend
O TypeScript reclamava porque `sections` não existia no tipo `OptimizedResume` em `types/generated/index.ts`.

## Correções Aplicadas

### A) Frontend - Tipo TypeScript
**Arquivo:** `frontend/src/types/generated/index.ts`
```typescript
export interface OptimizedResume {
  sections?: {
    professional_title?: string;
    professional_summary?: string;
    skills?: Array<{ category: string; items: string[] }>;
    experience?: Array<{...}>;
    // ...outros campos
  };
  // Campos flat (legacy)
  professional_title?: string;
  // ...
}
```

### B) Frontend - Page Preview
**Arquivo:** `frontend/src/app/(dashboard)/generated/[id]/page.tsx`
```typescript
resume={resume.contentJsonb?.optimized_resume?.sections ?? resume.contentJsonb?.optimized_resume}
```

### C) Backend - fromJson
**Arquivo:** `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`
```java
Map<String, Object> source = jsonb;
if (jsonb.containsKey("sections") && jsonb.get("sections") instanceof Map) {
    log.debug("Extracting resume data from optimized_resume.sections");
    source = (Map<String, Object>) jsonb.get("sections");
}
```

### D) Backend - Logging
**Arquivo:** `backend/src/main/java/com/resumeforge/export/service/DocxGenerationService.java`
```java
log.info("DOCX_RENDERER=STRUCTURED schemaVersion=2 hasHeader={} experiences={} ...");
log.warn("DOCX_RENDERER=MARKDOWN markdownFallback=true - optimized_resume not found...");
```

## Problema Real: Prompt da AI

O prompt em `PromptBuilderService.java` tinha:
```java
"sections": { ... }  // NÃO especificava os campos!
```

A AI inventava nomes como `summary` e `skills` como array de strings.

**Solução:** Adicionar schema EXATO no prompt:
```json
"sections": {
  "professional_title": "string",
  "professional_summary": "string",
  "skills": [{"category": "string", "items": ["string"]}],
  "experience": [{"company": "string", "official_role": "string", ...}],
  ...
}
```

## Lessons Learned

1. **Verificar banco ANTES de fazer 1000 rebuilds** - Query SQL economiza tempo
2. **Prompt deve especificar schema EXATO** - Não deixar a AI inventar nomes
3. **Commits devem incluir TODOS os arquivos modificados** - `types/generated/index.ts` foi esquecido
4. **Docker build com cache** - Única solução é `docker image rm` + `--no-cache`

## Comandos Úteis

```bash
# Ver estrutura do JSON no banco
docker exec resumeforge-postgres psql -U postgres -d resumeforge -c "
SELECT content_jsonb->'optimized_resume'->'sections'->>'professional_summary' as summary,
       content_jsonb->'optimized_resume'->'sections'->'experience' IS NOT NULL as has_exp
FROM generated_resumes ORDER BY created_at DESC LIMIT 1;"

# Rebuild forçado
docker image rm resumeforge-backend --force
docker compose build --no-cache backend
docker compose down && docker compose up -d

# Ver logs de geração DOCX
docker logs resumeforge-backend 2>&1 | grep DOCX_RENDERER
```

## Status: Pendente

- [ ] Verificar se novo currículo tem `professional_summary` e `skills` com formato correto
- [ ] Confirmar que preview e DOCX mostram dados estruturados
- [ ] Se não funcionar, corrigir PROMPT da AI, não o código
