# 07 — Frontend

## 1. Pilha Tecnológica

### Framework: Next.js com App Router

O projeto utiliza Next.js na versão 14 ou superior com App Router. A escolha pelo App Router (em detrimentos do Pages Router) é definitiva por razões arquiteturais: React Server Components permitem buscar dados no servidor antes do HTML ser enviado ao cliente, eliminando requests adicionais de hydration e reduzindo o JavaScript enviado ao browser. Para uma aplicação administrativa com foco em performance de carregamento inicial, esse é um benefício direto.

O Pages Router permanece como opção apenas para equipes já familiarizadas com o paradigma legado; para o MVP deste projeto, App Router é a única opção aceita.

### Componentes: Server vs. Client

A fronteira entre Server Components e Client Components deve ser respeitada rigorosamente:

- **Server Components**: todas as telas de listagem, dashboards, páginas de detalhe onde os dados vêm do servidor. O componente recebe os dados como props e os renderiza sem estado local.
- **Client Components**: formulários de login/registro, formulários de cadastro de currículo, telas com botões de ação que disparam mutações, modais de edição. Esses componentes recebem a diretiva `"use client"` no topo do arquivo.

A regra prática: se o componente usa `useState`, `useEffect`, handlers de evento, ou interage com React Query mutations, é um Client Component. Caso contrário, permanece como Server Component.

### Estado do Servidor: React Query (TanStack Query)

Toda interação com a API REST (GET, POST, PUT, DELETE) é gerenciada por React Query. O QueryClient é configurado no nível do root layout com cache timeout de 5 minutos para queries de lista e 0 (no-cache) para queries de detalhe individual.

React Query fornece:
- **Caching automático**: cada query é cacheada pela sua chave; navegação de volta a uma lista não dispara novo fetch.
- **Background refetch**: se o usuário deixa a aba do navegador inativa por mais de 30s e volta, React Query refetch automaticamente.
- **Optimistic updates**: para operações de delete e set-default em currículos base, a UI atualiza imediatamente antes da resposta do servidor, revertendo em caso de erro.
- **Loading/error states**: cada hook retorna `isLoading`, `isError`, `data` e `error`, que alimentam estados visuais da tela.

### Validação de Esquema: Zod

Zod é utilizado para validação de dados tanto no frontend quanto no backend. No frontend, Zod schemas definem o formato esperado das respostas da API e validam dados de formulário antes do submit. O schema pode ser compartilhado via um pacote interno (`@cur-oti/shared`) no futuro; no estado atual, cada camada define seus próprios schemas de forma independente.

### Estilização: Tailwind CSS

Tailwind CSS é o framework de CSS adotado. A configuração padrão do Next.js com Tailwind é suficiente para o MVP. Não há necessidade de CSS-in-JS, CSS Modules personalizados, ou bibliotecas de componentes (como shadcn/ui ou MUI) que adicionam overhead de configuração. Se a equipe precisar de componentes prontos no futuro, shadcn/ui é a escolha recomendada por ser leve e customizável.

### Estado Global: Sem Redux

Redux está excluído do MVP. A justificativa não é técnica — Redux funciona — mas prática: para uma aplicação com estado global limitado ao token de autenticação e preferências de UI, Redux adiciona complexidade desproporcional. O estado de autenticação é gerido com React Context simples (provedor `AuthProvider` que lê e escreve no localStorage) e o estado de formulário é gerido por tela com React Hook Form. Essa arquitetura cobre 100% das necessidades do MVP sem dependência adicional.

### Gerenciamento de Formulários: React Hook Form

React Hook Form controla todos os formulários da aplicação. Cada tela de formulário cria sua própria instância; não há estado de formulário global. O hook `useForm` fornece `register`, `handleSubmit`, `formState` (com `errors`), e `reset`. A integração com Zod é feita via `zodResolver`, que conecta o schema Zod ao `validationResolver` do formulário.

---

## 2. Inventário de Telas

### Tela 1: Login / Registro

**Objetivo**: autenticar usuário existente ou criar nova conta.

Campos para login: email, senha.
Campos para registro: nome completo, email, senha.
Estrutura: formulário com dois modos (aba ou toggle) — um para login, outro para registro.

**Estados**:
- Padrão: campos vazios, botão "Entrar" ou "Criar conta"
- Loading: botão exibe spinner, campos desabilitados, nenhum botão secundário clicável
- Erro: mensagem de erro inline abaixo do campo relevante (ex.: "Email ou senha incorretos")

