# Notificações — SCRUM-583

## Pendência de entrega — SCRUM-308

Em 20/10/2026, o solicitante pediu para aguardar seu teste manual antes de abrir a PR.
A implementação está na branch `feat/SCRUM-308-detalhe-leitura-notificacoes`, ainda sem
commit. Antes de enviar para a main: conferir o roteiro abaixo nos dois fluxos, registrar
o resultado e obter aprovação do solicitante; então preparar commit/PR e aguardar review/merge.
Não considerar essa integração entregue na main enquanto esses passos estiverem pendentes.

Complemento SCRUM-308 (20/10/2026): compilação debug, 78 testes unitários existentes e
lint aprovados, sem erros e com 111 avisos. Não foram criados testes novos. Rotas de
detalhe/leitura conferidas no backend em `e282d85`; o Swagger já consultado anteriormente
excedeu o timeout nas novas tentativas. Integração autenticada e conferência visual no
aparelho ainda não executadas; não considerar os testes da listagem como cobertura
específica das novas operações.

A tela compartilhada por Colaborador e Gestor consulta `GET /notifications` com o ID token
Firebase da sessão em `Authorization: Bearer <token>`. Não envia ID de usuário nem de workspace.
O contrato foi conferido no [Swagger publicado](https://astro-api-h2hx.onrender.com/swagger-ui/index.html)
e no [módulo `notification` da API](https://github.com/Astro-Inter/astro-api/tree/main/src/main/java/com/astro/api/notification)
em 18/10/2026.

```json
{
  "success": true,
  "data": [
    { "id": "mongo-id", "title": "Lembrete de treinamento", "viewed": false, "hours": 2 }
  ]
}
```

`NotificationsRepository` usa Retrofit e valida o envelope e os itens. `NotificationsViewModel`
mantém a consulta, o estado de carregamento, o texto pesquisado e a filtragem por título;
o Fragment renderiza os callbacks e a lista devolvida por `visibleNotifications()`.
A lista mantém a ordem recebida, do mais recente ao mais antigo. O ponto de nova notificação
aparece quando `viewed` é falso; `hours = 0` aparece como “Há menos de 1h”. A busca local
por título ignora maiúsculas e acentos. Uma lista vazia ou busca sem resultados mostra
“Nenhuma notificação encontrada”.

O skeleton acompanha a consulta real. Em 401, o token é renovado uma única vez; outro 401
encerra a sessão pelo fluxo existente. Erros de conexão, acesso (403/404), servidor ou
resposta inválida mostram mensagem e botão “Tentar novamente”. Ao sair ou pausar, a consulta
é cancelada, os dados são descartados e callbacks antigos são ignorados. Ao voltar, a tela
consulta novamente, preservando a pesquisa aplicada.

## Detalhe e leitura — SCRUM-308

O toque consulta `GET /notifications/{notificationId}` com o token Firebase. O painel
existente mostra loading real, título e `message`; `title` ausente usa o título da lista.
A API também fornece `eventId`: quando presente, o botão Visualizar evento abre o detalhe
do fluxo ativo, com os dados mockados já usados pelo protótipo e o ID real preservado nos
argumentos. Sem `eventId`, não há botão. O detalhe real ainda depende da integração própria.
O conteúdo abre em popup centralizado conforme os frames 2590:12312/2593:17018 do Figma,
com largura máxima de 370dp e rolagem para mensagens longas. A seção resumida da Home
continua vindo do endpoint da própria Home.

Depois que a mensagem carregar, se o item ainda não foi lido, o app chama
`PATCH /notifications/{notificationId}/viewed`, sem body. Somente HTTP 200 com
`success=true` remove a bolinha local; `data=null` é válido nessa operação idempotente.
Abrir a lista ou falhar no GET não marca leitura. Uma falha no PATCH mantém a mensagem
aberta e permite repetir somente a marcação. A busca e os demais itens são preservados.

Ambas as operações renovam o token uma vez em 401; outro 401 encerra a sessão.
403/404 mostram erro de acesso, falta de conexão mostra a mensagem correspondente,
timeout/5xx/envelope inválido mostram erro da operação, com nova tentativa no painel.
Fechar o painel, abrir outro item ou sair da tela cancela/desconsidera a operação anterior.
Nenhuma chamada privilegiada ou de push foi adicionada.

## Validação manual pendente

- Entrar como Colaborador e Gestor, abrir Notificações pelos atalhos da Home e conferir os dados da conta.
- Conferir ordem, textos, horas, ponto de não lida e detalhe do item tocado.
- Buscar com e sem acentos, limpar a busca e conferir o estado sem resultados.
- Conferir conta sem notificações, erro de conexão e nova tentativa.
- Sair durante o carregamento e voltar; conferir que respostas antigas não aparecem.
- Sair da conta, entrar com outra e conferir o isolamento dos dados.
- Abrir uma não lida: conferir mensagem real e bolinha removida só depois do PATCH.
- Reabrir uma lida: conferir detalhe sem novo PATCH.
- Interromper rede durante GET e PATCH; repetir sem perder lista ou texto pesquisado.
- Fechar durante loading e abrir outro item: nenhuma resposta pode preencher o item errado.

Testes locais cobrem o contrato HTTP, autenticação, token renovado, lista vazia, falhas,
cancelamento e ciclo de vida do ViewModel. A validação visual e autenticada em aparelho
permanece pendente.

Build debug, lint e 75 testes unitários aprovados na integração inicial, incluindo 13 testes novos de notificações.
Lint sem erros, com 111 avisos. Para executar o Gradle neste ambiente Windows, foi usado
`-Djdk.net.unixdomain.tmpdir` apontando para `.gradle/notification-tmp` apenas no processo
de validação, contornando a falha de socket no diretório temporário do sandbox.

## Ajuste de arquitetura

A busca segue a mesma divisão de `StaffViewModel.visibleMembers()` e
`ManagerEventsViewModel.visibleEvents()`: normalização de acentos/maiúsculas, tratamento
de espaços e seleção dos resultados ficam no ViewModel. O Fragment apenas encaminha
o texto digitado e apresenta os itens e o estado vazio. A pesquisa continua aplicada
ao digitar cada caractere, sem nova chamada HTTP e preservando a ordem da API.
O botão Pesquisar foi removido dos dois fluxos conforme o Figma; a ação do teclado
apenas recolhe o teclado, sem ser necessária para pesquisar.
O fluxo de consulta mantém `Fragment → ViewModel → Repository → AstroApi`, com callbacks,
Factory e cancelamento como nas Homes. Os testes adicionais verificam busca, limpeza,
ausência de resultados e aplicação da pesquisa a uma nova resposta após cancelamento.

Validação do ajuste: build debug, lint e 78 testes unitários aprovados (16 de notificações,
incluindo os três novos testes de busca), sem falhas. Lint com 0 erros e 111 avisos.
A validação autenticada e visual em aparelho permanece pendente.
