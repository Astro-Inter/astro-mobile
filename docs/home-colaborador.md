# Home do colaborador

A `EmployeeHomeFragment` solicita ao `EmployeeHomeViewModel` a consulta de
`GET /home/collaborator` ao entrar ou retornar à tela,
usando a URL configurada em `BuildConfig.API_BASE_URL`. O carregamento simulado e os
dados de demonstração da home foram removidos.

O `CollaboratorHomeRepository` obtém o ID token da sessão Firebase e envia
`Authorization: Bearer <token>`. Um HTTP 401 força a renovação do token e repete a
consulta uma única vez; outro 401 ou a ausência de sessão encerra o fluxo autenticado.
As requisições são canceladas ao pausar ou destruir a view e respostas antigas são
descartadas. Nenhum token é persistido pelo repositório.

## Contrato

Referência: [DTO da API](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/home/dto/response/CollaboratorHomeResponse.java)
e [controller](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/home/controller/HomeController.java).

O envelope deve conter `success: true` e `data` com:

| Campo | Exibição |
| --- | --- |
| `username` | Cumprimento |
| `workspace_name` | Nome do workspace |
| `events_metrics.pending` | Eventos pendentes |
| `events_metrics.under_analysis` | Eventos em análise |
| `events_metrics.done` | Eventos concluídos |
| `status_message` | Mensagem de situação: `URGENT`, `PENDENT`, `ANALYSIS` ou `OK` |
| `pending_events` | Título, prazo `limit_date` em `dd/MM/yyyy` e indicação de novo pelo campo `viewed` |
| `last_notifications` | Título, tempo `sent_at_in_hours` e indicação de nova pelo campo `viewed` |

Cada lista tem seu próprio estado vazio. Listas nulas são tratadas como vazias e
prazos ausentes ou inválidos mostram “Sem data limite”. O workspace e os contadores
continuam visíveis mesmo quando não há itens nos feeds.

## Carregamento e erros

O skeleton acompanha a requisição real. Erros de rede, servidor ou contrato inválido
mostram uma mensagem com **Tentar novamente**, sem exibir dados fictícios ou conteúdo
de uma consulta anterior. HTTP 403/404 mostra que a home não está disponível para a
conta; HTTP 503 usa a mensagem de falha no carregamento. O timeout permanece o do
cliente compartilhado, de 15 segundos.

Os atalhos de eventos e notificações continuam direcionando às listas existentes,
que ainda usam mocks. Esta integração não consulta detalhes nem marca itens como
visualizados: `viewed` é apenas lido da resposta da home.

## Validação

`CollaboratorHomeRepositoryTest` cobre o contrato e o header de autenticação, os quatro
status, listas vazias/nulas, renovação única após 401, sessão inválida, HTTP 403/404/503,
JSON inválido, falhas de transporte e descarte de consultas canceladas/substituídas.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain
```

Validação local em 18/10/2026: 38 testes unitários aprovados, incluindo os 11 testes
novos da home; APK debug compilado e lint sem erros (111 avisos no projeto).
Para executar neste ambiente Windows, foi necessário usar o JDK 25 instalado e
`-Djdk.net.unixdomain.tmpdir` apontando para `.gradle/socket-tmp` dentro do projeto,
aplicado somente aos processos de validação. Não houve alteração permanente no
Gradle ou no ambiente do usuário.

A consulta autenticada contra produção e a conferência visual em aparelho ainda
não foram realizadas.

## Revisão arquitetural das PRs #47 e #48 — SCRUM-582

As integrações iniciais usavam Repository e DTOs no lugar correto, mas as duas Homes
chamavam o Repository diretamente do Fragment. Isso não seguia o fluxo
`Fragment → ViewModel → Repository` usado pelo Perfil e pela autenticação.

O `EmployeeHomeViewModel`, em `employee/home`, passa a controlar os dados da consulta,
o carregamento e o cancelamento, com callbacks próprios e injeção manual pela Factory.
Bloqueia consultas simultâneas, libera o callback da View ao concluir/cancelar e limpa
os dados ao sair ou iniciar nova consulta. `onCleared` também cancela a operação.
O Fragment mantém Views, formatação, animações e navegação; `homeReady` controla somente
o dissolve. HTTP, token, renovação e validação do contrato continuam no Repository/DTO.

Não foram adicionados LiveData, novas dependências ou outra camada de rede.
Mantida a atualização ao retomar a tela, com cancelamento em `onPause`/`onDestroyView`.
Os testes do ViewModel cobrem carregamento, bloqueio de chamadas concorrentes, erro e
nova tentativa, dados antigos, cancelamento e limpeza ao remover o ViewModel.

Validação da correção arquitetural em 18/10/2026: `assembleDebug`, `lintDebug` e
`testDebugUnitTest` aprovados, com 62 testes (12 novos dos ViewModels), sem falhas.
Lint sem erros e com 111 avisos no projeto. Nenhuma instalação ou consulta autenticada
em produção executada nesta revisão.