**Fluxo**: submit valida campos localmente (email format, senha min 8 chars) → chamada POST /api/auth/login ou /api/auth/register → em caso de sucesso, JWT é armazenado no localStorage e usuário é redirecionado para /dashboard.

Funcionalidade "Lembrar-me" não existe no MVP.

---

### Tela 2: Dashboard

**Objetivo**: ponto de entrada pós-login, visão geral da atividade do usuário.

**Dados exibidos**:
- Três cards de estatísticas: total de currículos base cadastrados, total de aplicações/vagas analisadas, total de currículos gerados
- Lista de atividade recente: as 5 últimas gerações de currículo, ordenadas por data, mostrando empresa, título da vaga e data

**Ações rápidas**:
- Botão "Nova análise" → leva a tela de Nova Análise de Vaga
- Botão "Meus currículos base" → leva a tela de Lista de Currículos Base

**Estado vazio**: se o usuário ainda não tem currículos base, o dashboard exibe mensagem explicativa e um CTA destacado: "Cadastre seu primeiro currículo base para começar a gerar aplicações otimizadas." O CTA aponta para a tela de Cadastro de Currículo Base.

---

### Tela 3: Lista de Currículos Base

**Objetivo**: gerenciar todos os currículos base do usuário.

**Layout**: tabela com as seguintes colunas: Título, Data de criação, Badge "Padrão" (se for o default), Ações.

**Ações por linha**:
- Editar: abre tela de edição com ID do currículo
- Excluir: modal de confirmação antes de chamar DELETE /api/resumes/{id}
- Definir como padrão: se ainda não for o padrão, chama PUT /api/resumes/{id}/default

**Estado vazio**: mensagem amigável e CTA para criar o primeiro currículo base.

---

### Tela 4: Cadastro / Edição de Currículo Base

**Objetivo**: criar ou atualizar um currículo base.

**Modos de entrada**:
- Modo estruturado: formulário com seções repetíveis — Dados Pessoais, Experiência Profissional (repetível), Formação Acadêmica (repetível), Skills, Certificações. Cada seção tem campos adequados ao tipo.
- Modo livre: textarea único onde o usuário cola o texto do currículo como está. O conteúdo é armazenado como `content_text` e o backend faz a conversão para `content_markdown`.

**Campos obrigatórios**: título do currículo, e pelo menos nome ou algum conteúdo.

**A validação local**: aviso se o conteúdo total for menor que 200 caracteres (currículo provavelmente incompleto). O aviso não bloqueia o save, apenas exibe alerta.

**Ao salvar**: chamada POST (criação) ou PUT (edição) /api/resumes. O backend gera `content_markdown` a partir dos dados estruturados e persiste. A tela exibe estado de loading no botão e, em caso de sucesso, redireciona para a lista.

---

### Tela 5: Nova Análise de Vaga

**Objetivo**: iniciar o processo de geração de currículo otimizado para uma vaga específica.

Estrutura em **três etapas** (wizard):

**Etapa 1 — Selecionar currículo base**:
Card de seleção mostrando todos os currículos base do usuário. Um deve ser selecionado obrigatoriamente para prosseguir. Card selecionado ganha borda destacada.

**Etapa 2 — Detalhes da vaga**:
Campos: empresa (opcional), título da vaga (obrigatório), localização (opcional), tipo de emprego (select: CLT, PJ, Freelance, Estágio — opcional), senioridade (select: Estágio, Júnior, Pleno, Sênior, Especialista — opcional).
Campo de descrição da vaga: textarea obrigatório, mínimo 100 caracteres. Hint: "Cole aqui a descrição completa da vaga".

**Etapa 3 — Revisão e geração**:
Resumo visual dos dados selecionados: nome do currículo base, empresa, título, tipo de emprego. Botão principal: "Gerar currículo otimizado".

**Estado de loading**: ao clicar em gerar, tela inteira entra em estado de loading com spinner centralizado e texto: "Gerando currículo otimizado... isso pode levar até 30 segundos." O botão de voltar é desabilitado durante o processo.

**Estado de erro**: se a geração falhar, exibir mensagem de erro explicita e botão "Tentar novamente" que retorna a etapa 3 com os dados preservados.

**Sucesso**: redirecionamento para a tela de Detalhe do Currículo Gerado.

---

### Tela 6: Detalhe do Currículo Gerado

**Objetivo**: revisar o currículo gerado pela IA e a análise de adequação.

**Cabeçalho**: empresa, título da vaga, data de geração, badge de score de adequação (ex.: "82% de adequação").

