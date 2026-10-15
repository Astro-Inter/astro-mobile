# Fila local de Pull Requests

Atualizada em 2026-10-15. Registro para retomar a publicação sem depender do histórico do chat. Não significa que houve push, abertura de PR ou merge.

## Colaboradores

1. SCRUM-313 — lista de colaboradores: branch `feat/SCRUM-313-lista-colaboradores-ui`, base `main` em `293e7b2`. UI implementada; build/lint aprovados, conferência visual pendente. Commit local autorizado pelo solicitante em 15/10.
2. SCRUM-311 — detalhes do funcionário (pai SCRUM-257): branch `feat/SCRUM-311-detalhes-funcionario-ui`, empilhada na SCRUM-313 em `2cee85c`, commit local `e888e83`. Abertura pela lista e dados locais; sem API da SCRUM-312. Sem push ou PR.
3. SCRUM-309 — eventos do funcionário selecionado (pai SCRUM-258): branch `feat/SCRUM-309-eventos-funcionario-ui` sobre a SCRUM-311 em `e888e83`. UI, três exemplos, filtro por estado e skeleton de 1s; sem API da SCRUM-310. Build/lint aprovados; commit local autorizado em 15/10/2026 para iniciar a SCRUM-301 como irmã sobre a SCRUM-311. Sem push ou PR. Conferência visual pendente.

Publicar/mergear na ordem **SCRUM-313 → SCRUM-311 → SCRUM-309**. Após cada merge, atualizar `origin/main` e reaplicar somente os commits próprios da próxima branch sobre a main, conferindo o diff antes de trocar a base da PR. Registrar o SHA antigo de cada base; não mover todas as branches de uma vez.

## Eventos — fila independente

1. SCRUM-285 — atribuição de conclusão: branch `feat/SCRUM-285-atribuicao-conclusao-ui`, commit local `85ba3e9`, ainda fora da main local consultada.
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
- A autorização deste turno é para o commit local da SCRUM-313. Não inclui push, abertura de PR ou merge. Pedir autorização para essas ações quando chegarmos à publicação.
