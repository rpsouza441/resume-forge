# SPEC-07 — Frontend (Next.js App Router)

**Projeto:** Resume Forge
**Stack:** Next.js 14+ (App Router) + TypeScript + Tailwind CSS + TanStack Query + React Hook Form + Zod
**Versão:** 1.0.0
**Última atualização:** 2026-01-10

---

## 1. Stack e Configuração

### 1.1 Dependências

| Pacote                       | Versão   | Função                                 |
|------------------------------|----------|----------------------------------------|
| `next`                       | `14.x`   | Framework React com App Router         |
| `react` / `react-dom`        | `18.x`   | Biblioteca UI                          |
| `typescript`                 | `5.x`    | Superconjunto tipado de JavaScript     |
| `@tanstack/react-query`      | `5.x`    | Gerenciamento de estado de servidor    |
| `react-hook-form`            | `7.x`    | Gerenciamento de formulários           |
| `@hookform/resolvers`        | `3.x`    | Resolver de validação para RHF          |
| `zod`                        | `3.x`    | Schema de validação e tipos inferred   |
| `tailwindcss`               | `3.x`    | Framework de estilos                   |
| `clsx` / `tailwind-merge`   | latest   | Utilitários de concatenação de classes  |
| `lucide-react`              | `latest` | Biblioteca de ícones (SVG)             |
| `@radix-ui/react-*`         | latest   | Componentes acessíveis (Dialog, Select, etc.)|
| `sonner`                    | `1.x`    | Toast notifications                    |
| `date-fns`                  | `3.x`    | Formatação de datas                   |
| `next-auth` / `jose`        | latest   | Manipulação JWT no cliente             |

### 1.2 Configurações Importantes

**`next.config.js`:**
- `experimental.serverActions = true` (App Router)
- Imagens externas: permitir domínios da API e CDNs de avatares.

**`tsconfig.json`:**
- `strict: true`
- `paths` configurados para imports absolutos via alias `@/` (apontando para `src/`).

**`tailwind.config.js`:**
- Custom tokens para cores da marca (resume-forge palette).
- Screens: `sm: 640px`, `md: 768px`, `lg: 1024px`, `xl: 1280px`, `2xl: 1536px`.
- Desktop-first (default Tailwind).

---

## 2. Estrutura de Pastas

