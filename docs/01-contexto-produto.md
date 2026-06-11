# Contexto do Produto

## Objetivo do Produto

Criar um sistema SaaS que permita ao profissional de mercado cadastrar um currículo base uma única vez e, a partir dele, gerar múltiplas versões otimizadas para vagas específicas de emprego — utilizando inteligência artificial generativa para identificar palavras-chave, analisar aderência e produzir currículos sob medida.

O sistema fornece:
- **Otimização por IA**: currículos gerados com base na descrição da vaga, não adivinhação.
- **Análise de aderência**: diagnóstico explícito de compatibilidade entre currículo e vaga.
- **Histórico completo**: todas as versões geradas, por empresa, vaga e data.
- **Edição manual**: o usuário pode refinar o currículo gerado pela IA.
- **Exportação profissional**: download em formato Word (.docx) compatível com ATS.

---

## Personas Iniciais

### Persona A — Desenvolvedor Júnior

- **Nome**: Carolina, 23 anos, recém-formada em Ciência da Computação.
- **Contexto**: fez 3 estágios curtos, projetos pessoais no GitHub, um bootcamp. Busca primeiro emprego CLT.
- **Necessidade**: apresentar-se de forma competitiva mesmo com pouca experiência formal. Destacar projetos, tecnologias e eagerness to learn.
- **Dor principal**: ao colar sua experiência em uma vaga de backend, não sabe se deve mencionar o estágio de suporte; se deve listar todos os projetos ou só os mais relevantes; se parecerá "muito júnior".
- **Como o sistema ajuda**: a IA identifica as keywords da vaga, aponta lacunas e gera uma versão que destaca projetos e tecnologias relevantes sem inventar experiência.

### Persona B — Profissional em Transição de Carreira

- **Nome**: Rafael, 31 anos, 6 anos de experiência em suporte técnico, fazendo transição para desenvolvimento backend.
- **Contexto**: sabe Python, fez cursos, fez projetos pessoais. Currículo atual fala tudo de suporte — parece que não sabe programar.
- **Necessidade**: reformular completamente a narrativa do currículo para mostrar competências transferíveis (resolução de problemas, comunicação técnica, debugging) junto com o que aprendeu em programação.
- **Dor principal**: tudo que fez antes parece irrelevante para a vaga de dev. Não sabe como conectar os pontos.
- **Como o sistema ajuda**: a IA gera uma versão que reposiciona a experiência de suporte como contexto valioso e destaca skills técnicos e projetos.

### Persona C — Profissional Sênior

- **Nome**: Patrícia, 38 anos, 12 anos de experiência em TI, trabalho em empresas grandes e múltiplas certificações.
- **Contexto**: currículo tem 4 páginas, múltiplos cursos, certificações, idioma. Manda o mesmo currículo para tudo porque customizar leva 2 horas.
- **Necessidade**: selecionar e curar conteúdo para cada vaga, mantendo o currículo em 2 páginas sem perder o que importa.
- **Dor principal**: não sabe o que cortar. Vaga de liderança aparece e o currículo mostra demais (parece que ela está se candidatando a qualquer coisa); vaga de especialização técnica aparece e o currículo está genérico.
- **Como o sistema ajuda**: a IA filtra e prioriza conteúdo mais relevante para cada perfil de vaga, gerando versões curtas e específicas.

---

## Casos de Uso Principais

1. **CU-01**: O usuário cria uma conta e faz login no sistema.
2. **CU-02**: O usuário cadastra seu currículo base (formulário estruturado ou colagem de texto).
3. **CU-03**: O usuário lista seus currículos base, edita ou remove um.
4. **CU-04**: O usuário cria uma nova análise de vaga, colando a descrição completa da vaga.
5. **CU-05**: O usuário optionally informa empresa, título, localização, regime e senioridade da vaga.
6. **CU-06**: O usuário seleciona seu currículo base e inicia a geração otimizada via IA.
7. **CU-07**: O sistema exibe o currículo gerado junto com a análise de aderência.
8. **CU-08**: O usuário edita o currículo gerado manualmente e salva uma nova versão.
9. **CU-09**: O usuário baixa o currículo em formato .docx.
10. **CU-10**: O usuário consulta o histórico de currículos gerados, filtrando por empresa e data.