**Abas/seções**:
- **Currículo Gerado**: conteúdo formatado de forma legível (não markdown cru). Renderizado em HTML estilizado com Tailwind.
- **Análise**: score numérico, lista de pontos fortes identificados, lacunas identificadas, mapa de keywords da vaga vs. keywords presentes no currículo.
- **Histórico de Versões**: lista de versões anteriores do currículo para aquela vaga, com data e botões para visualizar.

**Ações**:
- Editar: abre tela do Editor do Currículo Gerado
- Baixar DOCX: aciona download do documento
- Nova análise para esta vaga: retorna a tela de Nova Análise com vaga pré-preenchida (gera nova versão)

---

### Tela 7: Editor do Currículo Gerado

**Objetivo**: permitir edição manual do currículo gerado antes do download final.

**Layout**: textarea grande ocupando a maior parte da tela, pré-preenchida com o conteúdo `content_markdown` do currículo. Toolbar simples acima do textarea com botões para inserir marcadores de negrito (`**`) e bullet points (`- `).

**Auto-save**: inexistente no MVP. Única forma de persistir é clicando no botão "Salvar". Isso simplifica a implementação e evita complexidades de debounce e conflitos de edição simultânea.

**Fluxo de save**: clique em Salvar → botão exibe estado de loading → PUT /api/generated/{id} com novo conteúdo → sucesso: toast "Currículo salvo com sucesso" (auto-dismiss 4s) → atualiza versão na tela.

**Navegação**: ao tentar sair da página com alterações não salvas, o navegador exibe o prompt nativo de confirmação ("Você tem alterações não salvas. Deseja sair?"). Implementado via `beforeunload` event listener.

---

### Tela 8: Histórico de Análises

**Objetivo**: navegar por todas as gerações de currículo, independente de vaga.

**Filtros**:
- Empresa: campo de texto livre
- Título da vaga: campo de texto livre
- Período: seletor de range de datas (de / até)

**Lista**: cada linha mostra empresa, título da vaga, data de geração, badge de score de adequação, contador de versões.

**Expansão**: ao clicar em uma linha, ela expande mostrando a lista de versões disponíveis para aquela vaga. Cada versão tem botões: visualizar, ver versão específica, baixar DOCX.

**Ação em lote**: não existe no MVP.

---

### Tela 9: Download DOCX

Não existe como tela separada. O download é uma ação iniciada a partir da Tela 6 (botão "Baixar DOCX") ou da Tela 7 (botão "Salvar e Baixar").

**Fluxo**: clique no botão → botão entra em estado de loading com spinner → GET /api/generated/{id}/docx → backend gera DOCX on-the-fly, retorna como `application/vnd.openxmlformats-officedocument.wordprocessingml.document` → browser exibe diálogo de download com nome de arquivo no formato: `[nome]-otimizado-[empresa]-[YYYY-MM-DD].docx`.

**Tratamento de erro**: se o backend retornar erro, o botão exibe estado de erro e um toast é exibido: "Não foi possível gerar o arquivo. Tente novamente." O arquivo não é baixado.

---

## 3. Regras de UX

**Regra 1 — Estados de loading em todas operações assíncronas**
Toda operação que envolve chamada de API deve exibir estado visual de loading. Isso inclui botões com spinner, desabilitação de campos, e mascaramento de áreas de conteúdo durante refetch. O usuário nunca deve ficar em dúvida se algo está carregando ou se a aplicação travou.

**Regra 2 — Erros de formulário inline no campo relevante**
Quando um campo falha na validação, a mensagem de erro aparece diretamente abaixo daquele campo, na cor de erro do design system. A mensagem deve ser específica: "Campo obrigatório", "Email inválido", "Mínimo 100 caracteres". Erros de formulário nunca aparecem em modal separado ou em toast.

**Regra 3 — Mensagens de erro amigáveis**
Erros vindos do backend ou de validações de negócio nunca exibem stack traces, códigos de erro técnicos, ou mensagens de exceção Java/TypeScript. Toda mensagem de erro é traduzida para linguagem de usuário final. Exemplo: erro 500 do backend vira "Algo deu errado. Tente novamente em alguns segundos." Exemplo: erro de validação de conteúdo vira "O currículo está muito curto. Adicione mais detalhes antes de salvar."

**Regra 4 — Toast notifications para feedback de ação**
Todas as ações de mutação (salvar, excluir, gerar, baixar) mostram feedback via toast. Toasts de sucesso aparecem com ícone de check e mensagem positiva. Toasts de erro aparecem com ícone de X e mensagem acionável quando aplicável. Toasts desaparecem automaticamente após 4 segundos. Toasts não bloqueiam interação com o resto da página.