```
src/
├── app/
│   ├── (auth)/                      # Route Group: sem autenticação requerida
│   │   ├── login/
│   │   │   └── page.tsx
│   │   ├── register/
│   │   │   └── page.tsx
│   │   └── layout.tsx               # Layout público (sem NavBar)
│   │
│   ├── (main)/                      # Route Group: autenticação requerida
│   │   ├── layout.tsx               # Layout principal (NavBar + Sidebar)
│   │   ├── dashboard/
│   │   │   └── page.tsx
│   │   ├── resumes/
│   │   │   ├── page.tsx             # Lista de currículos base
│   │   │   ├── new/
│   │   │   │   └── page.tsx         # Criar currículo
│   │   │   └── [id]/
│   │   │       ├── page.tsx         # Detalhe do currículo base
│   │   │       └── edit/
│   │   │           └── page.tsx     # Editar currículo
│   │   ├── jobs/
│   │   │   ├── page.tsx             # Lista de vagas
│   │   │   ├── new/
│   │   │   │   └── page.tsx         # Nova vaga (wizard 3 etapas)
│   │   │   └── [id]/
│   │   │       └── page.tsx         # Detalhe da vaga
│   │   └── generated/
│   │       ├── page.tsx             # Lista de currículos gerados
│   │       └── [id]/
│   │           ├── page.tsx         # Detalhe do currículo gerado (com tabs)
│   │           └── edit/
│   │               └── page.tsx     # Editor do currículo gerado
│   │
│   ├── api/                         # Rotas de API Route (Next.js) — thin proxies
│   ├── layout.tsx                   # Root Layout
│   ├── page.tsx                     # Root redirect → /dashboard ou /login
│   └── not-found.tsx
│
├── components/
│   ├── ui/                          # Componentes primitivos (atômicos)
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Textarea.tsx
│   │   ├── Select.tsx
│   │   ├── Badge.tsx
│   │   ├── Card.tsx
│   │   ├── Modal.tsx                # Wrapper sobre Radix Dialog
│   │   ├── Toast.tsx                # Wrapper sobre Sonner
│   │   ├── Spinner.tsx
│   │   ├── Table.tsx
│   │   ├── FormField.tsx            # Label + Input + ErrorMessage
│   │   ├── Skeleton.tsx             # Loading placeholders
│   │   └── EmptyState.tsx
│   │
│   ├── forms/
│   │   ├── ResumeForm.tsx           # Form de currículo base
│   │   ├── JobForm.tsx              # Form de vaga
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   └── GenerationWizard.tsx     # Wizard 3 etapas
│   │
│   ├── layouts/
│   │   ├── MainLayout.tsx           # Shell com NavBar + main content
│   │   ├── NavBar.tsx
│   │   ├── Sidebar.tsx              # Navegação lateral colapsável
│   │   └── PageHeader.tsx           # Título + breadcrumbs + ações
│   │
│   └── shared/
│       ├── ResumeCard.tsx           # Card de currículo na listagem
│       ├── GeneratedResumeCard.tsx
│       ├── JobCard.tsx
│       ├── AdherenceScore.tsx       # Score visual (0-100 com barra)
│       ├── AnalysisPanel.tsx        # Painel de análise de aderência
│       ├── VersionHistory.tsx       # Lista de versões
│       ├── DocxDownloadButton.tsx   # Botão + loading state + download
│       └── AiRunDetails.tsx         # Expansível para AI Runs
│
├── hooks/
│   ├── useAuth.ts                   # Hook: user, login, logout, register
│   ├── useProtectedRoute.ts         # Redireciona se não autenticado
│   └── queries/
│       ├── useResumes.ts            # Queries e mutations de currículos base
│       ├── useJobs.ts               # Queries e mutations de vagas
│       ├── useGeneratedResumes.ts   # Queries e mutations de gerados
│       ├── useGenerate.ts           # Mutation de geração síncrona
│       └── useAiRuns.ts             # Queries de AI Runs
│
├── lib/
│   ├── api.ts                       # Cliente Axios configurado com interceptors
│   ├── auth.ts                      # Helpers JWT (decode, refresh)
│   ├── zod-schemas/                 # Schemas Zod espelhando API
│   │   ├── auth.ts                  # LoginSchema, RegisterSchema
│   │   ├── resume.ts                # CreateResumeSchema, UpdateResumeSchema
│   │   ├── job.ts                   # CreateJobSchema, UpdateJobSchema
│   │   ├── generate.ts              # GenerateRequestSchema
│   │   └── generated.ts             # GeneratedResumeSchema, AnalysisSchema
│   │
│   ├── formatters.ts                # formatDate, formatScore, formatCurrency
│   └── cn.ts                        # Util: clsx + tailwind-merge
│
├── providers/
│   ├── AuthProvider.tsx             # Context API para auth state
│   ├── QueryProvider.tsx            # React Query ClientProvider wrapper
│   └── ToastProvider.tsx            # Sonner toaster provider
│
└── types/
    ├── auth.ts
    ├── resume.ts
    ├── job.ts
    ├── generated.ts
    └── api.ts                       # Tipos comuns de API (PaginatedResponse, ErrorResponse)
```

---

## 3. Inventário de Telas

### 3.1 Tela 1 — Login / Registro

**Rota:** `/login` e `/register` (mesma tela com toggle de modo)

**Objetivo:** Autenticar usuário existente ou cadastrar novo usuário.

**Componentes principais:**
- `LoginForm` / `RegisterForm` (form com validação Zod)
- Toggle Login ↔ Registro
- Link "Esqueci minha senha" (futuro — placeholder)

**Estados:**

