# Chat entre gestor e colaborador — SCRUM-261

## Base e escopo

A branch `feat/SCRUM-261-conversa-gestor-colaborador` parte da main `5f84070`.
O Git não aceita `[` em nomes de refs; por isso os colchetes solicitados foram
removidos. A main já continha o `minSdk = 31` que existia como alteração local.
Uma cópia dessa alteração foi preservada no stash `preservar ajuste local
minSdk antes de SCRUM-261`; ela não precisa ser reaplicada.

Foram consultados [SCRUM-261](https://luizacursino.atlassian.net/browse/SCRUM-261),
[SCRUM-301](https://luizacursino.atlassian.net/browse/SCRUM-301),
[SCRUM-302](https://luizacursino.atlassian.net/browse/SCRUM-302) e
[SCRUM-303](https://luizacursino.atlassian.net/browse/SCRUM-303).
A main já incluía a lista real (SCRUM-305) e o histórico/leitura compartilhados
(commit `e2ff09b`, PR #73). Os contatos de nova conversa e o envio ainda eram
locais ou bloqueados.

As descrições atuais de SCRUM-302/303 limitam o trabalho ao histórico e dizem
que o envio ainda não possui endpoint. O backend atual já oferece envio STOMP.
A orientação mais recente do solicitante autoriza esta integração. As descrições
do Jira precisam ser atualizadas pelo responsável para refletir essa ampliação;
este trabalho não altera status ou critérios no Jira automaticamente.

## Contrato consultado

Fontes no [astro-api](https://github.com/Astro-Inter/astro-api):

- [Documentação do chat](https://github.com/Astro-Inter/astro-api/blob/main/docs/api-chat.md).
- [Configuração WebSocket](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/config/ChatWebSocketConfig.java).
- [Segurança HTTP](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/auth/security/SecurityConfig.java).
- [Autenticação CONNECT](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/security/ChatStompAuthInterceptor.java).
- [Controller e erros STOMP](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/controller/ChatStompController.java).
- [DTO de envio](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/dto/request/ChatSendRequest.java), [confirmação](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/dto/response/ChatSendResponse.java) e [erro](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/dto/response/ChatSendErrorResponse.java).
- [Regras, persistência e deduplicação](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/service/ChatService.java).
- [Contatos e endpoints REST](https://github.com/Astro-Inter/astro-api/blob/main/src/main/java/com/astro/api/chat/controller/ChatController.java).

O servidor oferece WebSocket nativo em `/ws`, sem `withSockJS()`. Aceita Android
sem Origin; origens de navegador continuam restritas à configuração do servidor.
Não foi necessário alterar o backend nem enviar um Origin de navegador.

O Firebase ID token vai somente no cabeçalho `Authorization: Bearer ...` do
CONNECT STOMP. Os destinos são `/app/chat.send`, `/user/queue/messages` e
`/user/queue/errors`. O envio contém `content`, `clientMessageId` UUID e exatamente
um entre `recipientId` numérico ou `conversationId` UUID. O cliente não fornece
remetente, workspace ou timestamp. A ação Responder envia `replyToMessageId`;
o backend exige que a referência pertença à mesma conversa.

O servidor permite COLABORADOR → GESTOR/GESTOR_WORKSPACE e os três tipos
operacionais como destinatários de gestores. Ambos precisam estar ativos,
com unidade ativa e no mesmo workspace. A API permanece responsável pela
autorização; o fluxo visual não substitui essa verificação.

## Organização dos arquivos

| Camada/arquivos | Responsabilidade |
| --- | --- |
| `app/build.gradle.kts` | URLs HTTPS da API Java e IA; URL WSS independente. O worker de recuperação de senha mantém sua configuração própria. |
| `data/api/dto/ChatContactData`, `ChatSendRequest`, `ChatSendResponse`, `ChatSendError` | Modelos de transporte separados, conforme os DTOs reais. |
| `data/chat/StompFrame` | Codec STOMP 1.2 com escapes, UTF-8, content-length, frames parciais e heartbeats. |
| `data/chat/StompChatClient`, `ChatTransport`, `ChatScheduler`, `ChatConnectionState` | Sessão reutilizável e testável, duas inscrições, negociação de heartbeats e estados. |
| `data/chat/AndroidChatConnection` | OkHttp já disponível no projeto, Firebase, fila principal, rede e ciclo de vida. |
| `data/api/AstroApi`, `HumanChatContactsRepository` | `GET /chats/contacts`, Bearer, renovação após 401 e cancelamento de respostas antigas. |
| `HumanChatHistoryRepository`, `ChatMessageData` | Reuso de histórico/leitura e validação de datas; marcação de leitura independente durante recebimento. |
| `chat/human/HumanConversation`, `HumanMessage`, `HumanPendingSend` | Estado de apresentação, deduplicação e operação pendente estável. |
| `HumanConversationsViewModel` e sua `Factory` | Coordenação dos repositórios, estado observável, rascunhos, confirmação, histórico e limpeza de sessão. |
| `ConversationsFragment`, `HumanChatFragment`, adapters e recursos XML | Lista/contatos reais, indicação de conexão e envio, manutenção do rascunho e tentativa manual. |
| `MainActivity`, `StaffDetailsFragment`, `ErrorFragment`, manifesto | Primeiro/segundo plano, identidade real ao abrir pelo gestor, retry e permissão de rede. |

Os dois fluxos usam a mesma implementação. O gestor abre a conversa existente,
um contato autorizado ou o colaborador pelos detalhes. No fluxo do colaborador,
a seleção de contatos apresenta gestores. Nomes não são usados como identidade.
Nenhuma abertura de tela cria uma conversa no servidor; a criação ocorre no
primeiro envio confirmado.

## Comportamento de conexão e envio

- Uma instância por ViewModel da Activity, sem conexões ou inscrições adicionais
  ao recriar Fragments. O transporte usa a validação TLS padrão do OkHttp.
- Token atualizado antes de cada nova conexão, STOMP 1.2 e negociação
  `heart-beat:0,10000`: recebe heartbeats STOMP de 10s e verifica o transporte de
  saída com ping/pong WebSocket de 10s do OkHttp. O interceptor atual do servidor
  rejeita os heartbeats STOMP enviados pelo cliente; veja a pendência abaixo.
- Reconexão após 1, 2, 4, 8, 16 e 30 segundos; após seis tentativas sem sucesso,
  a tela oferece tentativa manual. Cada reconexão recria as duas inscrições.
- App em segundo plano ou rede sem validação encerra a sessão. Ao voltar à rede
  ou ao primeiro plano, o app conecta novamente e recupera lista/histórico REST.
- Logout ou mudança de identidade encerra inscrições e transporte, cancela
  consultas/timers e apaga mensagens, rascunhos e operações da sessão anterior.
- O balão enviado aparece apenas após a confirmação privada do servidor.
  O status normal de conexão fica oculto; avisos de conexão, falha e envio
  pendente aparecem somente enquanto necessários, sem reservar espaço no estado normal.
  A confirmação comprova persistência, contagem e publicação no backend; não é
  um comprovante de recebimento no outro aparelho ou de leitura.
- Confirmação ausente por 20s, desconexão ou erros de infraestrutura deixam a
  operação incerta. A seta permite repetir manualmente a mesma operação com
  `clientMessageId`, conteúdo e destinatário originais. Não existe reenvio automático.
- O backend deduplica por remetente e `clientMessageId`. Eventos repetidos ou
  sobrepostos ao histórico são deduplicados pelo `id` devolvido pelo servidor.
- `PATCH /chats/{id}/read` só zera o contador após 204. A paginação continua com
  limite 30 e cursor opaco, sem sintetizar mensagens da prévia da lista.
- Erros correlacionados são tratados por código. Sessão Firebase inválida e
  `UNAUTHENTICATED` seguem o logout existente. Um frame STOMP ERROR é tratado como
  conexão recusada: o backend não publica código estável que permita distinguir
  autenticação de falha técnica nesse frame. O app não classifica todos esses
  casos como token expirado.
- Falhas de certificado possuem estado próprio, sem aceitar certificados
  arbitrários. Inspeção HTTPS Cisco Umbrella deve ser corrigida na rede ou na
  distribuição da raiz confiável, mantendo a validação TLS do aplicativo.

## Verificação reproduzível

Use a configuração Firebase real local, que permanece ignorada pelo Git.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

No Windows com JDK 25, caso ocorra `Unable to establish loopback connection`
originado de `UnixDomainSockets`, foi necessário encurtar o caminho temporário:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir="C:/Android Studio/astro-mobile/.gradle/tmp"'
```

Os testes unitários cobrem CONNECT autenticado, inscrições únicas, DTOs e filas,
reconexão com token renovado, espera progressiva, heartbeats, interrupção no
logout, respostas tardias, certificado, codec, operação pendente, deduplicação,
histórico com limite/cursor e confirmação de leitura.

`HumanChatDeviceTest` valida conexão autenticada e retorno do segundo plano com
a sessão já existente no aparelho, sem enviar mensagens. Aguarda 32s conectado
para atravessar três ciclos de heartbeat.

`HumanChatUiDeviceTest` abre o histórico real já criado pelas contas de teste,
verifica os controles da tela, preserva o rascunho durante a recriação da
Activity e a ida/volta do segundo plano. Não publica mensagens.

`HumanChatEndToEndTest` é opt-in. Ele usa duas instâncias Firebase nomeadas,
preserva a sessão padrão do aparelho e persiste três mensagens de teste.
As contas precisam estar autorizadas mutuamente nos contatos. Os IDs vêm do
principal autenticado no CONNECTED (cabeçalho `user-name` do Spring), e são
conferidos com as listas retornadas antes de qualquer envio.

Para executar, crie localmente `.gradle/chat-e2e-credentials.properties` com
`first.email`, `first.password`, `second.email` e `second.password`. Não versione
esse arquivo, não coloque senhas em argumentos do runner e não registre tokens.
Instale os dois APKs, copie o arquivo para `files/chat-e2e.properties` usando
`adb shell run-as com.example.astro_mobile`, e remova a cópia intermediária e o
arquivo local. Então execute:

```powershell
adb shell am instrument -w -e class com.example.astro_mobile.chat.HumanChatEndToEndTest com.example.astro_mobile.test/androidx.test.runner.AndroidJUnitRunner
```

O teste apaga o arquivo privado no `finally`, faz logout das sessões de teste e
encerra suas conexões. A sessão padrão do aplicativo permanece preservada.
Não há endpoint de exclusão de mensagens no contrato usado; as mensagens de
teste continuam no histórico, identificadas pelo prefixo `[teste mobile]`.

Para conferir pela interface em dois aparelhos, entre com as duas contas no
mesmo workspace, abra Chat → Nova conversa e selecione o outro usuário. Envie
uma mensagem em cada sentido, confirme os balões, volte à lista e verifique
prévia e contador. Coloque um app em segundo plano, envie pelo outro, retorne e
confira a recuperação. Teste logout e novo login, verificando que rascunhos da
conta anterior desapareceram. Faça a verificação também em rede sem inspeção
HTTPS; não desative TLS para contornar falhas de rede.

## Limites e pendências

Anexos e comprovantes individuais de leitura não fazem
parte da interface/contrato implementados aqui. Rascunhos e operações pendentes
vivem no ViewModel: o encerramento do processo não recupera uma operação incerta.
Após reiniciar, consulte o histórico antes de digitar uma nova cópia do envio.

O backend exige MongoDB com transações e Redis/PubSub funcionando. Confirmação
de infraestrutura e a corrida conhecida do contador simples de não lidas
continuam sendo responsabilidades do backend descritas em `docs/api-chat.md`.
A alternância física entre Wi-Fi/rede móvel e o comportamento durante longos
períodos de Doze precisam de validação adicional em dispositivos/redes alvo.

### Heartbeat do servidor: divergência confirmada

O teste de 32s no M31 reproduziu `CONNECTED → REJECTED` usando o CONNECT
`heart-beat:10000,10000`. No backend consultado, `ChatStompAuthInterceptor.preSend`
rejeita `accessor.getCommand() == null` antes de distinguir o tipo HEARTBEAT.
O Spring representa o heartbeat STOMP `\n` justamente sem comando. Isso torna
incompatíveis a configuração anunciada e o interceptor quando o cliente envia
heartbeats STOMP. O teste inicial curto não atravessava esse intervalo.

O mobile negocia explicitamente `0,10000` em conformidade com STOMP 1.2 e usa
ping/pong WebSocket nativo de 10s para a saída. Não envia mensagens artificiais,
ACKs fictícios, assinaturas extras nem modifica a autorização para manter a conexão.
Uma falha de pong é reportada pelo OkHttp e segue a reconexão normal.

Correção recomendada no backend: reconhecer `SimpMessageType.HEARTBEAT`, exigir
o `ChatPrincipal` autenticado e aceitar o frame antes da validação genérica de
comando nulo. Frames comuns sem comando e heartbeats sem sessão autenticada
devem continuar recusados. Acrescentar testes do interceptor para os dois casos
e um teste de sessão com duração superior a três ciclos. Depois de publicado,
o mobile pode voltar a oferecer `10000,10000`. Esta entrega não altera astro-api.

## Resultado da execução local

### Hierarquia e respostas citadas

Referências visuais consultadas: Figma `3272:11287` (gestor) e `3326:5594`
(colaborador), no arquivo `qdCrRXAVuiNhNkBuim95zz`. Foram preservados a paleta
`#1c1839`/`#362361`/`#5b4a82`, Montserrat, cantos de 16dp e fotos dinâmicas.
O chat humano passou a usar `item_human_message.xml`, com texto e horário em
linhas distintas, alinhamento por remetente e espaçamento reduzido entre
mensagens consecutivas do mesmo autor. O layout da IA permanece independente.

Toque ou pressione um balão para abrir Responder/Copiar texto. A resposta
selecionada aparece acima do campo, pode ser cancelada e é mantida enquanto o
envio aguarda confirmação. O payload mantém a mesma referência nas tentativas
manuais. Histórico e eventos STOMP preservam `replyToMessageId` no modelo visual.
O balão da resposta mostra autor e trecho original; tocar na citação navega
até a mensagem já carregada. Se ela estiver fora da página atual, aparece
"Mensagem anterior não carregada" e a orientação para carregar o histórico,
sem criar um endpoint ou fabricar o conteúdo original.

`HumanMessageLayoutTest` verifica a geometria de texto/horário, alinhamento,
avatares e citação com dados locais; não envia mensagens ao servidor.
Na correção das citações curtas, os balões passaram a reservar a largura disponível
para a referência, evitando a medição circular entre wrap_content e filhos com
peso. O autor ocupa uma linha e a prévia continua limitada a duas. Mensagens sem
citação mantêm largura proporcional ao conteúdo. Tipografia de 17sp, padding
de 21dp/18dp e margem lateral seguem o chatbot existente e as telas Figma
3272:11287 (conversa) e 3272:11334 (Astrinho), no arquivo qdCrRXAVuiNhNkBuim95zz.
O teste inclui resposta "Oi" citando mensagem própria, largura normal e 320dp.

O chat também permite arrastar uma mensagem para a direita e soltar para
selecionar Responder. O deslocamento fica limitado a 72dp; ao ultrapassar 64dp,
o indicador confirma o gesto com feedback tátil. A mensagem retorna à posição
original e a mesma seleção usada pelo menu abre a citação e o teclado, mantendo
o rascunho. A ação vale para mensagens próprias e recebidas com ID persistido,
sem remover linhas ou alterar a operação de envio pendente. O helper é removido
ao destruir a view. O menu continua disponível para toque e acessibilidade.
HumanMessageSwipeTest cobre gestos em mensagens enviadas/recebidas, deslocamento
curto, sentido contrário e envio pendente. Esses testes instrumentados foram
compilados; a execução no aparelho foi deixada para o usuário.
Validação de 30/10/2026: 153 testes unitários aprovados, `assembleDebug`,
`assembleDebugAndroidTest` e `lintDebug` aprovados. O teste de layout passou no
M31 em 2,141s e a captura foi inspecionada. A versão foi instalada no aparelho.
O envio de uma resposta citada entre contas reais ainda não foi exercitado nesta
etapa; contrato, preservação do ID em retry e renderização foram verificados.

Em 29/10/2026, o teste do computador validou HTTPS das novas APIs e handshake
WebSocket HTTP 101 sem Origin, mantendo a verificação de certificados. A primeira
rodada teve 151 testes unitários aprovados, build e lint sem erros. APK instalado
com sucesso no Samsung M31 (Android 12), preservando os dados existentes.
Os dois testes instrumentados `HumanChatEndToEndTest` e `HumanChatDeviceTest`
passaram inicialmente no M31 em 20,793s: autenticação com as duas contas fornecidas, autorização
pelos contatos, envio e confirmação nos dois sentidos, deduplicação de reenvio,
histórico REST, leitura 204/contador zerado, retorno do segundo plano, restauração
das inscrições e logout. Três mensagens únicas foram persistidas; as repetições
usaram os mesmos IDs. O arquivo temporário de credenciais foi removido do
computador e do aparelho. A sessão padrão anterior foi preservada.

O teste prolongado acrescentado depois detectou a recusa do heartbeat STOMP
de saída. Após a negociação `0,10000` com ping/pong WS de 10s, a sessão permaneceu
conectada por 32s e reconectou ao voltar ao primeiro plano (`HumanChatDeviceTest`,
35,185s, aprovado). Os testes unitários finais totalizaram 152, sem falhas.

A rodada final com a adaptação aplicada aprovou os três testes instrumentados
(`HumanChatEndToEndTest`, `HumanChatDeviceTest`, `HumanChatUiDeviceTest`) em
63,4s. Incluiu novamente envio/recebimento real, três ciclos de heartbeat,
histórico na tela, botão de envio habilitado, rascunho após recriação da Activity
e retorno do segundo plano. A compilação debug, o APK de testes e `lintDebug`
também passaram; o lint não apontou erros, mantendo 141 avisos no projeto.
As duas rodadas do teste de envio persistiram seis mensagens únicas ao todo.
