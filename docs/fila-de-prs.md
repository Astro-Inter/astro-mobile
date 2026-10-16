# Fila local de Pull Requests

Atualizada em 2026-10-16. PRs #35–42 integradas à main 911ab3d. A SCRUM-274 começa diretamente dessa main, sem empilhamento nas antigas branches de integração. Build/lint e 27 testes existentes aprovados; validação visual/GPS pendente devido ao erro de restauração da sessão no aparelho.

## Colaboradores

1. SCRUM-313 — lista de colaboradores: [PR #37](https://github.com/Astro-Inter/astro-mobile/pull/37), integrada à main em e2274d3.
2. SCRUM-311 — detalhe do funcionário: [PR #38](https://github.com/Astro-Inter/astro-mobile/pull/38), integrada à main em f66cab2.
3. SCRUM-309 — eventos do funcionário: [PR #39](https://github.com/Astro-Inter/astro-mobile/pull/39), integrada à main em fae427d.
4. SCRUM-301 — conversa gestor-colaborador: [PR #40](https://github.com/Astro-Inter/astro-mobile/pull/40), integrada à main em 46757b8.

As quatro PRs da sequência de colaboradores já estão na main. Manter os dois atalhos do detalhe do funcionário: Conversar e Ver pendências.

## Eventos — fila independente

1. SCRUM-307 — notificações: [PR #35](https://github.com/Astro-Inter/astro-mobile/pull/35), já integrada à `main`.
2. SCRUM-285 — atribuição de conclusão: [PR #36](https://github.com/Astro-Inter/astro-mobile/pull/36), integrada à main no merge ad0f6b4.
3. SCRUM-289 — validação de conclusão: alterações preservadas, ainda sem commit.
4. SCRUM-287 — visualização da evidência: integrada localmente à validação, ainda sem commit. Publicar depois da validação, mantendo cada tarefa separada.

Backup completo de SCRUM-289/287, incluindo novos arquivos: stash `1aba05a51ef51483d1190bffb54c39e968625ecf`. Backup isolado da SCRUM-289: `d631bc459e4a0a8fc792116ee24853835d0b3abb`. Não descartar nenhum antes de recuperar e conferir os diffs. A dependência exata da validação sobre a atribuição deve ser conferida no diff recuperado; a sequência acima é a ordem funcional recomendada, não uma afirmação de ancestralidade Git.

## Pendência antiga a revisar

Há um stash de SCRUM-307 (notificações) que precisa ser comparado à main antes de propor PR. Ele contém inclusive uma cópia de configuração Firebase; não aplicar nem publicar tudo indiscriminadamente. A existência do stash não prova que a tarefa ainda esteja ausente da main.

## Formulários — sequência local posterior à integração

- SCRUM-279: [PR #41](https://github.com/Astro-Inter/astro-mobile/pull/41), integrada à main em 60ab01b.
- SCRUM-277: [PR #42](https://github.com/Astro-Inter/astro-mobile/pull/42), integrada à main em 911ab3d. Snapshot anterior à separação preservado no stash `9f17a50db93ad30eced055eedf53b44d6a6b657c` e nas branches de backup correspondentes.
- SCRUM-274: branch `feat/SCRUM-274-responder-formulario-ui`, base main 911ab3d. UI e mocks, campos obrigatórios, GPS real antecipado da SCRUM-276 com referência inicial provisória e raio de 100 m, pausa/retomada. Evidências e conclusão locais; sem câmera ou API. Publicar em rascunho enquanto a validação visual/GPS não puder ser feita. A SCRUM-276 está parcialmente em andamento; não concluir o card de câmera/evidências só pelo GPS.

## Procedimento de publicação

- Antes de cada PR, conferir estado remoto, commits já mergeados, diff próprio e dependências. Esta fila é um retrato local, não substitui a consulta ao GitHub.
- Acompanhar uma PR por vez com o solicitante; não mergear uma filha antes da base.
- Em stacks, registrar o SHA antigo da base antes de rebase. Após squash/merge da base, preferir rebase `--onto` para retirar somente os commits herdados, sem duplicar alterações.
- Não reescrever branch de colega nem sobrescrever mudanças remotas. Push com histórico reescrito exige aprovação específica e conferência do remoto.
- Após cada merge confirmado, atualizar os cards correspondentes e conferir a sprint atual.
- O solicitante autorizou commits, push e abertura de PRs nesta sequência. Essa autorização não substitui as aprovações da equipe; não mergear nesta resolução.