| Estado    | Comportamento                                                      |
|-----------|--------------------------------------------------------------------|
| Padrão    | Form vazio com validação em tempo real ao digitar                  |
| Submitting | Botão desabilitado, Spinner, campos readonly                      |
| Erro      | Toast de erro (credenciais inválidas, e-mail duplicado)            |
| Sucesso   | Redireciona para `/dashboard`, Toast "Bem-vindo(a), {name}!"      |

**Ações:**
- Submit: chama `POST /api/auth/login` ou `POST /api/auth/register`
- Falha: exibe toast de erro com `error.message` da API

---

### 3.2 Tela 2 — Dashboard

**Rota:** `/dashboard`

**Objetivo:** Visão geral do usuário com métricas e atalhos.

**Componentes principais:**
- `PageHeader` com saudação personalizada
- Cards de métricas (4 cards): Total Currículos, Currículos Gerados, Taxa Média de Aderência, Vagas Ativas
- Lista de últimas 5 atividades (últimos currículos gerados com score)
- Botões de ação rápida: "Novo Currículo", "Nova Vaga", "Gerar Currículo"

**Estados:**

| Estado    | Comportamento                                                     |
|-----------|-------------------------------------------------------------------|
| Padrão    | Cards com valores reais, lista com últimos itens                  |
| Loading   | Skeletons em todos os cards e na lista                            |
| Erro      | Card de erro com botão "Tentar novamente"                         |
| Vazio     | EmptyState com CTA para primeira ação                              |
| Atualização | Dados atualizam após mutations concluídas                      |

---

### 3.3 Tela 3 — Lista de Currículos Base

**Rota:** `/resumes`

**Objetivo:** Listar, buscar e gerenciar currículos base do usuário.

**Componentes principais:**
- `PageHeader`: "Meus Currículos" + botão "Novo Currículo"
- Barra de busca por título (debounce 300ms)
- Filtro `isDefault`
- `Table` com colunas: Título, Padrão, Atualizado em, Ações
- Badge "Padrão" no currículo padrão
- Ações por linha: Visualizar, Editar, Definir como padrão, Excluir

**Estados:**

| Estado    | Comportamento                                                    |
|-----------|------------------------------------------------------------------|
| Padrão    | Lista paginada com ordenação                                     |
| Loading   | Table com Skeleton rows                                          |
| Erro      | Table com mensagem de erro + botão "Tentar novamente"           |
| Vazio     | EmptyState: "Você ainda não tem currículos. Crie seu primeiro." |
| Atualização | Lista invalida cache após criar, editar ou gerar currículo       |

**Ações:**
- Criar: redireciona para `/resumes/new`
- Buscar: query param `?title=...` atualiza URL e refetch
- Excluir: Modal de confirmação antes de chamar `DELETE /api/resumes/{id}`
- Definir padrão: chama `PUT /api/resumes/{id}/default`, atualiza lista

---

### 3.4 Tela 4 — Cadastro / Edição de Currículo Base

**Rota:** `/resumes/new` e `/resumes/[id]/edit`

**Objetivo:** Criar ou editar um currículo base (modo estruturado ou modo livre).

**Componentes principais:**
- `PageHeader` com título dinâmico
- Toggle de modo: **Estruturado** | **Livre**
- `ResumeForm` (form com todos os campos)
- Seção: Dados Pessoais (nome, e-mail, telefone, localização)
- Seção: Resumo Profissional (textarea)
- Seção: Experiência (array dynamic — Add/Remove entries)
- Seção: Formação (array dynamic)
- Seção: Habilidades (tag input — adicionar/remover skill)
- Seção: Certificações (tag input)
- Seção: Idiomas (tag input)
- Botões: Salvar (submit) e Cancelar (navega de volta)

**Modo Livre:** Oculta todas as seções estruturadas e exibe único `Textarea` com `freeForm`.

**Estados:**