---

## Jornada do Usuário — Fluxo Completo

### Jornada 1: Novo Usuário Cria Primeiro Currículo Otimizado

1. Usuário acessa o site → tela de registro.
2. Preenche nome, email, senha → conta criada.
3. Redirect para dashboard → vazio, com CTA "Criar primeiro currículo".
4. Clica em "Criar currículo base" → formulário de currículo.
5. Preenche seções: dados pessoais, experiência, educação, skills, certificações.
6. Salva → currículo base criado, marcado como default.
7. Volta ao dashboard → vê currículo na lista.
8. Clica em "Nova análise" → seleciona currículo base.
9. Preenche dados da vaga: cola descrição da vaga, informa empresa e título.
10. Clica "Gerar currículo" → loading spinner.
11. Sistema chama IA → retorna currículo + análise.
12. Tela de resultado: currículo gerado + score de aderência + pontos fortes/lacunas.
13. Usuário revisa, clica "Baixar DOCX" → arquivo baixa.
14. Opcionalmente edita currículo → salva nova versão.

### Jornada 2: Usuário Retorna para Refinar Currículo Gerado

1. Login → dashboard.
2. Vê histórico: currículo para "Empresa X - Desenvolvedor Backend - 2 semanas atrás".
3. Clica no item → tela de detalhe.
4. Revisa o currículo gerado → identifica que quer mudar um parágrafo.
5. Clica "Editar" → editor abre com conteúdo.
6. Faz alteração → salva.
7. Nova versão criada no histórico.

### Jornada 3: Usuário Gera Currículo para Nova Vaga

1. Login → dashboard.
2. Já tem currículo base.
3. Clica "Nova análise" → seleciona currículo base (já tem default).
4. Cola descrição da vaga: "Empresa Y - Tech Lead - modelo híbrido SP".
5. Clica "Gerar currículo".
6. Recebe resultado → adherence score显示baixa (40%).
7. Sistema mostra lacunas: "Falta experiência com gestão de equipe".
8. Usuário edita currículo para contextualizar experiência de coordenação informal.
9. Salva nova versão.

---

## Requisitos Funcionais do MVP

### RF-01: Autenticação
- O sistema deve permitir registro com email e senha.
- O sistema deve permitir login com email e senha.
- O sistema deve emitir JWT com tempo de expiração de 1 hora.
- O sistema deve permitir logout (invalidação de token).
- O sistema deve manter sessão via httpOnly cookie ou localStorage.

### RF-02: Currículo Base
- O usuário deve poder criar um currículo base com título personalizado.
- O usuário deve poder preencher dados pessoais: nome, email, telefone, localização, LinkedIn, GitHub.
- O usuário deve poder adicionar múltiplas entradas de experiência profissional (empresa, cargo, período, descrição, conquistas).
- O usuário deve poder adicionar múltiplas entradas de formação acadêmica.
- O usuário deve poder listar skills técnicos e skills comportamentais.
- O usuário deve poder adicionar certificações.
- O usuário deve poder salvar o currículo; o sistema deve gerar automaticamente markdown e JSONB a partir dos campos.
- O usuário deve poder editar e excluir currículos base.

### RF-03: Vagas de Emprego
- O usuário deve poder criar uma vaga colando a descrição completa.
- O usuário deve poder informar empresa, título, localização, regime (CLT/PJ/freelancer), senioridade (júnior/pleno/sênior).
- O usuário deve poder listar, editar e excluir vagas.

### RF-04: Geração com IA
- O usuário deve poder selecionar um currículo base e uma vaga para gerar um currículo otimizado.
- O sistema deve extrair keywords da descrição da vaga.
- O sistema deve gerar uma versão do currículo com as keywords relevantes incorporadas naturalmente.
- O sistema deve gerar análise de aderência com score (0-100), nível (alta/média/baixa), pontos fortes, lacunas e mapa de keywords.
- O sistema deve salvar todo o conteúdo gerado no banco (texto, markdown, JSONB, análise).
- O sistema deve salvar o prompt usado e a resposta bruta da IA para auditoria.

