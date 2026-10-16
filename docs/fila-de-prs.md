# Fila local de Pull Requests

Atualizada em 2026-10-15. Registro para retomar a publicação sem depender do histórico do chat. Não significa que houve push, abertura de PR ou merge.

## Colaboradores

1. SCRUM-313 — lista de colaboradores: branch `feat/SCRUM-313-lista-colaboradores-ui`, base `main` em `293e7b2`. UI implementada; build/lint aprovados, conferência visual pendente. Commit local autorizado pelo solicitante em 15/10.
2. SCRUM-311 — detalhes do funcionário (pai SCRUM-257): próxima UI escolhida, ainda não implementada. Criar branch empilhada na SCRUM-313 para ligar a abertura pela lista. Não incluir API da SCRUM-312.

Publicar/mergear a lista antes dos detalhes. Após o merge da lista, atualizar `origin/main` e reaplicar somente os commits próprios dos detalhes sobre a main, conferindo o diff antes de trocar a base da PR.

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