| Estado     | Comportamento                                                    |
|------------|------------------------------------------------------------------|
| Padrão     | Form preenchido com dados existentes (edit) ou vazio (create)  |
| Loading    | Spinner central                                                  |
| Submitting | Botão desabilitado, Spinner no botão, campos readonly            |
| Erro       | Toast de erro + erros inline nos campos inválidos (details[])  |
| Sucesso    | Redireciona para `/resumes`, Toast "Currículo salvo com sucesso"|
| Vazio      | N/A                                                              |

**Validação Zod (parcial):**

```typescript
const resumeSchema = z.object({
  title: z.string().min(1, "Título é obrigatório").max(200),
  data: z.object({
    fullName: z.string().optional(),
    email: z.string().email("E-mail inválido").optional().or(z.literal("")),
    phone: z.string().optional(),
    location: z.string().optional(),
    summary: z.string().max(2000).optional(),
    experience: z.array(experienceSchema).default([]),
    education: z.array(educationSchema).default([]),
    skills: z.array(z.string()).default([]),
    certifications: z.array(z.string()).default([]),
    languages: z.array(z.string()).default([]),
    freeForm: z.string().optional(),
  }),
});
```

---

### 3.5 Tela 5 — Nova Análise de Vaga (Wizard 3 Etapas)

**Rota:** `/jobs/new`

**Objetivo:** Criar uma vaga e iniciar a geração de currículo em um fluxo guiado.

**Etapa 1 — Dados da Vaga:**

- Campos: Título, Empresa, Descrição (textarea), Requisitos (tag input), Benefícios (tag input), Localização, Faixa Salarial
- Validação: Título, Empresa e Descrição obrigatórios
- Botão: "Próximo →"

**Etapa 2 — Selecionar Currículo Base:**

- Lista de currículos do usuário (cards com radio selection)
- Busca por título
- Indica qual é o padrão com badge "Padrão"
- Botões: "← Voltar" e "Próximo →"

**Etapa 3 — Configurar Geração + Confirmar:**

- Resumo da vaga selecionada (readonly)
- Resumo do currículo selecionado (readonly)
- Opções: Idioma (`pt-BR` default, `en-US`), Incluir scores (toggle, default true)
- Botão: "← Voltar" e "Gerar Currículo" (submit final)

**Estados por Etapa:**

| Estado      | Comportamento                                                    |
|-------------|------------------------------------------------------------------|
| Etapa ativa | Campos da etapa visíveis;其余 desabilitados                    |
| Validando   | Scroll até primeiro campo inválido com erro inline               |
| Etapa 3 Submitting | Botão desabilitado, Spinner                           |
| Sucesso     | Redireciona para `/generated/{id}`, Toast "Currículo sendo gerado!"|
| Erro        | Toast de erro, permanece na etapa 3                              |

**Fluxo de submit (Etapa 3):**
1. `POST /api/jobs` → cria vaga
2. `POST /api/generate` → gera de forma síncrona, mantendo a etapa em loading
3. Redireciona para detalhe do currículo gerado após resposta `201 Created`

---

### 3.6 Tela 6 — Detalhe do Currículo Gerado (Tabs)

**Rota:** `/generated/[id]`

**Objetivo:** Exibir currículo gerado completo com análise de aderência e histórico.

**Componentes principais:**

- `PageHeader` com título da vaga + score badge + ações
- `Tab 1 — Currículo`: Renderização formatada do conteúdo gerado (similar a PDF preview)
- `Tab 2 — Análise`: `AnalysisPanel` com score geral, keywords, gaps, sugestões
- `Tab 3 — Histórico`: `VersionHistory` com lista de versões + diff básico

**Score Badge:**
- 0-40: vermelho
- 41-70: amarelo
- 71-100: verde

**Estados:**

| Estado          | Comportamento                                                       |
|-----------------|---------------------------------------------------------------------|
| Gerando        | Loading bloqueante durante o submit síncrono de `POST /api/generate` |
| Pronto         | Tabs normais, score badge colorida                                   |
| Erro de geração | Permanece no wizard com mensagem de erro e botão "Tentar novamente" |
| Loading tabs    | Skeleton por tab                                                     |
| Tab Análise vazia| Texto "Análise ainda não disponível"                                 |