**Regra 5 — Suporte a viewport de 400px, desktop-first**
O MVP é projetado para desktop. A largura mínima suportada é 400px. Abaixo disso, elementos podem empilhar verticalmente. Não há garantia de UX ótima em mobile, mas a aplicação não deve quebrar. A partir da fase 2, pode-se considerar uma abordagem responsive-first se o uso mobile for identificado como relevante.

---

## 4. Gestão de Estado

### Estado do Servidor

React Query é a única fonte de verdade para dados que vêm da API. Cada entidade (resumes, jobs, generated) tem sua query key prefix. Queries são configuradas com `staleTime` de 5 minutos para dados de lista e `staleTime: 0` para dados que mudam com frequência (como o status de uma geração em andamento).

Mutations usam `onMutate` para optimistic updates em deletes e updates de status. `onError` faz rollback automático. `onSettled` refetch a query afetada para garantir consistência.

### Estado de Autenticação

`AuthProvider` é um React Context que armazena o token JWT e os dados básicos do usuário logado (id, nome, email). O token é lido do localStorage na inicialização do provider. O provider expõe `user`, `token`, `login(token, userData)`, `logout()`, e `isAuthenticated`. Não há refresh token no MVP — se o token expirar, o usuário faz login novamente.

### Estado de Formulário

Cada formulário é gerido por sua própria instância de React Hook Form. Não há formulário global. Campos complexos (seções repetíveis de experiência, formação) usam `useFieldArray` do React Hook Form para gerenciar arrays de campos dinâmicos.

### Estado Global

Não existe. O Context de Autenticação cumpre o papel de estado global mínimo necessário. Se no futuro houver necessidade de estado global para outras seções (temas, notificações em tempo real, preferências), a escolha é avaliar React Context simples antes de recorrer a bibliotecas de estado.

---

## 5. Estrutura de Pastas Proposta

```
src/
  app/
    (auth)/
      login/page.tsx
      register/page.tsx
    (main)/
      dashboard/page.tsx
      resumes/
        page.tsx                  # lista
        new/page.tsx              # cadastro
        [id]/edit/page.tsx       # edição
      jobs/
        page.tsx                  # lista de vagas
        new/page.tsx             # cadastro de vaga
        [id]/page.tsx            # detalhe da vaga
      generated/
        page.tsx                  # histórico de currículos gerados
        [id]/page.tsx           # detalhe do currículo gerado
        [id]/edit/page.tsx      # editor
      layout.tsx                # layout autenticado (sidebar/nav)
    layout.tsx                  # root layout
    page.tsx                    # redireciona para /login ou /dashboard
  components/
    ui/                         # componentes base (Button, Input, Badge, Toast, etc.)
    forms/                      # componentes de formulário reutilizáveis
    layouts/                   # Shell, Sidebar, PageHeader
  hooks/
    useAuth.ts
    queries/                    # arquivos de queries por entidade
  lib/
    api.ts                     # cliente HTTP (axios ou fetch wrapper)
    zod-schemas/               # schemas Zod compartilhados
  providers/
    AuthProvider.tsx
    QueryProvider.tsx
    ToastProvider.tsx
```

---

## 6. Nota sobre Nomenclatura de Endpoints

A documentação do frontend usa caminhos de rota que correspondem aos endpoints da API REST definidos em `06-api-backend.md`. O mapping entre rota frontend e endpoint API é:

| Rota Frontend | Ação | Endpoint API | Método |
|---------------|------|-------------|--------|
| /resumes | listar | /api/resumes | GET |
| /resumes/new | criar | /api/resumes | POST |
| /resumes/[id]/edit | editar | /api/resumes/{id} | PUT |
| /resumes/[id] | excluir | /api/resumes/{id} | DELETE |
| /jobs | listar | /api/jobs | GET |
| /jobs/new | criar | /api/jobs | POST |
| /generated | listar | /api/generated | GET |
| /generated/[id] | detalhe | /api/generated/{id} | GET |
| /generated/[id]/edit | editar | /api/generated/{id} | PUT |
| /generated/[id]/docx | download | /api/generated/{id}/docx | GET |
| /generated/[id]/versions | versões | /api/generated/{id}/versions | GET |

**Importante**: o frontend nunca acessa diretamente `content_jsonb` para display — usa `content_markdown` para renderização e edição. O JSONB é enviado para a IA e usado em funcionalidades futuras de edição estruturada.