# Fila local de Pull Requests

Atualizada em 2026-10-16. PRs #35–39 integradas à main fae427d. PR #40 integra essa base, preservando as duas ações do detalhe do funcionário; build/lint sem erros (99 avisos). Aprovação e merge continuam separados desta resolução.

## Colaboradores

1. SCRUM-313 — lista de colaboradores: [PR #37](https://github.com/Astro-Inter/astro-mobile/pull/37), integrada à main em e2274d3.
2. SCRUM-311 — detalhe do funcionário: [PR #38](https://github.com/Astro-Inter/astro-mobile/pull/38), integrada à main em f66cab2.
3. SCRUM-309 — eventos do funcionário: [PR #39](https://github.com/Astro-Inter/astro-mobile/pull/39), integrada à main em fae427d.
4. SCRUM-301 — conversa entre gestor e colaborador: [PR #40](https://github.com/Astro-Inter/astro-mobile/pull/40); integra a main fae427d e passa a ter base main. Manter Conversar e Ver pendências no detalhe, os dois destinos e o tratamento da navbar/teclado. Sem API nesta etapa.

As branches SCRUM-309 e SCRUM-301 nasceram como irmãs da SCRUM-311; após os merges anteriores, a PR #40 deve comparar somente a conversa humana com a main atual.

## Eventos — fila independente

1. SCRUM-285 — atribuição de conclusão: PR #36 integrada à main em ad0f6b4.
2. SCRUM-289 — validação de conclusão: alterações preservadas, ainda sem commit.
3. SCRUM-287 — visualização da evidência: integrada localmente à validação, ainda sem commit. Publicar depois da validação, mantendo cada tarefa separada.

Backup completo de SCRUM-289/287, incluindo novos arquivos: stash `1aba05a51ef51483d1190bffb54c39e968625ecf`. Backup isolado da SCRUM-289: `d631bc459e4a0a8fc792116ee24853835d0b3abb`. Não descartar nenhum antes de recuperar e conferir os diffs. A dependência exata da validação sobre a atribuição deve ser conferida no diff recuperado; a sequência acima é a ordem funcional recomendada, não uma afirmação de ancestralidade Git.

## Pendência antiga a revisar

Há um stash de SCRUM-307 (notificações) que precisa ser comparado à main antes de propor PR. Ele contém inclusive uma cópia de configuração Firebase; não aplicar nem publicar tudo indiscriminadamente. A existência do stash não prova que a tarefa ainda esteja ausente da main.

## Procedimento de publicação

- Antes de cada PR, conferir estado remoto, commits já mergeados, diff próprio e dependências. Esta fila é um retrato local, não substitui a consulta ao GitHub.
- Acompanhar uma PR por vez com o solicitante; não mergear uma filha antes da base.
- Em stacks, registrar o SHA antigo da base antes de rebase. Após squash/merge da base, preferir rebase `--onto` para retirar somente os commits herdados, sem duplicar alterações.
- Não reescrever branch de colega nem sobrescrever mudanças remotas. Push com histórico reescrito exige aprovação específica e conferência do remoto.
- Após cada merge confirmado, atualizar os cards correspondentes e conferir a sprint atual.
- O solicitante autorizou commits, push e abertura de PRs nesta sequência. Essa autorização não substitui as aprovações da equipe; não mergear nesta resolução.