**Geração síncrona:**
- `POST /api/generate` mantém o botão em loading até retornar sucesso ou erro.
- Ao retornar sucesso, invalidar queries de `generated`, `jobs` e detalhe da vaga.
- Não há polling automático no MVP.

---

### 3.7 Tela 7 — Editor do Currículo Gerado

**Rota:** `/generated/[id]/edit`

**Objetivo:** Permitir edição manual do conteúdo do currículo gerado antes do download.

**Componentes principais:**
- `PageHeader` com "Editando Currículo v{version}" + badge da vaga
- Formulário com campos pré-preenchidos (originais + sugestões aceitas)
- Botões: Salvar como nova versão, Cancelar, Download DOCX

**Comportamento:**
- Salvar: `PUT /api/generated/{id}` com `contentMarkdown` e `contentJsonb` opcional
- Não persiste no banco como versão intermediária — cria nova versão
- Validação: campos obrigatórios (`fullName`, `email`)

---

### 3.8 Tela 8 — Histórico de Análises

**Rota:** `/generated` (lista principal)

**Objetivo:** Listar todos os currículos gerados com filtros avançados e expansão de detalhes inline.

**Componentes principais:**
- `PageHeader`: "Currículos Gerados"
- Filtros: `resumeProfileId` (select), `companyName`, `jobTitle`, intervalo de datas e `isCurrent`
- `Table` com colunas: Vaga, Currículo Base, Versão, Score, Status, Data, Ações
- Row expandível: mostra snippet da análise (overallScore + 2 primeiras sugestões)
- Badge de score/versão atual

**Estados:**

| Estado    | Comportamento                                                    |
|-----------|------------------------------------------------------------------|
| Padrão    | Filtros com valores da URL (shareable links)                     |
| Loading   | Table com Skeleton rows                                          |
| Erro      | Card de erro                                                     |
| Vazio     | EmptyState: "Nenhum currículo gerado ainda. Comece pela aba Gerar."|
| Filtro ativo | Badge de filtro ativo count                         |
| Atualização | Lista invalida cache após geração, edição ou regeneração       |

**Ações por linha:**
- Ver detalhes: link para `/generated/[id]`
- Download DOCX: chama `GET /api/generated/{id}/docx` (abre blob)
- Regenerar: link para `/generated/[id]/edit` com prefill

---

### 3.9 Tela 9 — Download DOCX

**Descrição:** Não é uma rota/page — é uma **ação** reutilizável.

**Comportamento:**
- Componente `DocxDownloadButton` com estados: idle, loading, success, error
- Loading: Spinner + texto "Gerando DOCX..."
- Success: baixa o arquivo via `URL.createObjectURL(blob)` + `anchor.click()`
- Error: Toast de erro "Falha ao gerar DOCX. Tente novamente."
- Cleanup: `URL.revokeObjectURL` após download

**Uso:**
- `/generated/[id]` → Tab Currículo + botão "Baixar DOCX"
- `/generated/[id]/edit` → botão "Salvar e Baixar"

---

## 4. Regras de UX

### Regra 1 — Loading States em Todas Operações Assíncronas

- **Queries:** Skeleton components enquanto `isLoading`.
- **Mutations submit:** Botão desabilitado + Spinner interno + campos `readonly`.
- **Botões de ação (delete, download):** Spinner + texto "Aguarde..." + desabilitado.
- **Navegação entre páginas:** Spinner central na página, nunca page blank.
- **Regra absoluta:** Nunca exibir página ou card vazio sem estado de loading ou empty state.

### Regra 2 — Erros Inline no Campo Relevante

- Erros de validação Zod aparecem **abaixo do campo** (não em toast) em tempo real.
- Erros de API com `details[]` populado aparecem **abaixo do campo específico** com ícone vermelho.
- Campo com erro: borda vermelha (`border-red-500`), fundo `bg-red-50`.
- Se erro de API não tem `field` mapeável: toast genérico.
- Ordem de prioridade: validação Zod client-side primeiro, depois validação server-side.

