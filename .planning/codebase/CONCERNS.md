---
name: CONCERNS
description: Technical debt, known issues, and areas of risk
last_mapped_commit: HEAD
---

# CONCERNS — Technical Debt & Risk Areas

**Date:** 2026-06-15  
**Focus:** Concerns

## Known Issues & Technical Debt

### 1. AI Provider Error Handling
- **Risk:** AI API calls may fail without proper retry logic
- **Files:** `backend/src/main/java/com/resumeforge/ai/provider/impl/*`
- **Mitigation:** `ContentValidationService` validates output, but retry/backoff not visible

### 2. Missing Unit Tests
- **Risk:** Limited test coverage for critical paths
- **Files:** Backend services, controllers
- **Mitigation:** Add tests before major changes

### 3. Frontend API Error Handling
- **Risk:** API errors may not display user-friendly messages
- **Files:** `frontend/src/hooks/queries/*`, `frontend/src/lib/api.ts`
- **Mitigation:** Add error boundaries and toast notifications

### 4. JWT Token Expiration
- **Risk:** No visible refresh token mechanism
- **Files:** `backend/src/main/java/com/resumeforge/auth/security/*`
- **Mitigation:** Implement token refresh flow

### 5. Database Connection Pool
- **Risk:** Default H2 used in dev; PostgreSQL config may need tuning
- **Files:** `backend/src/main/resources/application.yml`
- **Mitigation:** Configure HikariCP for production

### 6. DOCX Generation Complexity
- **Risk:** `StructuredDocxConverter` may have edge cases
- **Files:** `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`
- **Mitigation:** Add more test cases for different content types

### 7. AI Prompt Injection
- **Risk:** User input in prompts could be exploited
- **Files:** `backend/src/main/java/com/resumeforge/generation/service/PromptBuilderService.java`
- **Mitigation:** Sanitize user input before building prompts

### 8. Missing Rate Limiting
- **Risk:** API endpoints vulnerable to abuse
- **Files:** `backend/src/main/java/com/resumeforge/*/controller/*`
- **Mitigation:** Add rate limiting for AI generation endpoints

## Security Considerations

### Current
- ✅ JWT authentication implemented
- ✅ Spring Security filter chain
- ✅ Input validation with Zod (frontend) and Bean Validation (backend)

### Needed
- 🔲 Rate limiting on AI endpoints
- 🔲 API key rotation mechanism
- 🔲 Audit logging for sensitive operations
- 🔲 CORS configuration review for production

## Performance Considerations

### Backend
- AI API calls are synchronous — consider async processing
- Database queries may need indexing
- DOCX generation is CPU-intensive

### Frontend
- React Query caching configured
- Consider virtualization for large lists
- Image optimization for Next.js

## Areas for Improvement

### High Priority
1. Add comprehensive test coverage
2. Implement token refresh mechanism
3. Add rate limiting

### Medium Priority
4. Optimize database queries
5. Add API documentation (OpenAPI/Swagger)
6. Implement async AI processing

### Low Priority
7. Add monitoring/observability
8. Implement WebSocket for real-time updates
9. Add admin dashboard

---

## Planned Epics

### Epic 1: DOCX Fix 🔴 (Alta)
- **Base:** Prompt de auditoria detalhado criado em 2026-06-15
- **Owner:** TBD
- **Skills:** `engineering/code-review`, `engineering/diagnose`
- **Status:** Aguardando implementação
- **Problems:**
  - Fontes em 20-21pt (corpo maior que títulos)
  - Header ausentes (nome, título, contatos)
  - Markdown residual visível (`**`, `###`)
  - Sem bullets reais OOXML
  - 3 páginas para conteúdo de 1 página
  - Margens 2.5cm (deveria ser 1.5-1.8cm)
- **Files:** `backend/src/main/java/com/resumeforge/export/converter/StructuredDocxConverter.java`
- **Deliverables:**
  - Causa exata da fonte de 20-21pt
  - Header estruturado implementado
  - Bullets reais OOXML
  - Testes estruturais
  - QA visual

### Epic 2: AI Config Frontend 🟡 (Média)
- **Base:** concerns #1, #4, #8 (error handling, JWT refresh, rate limiting)
- **Owner:** TBD
- **Skills:** `frontend/react-patterns`, `backend/api-design`
- **Status:** Planejado
- **Changes:**
  - Mover API key e seleção de modelo do env → frontend
  - Permitir mudança de provedor AI em runtime
  - UI para configurar credenciais
- **Files:** `backend/src/main/resources/application.yml`, `frontend/src/lib/api.ts`
- **Dependencies:** None (pode ser feito em paralelo)

### Epic 3: UX Improvement 🟢 (Baixa)
- **Base:** concerns #9 (admin dashboard)
- **Owner:** TBD
- **Skills:** UX Design
- **Tools:** Lovable / Google Stitch
- **Status:** Futuro
- **Changes:**
  - Melhorar UX geral do dashboard
  - Novo design system
  - Admin dashboard completo
- **Dependencies:** Epic 1 e 2 (precisa stabilização primeiro)

---

## Epic Roadmap

| # | Epic | Priority | Status | Owner |
|---|------|----------|--------|-------|
| 1 | DOCX Fix | 🔴 Alta | Aguardando | TBD |
| 2 | AI Config Frontend | 🟡 Média | Planejado | TBD |
| 3 | UX Improvement | 🟢 Baixa | Futuro | TBD |

---

*Concerns audit: 2026-06-15*
*Epics added: 2026-06-15*
