# Fluxo de publicação do Astro Mobile

O estado de PRs e merges deve ser conferido no GitHub. Este arquivo descreve o procedimento; não mantém uma fila histórica de branches ou stashes.

1. Partir da main atualizada ou declarar explicitamente a base quando a tarefa for empilhada.
2. Revisar o diff e executar as verificações pertinentes antes de publicar.
3. Conferir conflitos com a base e resolvê-los preservando os dois lados do trabalho.
4. Publicar somente com autorização e informar dependências e QA pendente na descrição.
5. Após o merge da base, atualizar a branch dependente e conferir novamente o diff e os conflitos.

O estado de implementação das telas está em [SCREEN_STATUS.md](../SCREEN_STATUS.md). A fila de QA precisa distinguir código implementado de integração validada em aparelho.