### Regra 3 — Mensagens de Erro Amigáveis

- **Nunca expor stack traces, mensagens de erro técnicas ou código de erro interno.**
- Mensagens de erro da API: usar `error.message` diretamente (já em português pela API).
- Fallback genérico: "Algo inesperado aconteceu. Tente novamente."
- Erro de rede: "Sem conexão. Verifique sua internet."
- 401 Token expirado: "Sua sessão expirou. Faça login novamente."
- 429 Rate limit: "Muitas requisições. Aguarde um momento e tente novamente."
- 500: "Erro no servidor. Nossa equipe foi notificada."

### Regra 4 — Toast Notifications

- **Sucesso (mutations):** Toast verde com ícone check, duração 4s, dismissível.
- **Erro:** Toast vermelho com ícone X, duração 6s, dismissível.
- **Informativo:** Toast azul, duração 3s.
- **Progresso:** Não usar toast persistente — usar loading inline no botão/formulário.
- Posição: bottom-right.
- Máximo 3 toasts simultâneos (Sonner tem esse comportamento por padrão).
- Toasts de erro **não desaparecem automaticamente** até o usuário dispensar ou timeout de 6s.

### Regra 5 — Suporte a Viewport 400px, Desktop-First

- **Layout responsivo:**
  - Sidebar: colapsável em `< lg`, oculta em `< md`, menu hamburger em mobile.
  - Tables: scroll horizontal em `< md`, cards empilhados em `< sm`.
  - Forms: duas colunas em `>= lg`, uma coluna em `< lg`.
  - Modal/Dialog: fullscreen em `< sm`, centrado com max-width em `>= sm`.
- **Touch targets:** Mínimo 44x44px para botões e inputs em mobile.
- **Font sizes:** Readable em 400px: body `text-base` (16px), labels `text-sm` (14px).
- **Breakpoint principal:** Desktop (`lg`). Mobile é Progressive Enhancement.

---

## 5. Componentes UI Necessários

| Componente       | Tipo      | Descrição / Responsabilidades                                         |
|-----------------|-----------|-----------------------------------------------------------------------|
| `Button`        | Primitivo | Variants: `primary`, `secondary`, `ghost`, `danger`; sizes: `sm`, `md`, `lg`; loading state com Spinner interno; disabled state |
| `Input`         | Primitivo | Label, placeholder, tipo (text/email/password/number), erro inline, ícone à esquerda |
| `Textarea`      | Primitivo | Rows configurável, maxLength com contador, resize vertical           |
| `Select`        | Primitivo | Wrapper Radix Select com options customizáveis, busca, multi-select opcional |
| `Badge`         | Primitivo | Variants: `default`, `success`, `warning`, `error`, `info`; size `sm`, `md` |
| `Card`          | Primitivo | Header, body, footer opcionais; variant `elevated` ou `bordered`     |
| `Modal`         | Compositivo | Radix Dialog com overlay, header, body, footer, close button      |
| `Toast`          | Provedor  | Sonner provider com customização de tema (cores da marca)            |
| `Spinner`       | Primitivo | Sizes: `xs` (16px), `sm` (24px), `md` (32px), `lg` (48px)           |
| `Table`         | Compositivo | Wrapper sobre HTML table com sorting, pagination, loading, empty    |
| `FormField`     | Compositivo | Label + Input/Textarea/Select + ErrorMessage em um componente       |
| `Skeleton`      | Primitivo | Animação pulse, variants para texto, card, table row                  |
| `EmptyState`    | Compositivo | Ícone + título + descrição + CTA button                              |

---

## 6. Gestão de Estado

### 6.1 Estado do Servidor — React Query

**Configuração do `QueryClient`:**

```typescript
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutos para listas
      gcTime: 10 * 60 * 1000,  // 10 minutos em cache
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});
```

**Regras de staleTime:**

