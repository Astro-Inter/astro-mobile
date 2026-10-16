# Lista de colaboradores — SCRUM-313

UI e mocks da subtarefa de [SCRUM-256](https://luizacursino.atlassian.net/browse/SCRUM-256), com referência no [App V2 — 2590:12557](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12557).

## Escopo

- Cabeçalho `Colaboradores`, seta de voltar e X com área de toque/ripple circular.
- Pesquisa local por nome ou cargo, combinada aos filtros `Todos`, `Com pendências` e `Sem pendências`.
- Três exemplos do Figma: Kevin Parker/Eletricista/10 pendências; Kevin Parker/Analista de dados/10 pendências; Kevin Parker/Tech Lead/sem pendências. Os nomes repetidos são intencionais, conforme o design.
- Skeleton mock de 1 segundo aprovado pelo solicitante, oculto na prévia XML, com cancelamento ao sair. O ViewModel guarda pesquisa, filtro e carregamento enquanto a tela permanece no histórico.
- Acesso pelo atalho de colaboradores da Home do Gestor; transição horizontal padrão. Navbar única na Activity, fixa durante o push; nenhuma aba é destacada nesta tela, conforme o frame.
- Estado vazio com texto provisório `Nenhum colaborador encontrado` para permitir conferir a pesquisa sem resultados. Não é uma regra ou resposta do backend.

## Como conferir

1. Entre no fluxo Gestor e toque no atalho de colaboradores na Home.
2. Após um segundo, `Todos` mostra os três exemplos; `Com pendências` mostra dois e `Sem pendências` mostra um.
3. Pesquise `eletricista`, `analista` ou `tech`, com diferenças de caixa/acentuação, para conferir o filtro de cargo. `Kevin` encontra os três; um termo inexistente mostra o estado vazio. Limpar a pesquisa mantém o filtro selecionado.
4. A ação de pesquisa do teclado apenas fecha o teclado; não faz requisição.
5. Voltar preserva a Home. O X retorna diretamente à Home do Gestor.
6. Na branch empilhada da SCRUM-311, tocar em um exemplo abre os detalhes do funcionário com a identidade do mock escolhido.

## Fidelidade e recursos

Reutilizados retratos `participant_avatar_1/2/3`, lupa `manager_events_search`, seta, X, cores, filtros e divider canônicos. Os três retratos têm hashes idênticos aos exports do Figma; a lupa renderizada do SVG tem pixels idênticos ao PNG existente. Nenhum bitmap, ícone, fonte ou dependência foi adicionado.

Retratos em 44dp, lupa na geometria de raiz do slot de 18dp, título em 24sp, nome em 16sp e metadados em 12sp. As colunas usam pesos em vez das larguras fixas que não caberiam em telas menores; há uma reserva de 38dp à direita, vazia como no componente. Filtros podem crescer em altura para não cortar texto em telas estreitas. Navbar fora do RecyclerView.

O placeholder `Pesquisar em participantes` foi mantido literalmente como está no Figma, reutilizando a string existente; sua revisão editorial fica pendente no design. O estado vazio é provisório. Não foram adicionados chevrons ausentes no frame.

O contexto completo e o screenshot do Figma foram obtidos na consulta anterior. Na retomada, a nova consulta demorou a responder e foi interrompida; a implementação usou o contexto de alta fidelidade já salvo, não valores deduzidos de uma imagem.

## Organização e limites

Classes em `manager/staff`; `StaffViewModel` guarda estado/filtros, `StaffAdapter` preenche os itens e `StaffSamples` isola os mocks. Sem LiveData, Safe Args, nova Activity, Repository vazio, API ou Firebase. Contagens e cargos são apenas dados de exemplo, sem aplicar regras de domínio do backend.

O detalhe de funcionário foi ligado na SCRUM-311, subtarefa de SCRUM-257, em branch empilhada; veja `docs/detalhes-funcionario.md`. Os eventos desse funcionário pertencem à SCRUM-258. Integração da listagem na SCRUM-314.

## Git

Branch `feat/SCRUM-313-lista-colaboradores-ui`, criada diretamente da main atualizada `293e7b2`, independente das tarefas de validação/evidência.

As SCRUM-289/287 continuam sem commit e foram guardadas integralmente no stash `1aba05a51ef51483d1190bffb54c39e968625ecf`, incluindo os arquivos novos. O backup isolado anterior da SCRUM-289 (`d631bc459e4a0a8fc792116ee24853835d0b3abb`) também foi mantido. Commit local da SCRUM-313 autorizado pelo solicitante em 15/10/2026; sem push ou PR. Ordem de publicação registrada em `docs/fila-de-prs.md`.

## Verificação

`assembleDebug` e `lintDebug` aprovados na verificação final, sem erros; plural da contagem utiliza o recurso nativo de quantidade. `git diff --check` sem problemas de whitespace. Sem físico conectado: instalação/navegação não realizadas. Previews de telefone e tablet não conferidos; emulador não iniciado.