### RF-05: Edição e Versionamento
- O usuário deve poder editar o currículo gerado em tela.
- Cada edição deve criar uma nova versão, sem sobrescrever a anterior.
- O sistema deve manter链 de versões (version_number + parent_version_id).
- O usuário deve poder consultar o histórico de versões de um currículo gerado.

### RF-06: Exportação
- O usuário deve poder baixar o currículo gerado em formato .docx.
- O .docx deve ser gerado sob demanda a partir do conteúdo salvo no banco.
- O .docx deve seguir template ATS-friendly (sem tabelas complexas, sem imagens, sem colunas).
- O nome do arquivo deve seguir o padrão: "[Nome]-otimizado-[Empresa]-[Data].docx".

### RF-07: Histórico
- O usuário deve poder listar todos os currículos gerados.
- A lista deve permitir filtro por empresa, título da vaga e data.
- Cada item deve mostrar: empresa, título, data de geração, score de aderência, número de versões.
- O usuário deve poder acessar qualquer versão de qualquer currículo gerado.

---

## Requisitos Não-Funcionais

### RNF-01: Performance
- Tempo de resposta da API (excluindo IA): < 200ms para operações de leitura, < 500ms para operações de escrita.
- Tempo de geração com IA: < 30 segundos (timeout configurável).
- Tempo de geração de .docx: < 3 segundos.

### RNF-02: Disponibilidade
- Sistema de autenticação: disponibilidade de 99%.
- API de geração: disponibilidade de 95% (IA pode ter downtime).

### RNF-03: Segurança
- Todas as senhashasheadas com BCrypt (cost factor >= 12).
- JWT com expiração curta (access: 1h, refresh: 7d).
- Todas as comunicações via HTTPS.
- Dados encriptados at rest (PostgreSQL com encryption configurada).
- Validação de input em todas as camadas (backend é a source of truth).

### RNF-04: Escalabilidade
- MVP dimensionado para centenas de usuários simultâneos.
- Banco dimensionado para milhares de currículos por usuário.
- IA: limitação de rate limit por provedor respeitada.

### RNF-05: Manutenibilidade
- Código documentado com Javadoc/Springdoc.
- Testes unitários para lógica de domínio.
- Logs estruturados para debugging.

---

## Limites do MVP

O MVP é intencionalmente limitado para reduzir tempo de entrega e risco:

- **Single-user**: cada conta pertence a um único usuário; sem equipes, sem compartilhamento.
- **Sem multi-tenant**: um banco de dados por instância; sem isolamento entre "organizações".
- **Sem billing**: não há planos, limites de uso, nem cobrança.
- **Sem upload de arquivos**: o usuário digita ou cola texto; não faz upload de PDF ou DOCX existente.
- **Sem scraping**: o usuário cola a descrição da vaga manualmente; não há automação de coleta.
- **Sem pesquisa salarial**: funcionalidade documentada para fase futura.
- **Sem storage externo**: todo conteúdo no PostgreSQL; .docx gerado sob demanda.
- **Sem worker Python**: todo o processamento é feito em Java/Spring Boot no MVP.

---

## Funcionalidades Futuras (Fases 2-4)

### Fase 2 — Pesquisa Salarial e IA Avançada
- Pesquisa salarial com fontes brasileiras configuráveis.
- Cache de resultados de pesquisa.
- Relatório salarial separado do currículo.
- Few-shot examples no prompt de IA.
- Histórico de prompts otimizados.

### Fase 3 — Upload de Arquivos e Extração
- Upload de PDF de currículo existente → extração de texto.
- Upload de DOCX → extração de texto.
- Possível integração com storage (S3/MinIO) para arquivos grandes.
- Possível worker Python para extração de PDF (Apache Tika ou similar).
- Validação de schema JSONB para currículos estruturados.

### Fase 4 — SaaS Completo
- Multi-tenant com isolamento por organização.
- Planos e limites de uso (quantidade de gerações por mês).
- Cobrança via gateway de pagamento.
- Storage de arquivos (uploads de candidatos, uploads de vagas).
- Auditoria avançada de ações.
- Observabilidade (métricas, logs agregados).
- Templates de currículo customizáveis.
- Colaboração em equipe.
- Integrações com ATS externos (Rhizome, Gupy, etc.).