| Tipo de Query              | staleTime | Motivo                                    |
|---------------------------|-----------|-------------------------------------------|
| Listas (`GET /resumes`)   | 5 min     | Dados mudam com baixa frequência          |
| Detalhes (`GET /resumes/{id}`) | 0      | Sempre fresco ao navegar                  |
| Currículos gerados (`GET /generated`) | 5 min | |
| Detalhe gerado (`GET /generated/{id}`) | 30s  | Revalidar ao abrir e após edição         |
| AI Runs                   | 0         | Logs de auditoria sempre frescos          |
| User (`GET /auth/me`)     | 5 min     | Stable                                    |

**Invalidation strategy:**
- `POST /resumes` → `invalidateQueries({ queryKey: ['resumes'] })`
- `PUT /resumes/{id}` → `invalidateQueries({ queryKey: ['resumes', id] })` + `['resumes']`
- `POST /generate` → `invalidateQueries({ queryKey: ['generated'] })`
- Logout → `queryClient.clear()`

### 6.2 Estado de Autenticação — AuthProvider

```typescript
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

// AuthProvider fornece:
const { user, isAuthenticated, isLoading, login, logout, register } = useAuth();
```

- Ao iniciar (mount): tenta ler token do cookie/localStorage, valida com `GET /auth/me`
- `isLoading: true` enquanto valida token no mount
- `isAuthenticated: true` apenas se `GET /auth/me` retornar 200
- `logout()`: limpa token, cookie, e `queryClient.clear()`, redireciona para `/login`

### 6.3 Estado de Formulário — React Hook Form

- Cada tela com formulário usa **uma instância isolada** de `useForm`.
- Schema Zod via `zodResolver`.
- Campos com validação em tempo real (não apenas no submit).
- `reset()` chamado após sucesso de mutation.
- Estados gerenciados nativamente pelo RHF: `formState.errors`, `formState.isSubmitting`, `formState.isDirty`.

### 6.4 Estado Global — Inexistente

- **Nenhum estado global além de AuthProvider.**
- Dados de servidor: React Query (cache).
- UI state (modals, sidebars): estado local do componente React.
- URL como source of truth para filtros e paginação (`router.push('?page=2')`).

---

## 7. Mapping Rota → Endpoint

| Rota Frontend                            | Método | Endpoint API                      | Tipo    | Query Params                                       |
|------------------------------------------|--------|-----------------------------------|---------|---------------------------------------------------|
| `/login` (submit)                        | POST   | `/api/auth/login`                 | Mutation| —                                                 |
| `/register` (submit)                     | POST   | `/api/auth/register`              | Mutation| —                                                 |
| `/dashboard`                             | GET    | `/api/auth/me`                    | Query   | —                                                 |
| `/dashboard`                             | GET    | `/api/resumes?size=5&sort=updatedAt,desc` | Query | —                                          |
| `/dashboard`                             | GET    | `/api/generated?size=5&sort=createdAt,desc` | Query | —                                        |
| `/dashboard`                             | GET    | `/api/jobs`                       | Query   | `size`, `sort`                                    |
| `/resumes`                               | GET    | `/api/resumes`                    | Query   | `page`, `size`, `sort`, `title`, `isDefault`     |
| `/resumes/new` (submit)                  | POST   | `/api/resumes`                    | Mutation| —                                                 |
| `/resumes/[id]`                          | GET    | `/api/resumes/{id}`                | Query   | —                                                 |
| `/resumes/[id]/edit` (submit)            | PUT    | `/api/resumes/{id}`                | Mutation| —                                                 |
| `/resumes/[id]` (delete)                 | DELETE | `/api/resumes/{id}`                | Mutation| —                                                 |
| `/resumes/[id]` (set default)            | PUT    | `/api/resumes/{id}/default`       | Mutation| —                                                 |
| `/jobs`                                  | GET    | `/api/jobs`                       | Query   | `page`, `size`, `sort`, `companyName`, `status`   |
| `/jobs/new` (submit — etapa 1)           | POST   | `/api/jobs`                       | Mutation| —                                                 |
| `/jobs/new` (submit — etapa 3)           | POST   | `/api/generate`                   | Mutation| —                                                 |
| `/jobs/[id]`                             | GET    | `/api/jobs/{id}?generatedResumes=true` | Query | —                                           |
| `/jobs/[id]` (update)                    | PUT    | `/api/jobs/{id}`                   | Mutation| —                                                 |
| `/jobs/[id]` (delete)                    | DELETE | `/api/jobs/{id}`                   | Mutation| —                                                 |
| `/generated`                            | GET    | `/api/generated`                  | Query   | `page`, `size`, `sort`, `resumeProfileId`, `companyName`, `jobTitle`, `dateFrom`, `dateTo`, `isCurrent` |
| `/generated/[id]`                        | GET    | `/api/generated/{id}`             | Query   | —                                                 |
| `/generated/[id]` (analysis)             | GET    | `/api/generated/{id}/analysis`     | Query   | carregar análise sob demanda                     |
| `/generated/[id]/edit` (submit)          | PUT    | `/api/generated/{id}`             | Mutation| —                                                 |
| `/generated/[id]/versions`               | GET    | `/api/generated/{id}/versions`    | Query   | —                                                 |
| `/generated/[id]/docx`                   | GET    | `/api/generated/{id}/docx`        | Query   | —                                                |
| `/api/auth/me` (auth check)             | GET    | `/api/auth/me`                    | Query   | — (usado no AuthProvider mount)                  |

