# Notificações — SCRUM-583

A tela compartilhada por Colaborador e Gestor consulta `GET /notifications` com o ID token
Firebase da sessão em `Authorization: Bearer <token>`. Não envia ID de usuário nem de workspace.
O contrato foi conferido no [Swagger publicado](https://astro-api-h2hx.onrender.com/swagger-ui/index.html)
e no [módulo `notification` da API](https://github.com/Astro-Inter/astro-api/tree/main/src/main/java/com/astro/api/notification)
em 18/10/2026.

```json
{
  "success": true,
  "data": [
    { "id": "mongo-id", "titulo": "Lembrete de treinamento", "viewed": false, "horas": 2 }
  ]
}
```

`NotificationsRepository` usa Retrofit e valida o envelope e os itens. `NotificationsViewModel`
mantém a consulta, o estado de carregamento, o texto pesquisado e a filtragem por título;
o Fragment renderiza os callbacks e a lista devolvida por `visibleNotifications()`.
A lista mantém a ordem recebida, do mais recente ao mais antigo. O ponto de nova notificação
aparece quando `viewed` é falso; `horas = 0` aparece como “Há menos de 1h”. A busca local
por título ignora maiúsculas e acentos. Uma lista vazia ou busca sem resultados mostra
“Nenhuma notificação encontrada”.

O skeleton acompanha a consulta real. Em 401, o token é renovado uma única vez; outro 401
encerra a sessão pelo fluxo existente. Erros de conexão, acesso (403/404), servidor ou
resposta inválida mostram mensagem e botão “Tentar novamente”. Ao sair ou pausar, a consulta
é cancelada, os dados são descartados e callbacks antigos são ignorados. Ao voltar, a tela
consulta novamente, preservando a pesquisa aplicada.

O toque abre o título e o tempo do item selecionado. A listagem não fornece corpo nem ID de
evento: a descrição de exemplo foi removida e o botão de evento fica oculto. Consultar ou
abrir a lista não marca itens como lidos; `PATCH /notifications/{id}/viewed` não faz parte
desta integração. A seção resumida da Home continua vindo do endpoint da própria Home.

## Validação manual pendente

- Entrar como Colaborador e Gestor, abrir Notificações pelos atalhos da Home e conferir os dados da conta.
- Conferir ordem, textos, horas, ponto de não lida e detalhe do item tocado.
- Buscar com e sem acentos, limpar a busca e conferir o estado sem resultados.
- Conferir conta sem notificações, erro de conexão e nova tentativa.
- Sair durante o carregamento e voltar; conferir que respostas antigas não aparecem.
- Sair da conta, entrar com outra e conferir o isolamento dos dados.

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
ao acionar o botão ou o teclado, sem nova chamada HTTP e preservando a ordem da API.
O fluxo de consulta mantém `Fragment → ViewModel → Repository → AstroApi`, com callbacks,
Factory e cancelamento como nas Homes. Os testes adicionais verificam busca, limpeza,
ausência de resultados e aplicação da pesquisa a uma nova resposta após cancelamento.

Validação do ajuste: build debug, lint e 78 testes unitários aprovados (16 de notificações,
incluindo os três novos testes de busca), sem falhas. Lint com 0 erros e 111 avisos.
A validação autenticada e visual em aparelho permanece pendente.