---

## 8. Comportamentos Específicos de Geração

### Geração Síncrona (`POST /api/generate`)

```typescript
const generateMutation = useMutation({
  mutationFn: (payload) => api.post('/generate', payload),
  onSuccess: (generated) => {
    queryClient.invalidateQueries({ queryKey: ['generated'] });
    router.push(`/generated/${generated.id}`);
  },
});
```

- Enquanto a mutation estiver pendente, desabilitar botões de voltar/submeter.
- Exibir loading inline na etapa final do wizard.
- Em sucesso: toast "Currículo gerado com sucesso!" e redirecionamento para detalhe.
- Em erro: manter dados preenchidos e mostrar toast/erro inline.

### Refresh Manual

- Pull-to-refresh: não aplicável (desktop-first)
- Botão "Atualizar" (`refetch`) em todas as listas
- Listas com `staleTime: 5min` não fazem refetch automático ao revisit — memorization do React Query

---

## 9. Configuração de API Client (`src/lib/api.ts`)

```typescript
// Axios instance com interceptors
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? 'https://api.resumeforge.com/v1',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: anexa Bearer token
api.interceptors.request.use((config) => {
  const token = getAccessToken(); // lê de localStorage/cookie
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: trata 401 → redirect login, 429 → toast rate limit
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        error.config.headers.Authorization = `Bearer ${getAccessToken()}`;
        return api.request(error.config);
      }
      logout(); // token não pôde ser refreshado
    }
    return Promise.reject(error);
  }
);
```

---

## 10. Error Handling no Frontend

### Hierarquia de tratamento:

1. **Interceptor Axios** — erros 401, 429, 500 genéricos
2. **React Query `onError`** — invalidação de cache, toasts específicos
3. **Zod `onSubmit` errors** — erros de validação client-side (nunca chegam ao servidor)
4. **Server-side `details[]`** — mapeamento campo → erro inline

### Mapeamento `error.details[].field` → Input:

```typescript
// Exemplo de handler genérico em FormField
const serverErrors = error.response?.data?.details ?? [];
const fieldError = serverErrors.find((e) => e.field === fieldName);
if (fieldError) {
  setError(fieldName, { message: fieldError.message });
}
```

---

## 11. Acessibilidade

- Todos os `Input`, `Textarea`, `Select` têm `id` e `aria-describedby` apontando para a mensagem de erro.
- `Button` com `isLoading` mantém `aria-busy="true"` e `aria-label` atualizado.
- Modais: foco preso no modal, `Escape` fecha, overlay click fecha.
- Tables: `role="table"`, `aria-sort` em colunas ordenáveis.
- Empty states: `role="status"` para screen readers.
- Skip-to-content link no root layout.
