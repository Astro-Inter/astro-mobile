# Notificações de mensagens — SCRUM-674

O chat continua usando STOMP e histórico REST. O novo aviso Android usa o Firebase
Messaging e o cadastro Firestore de dispositivos já existentes. Não abre uma
segunda conexão WebSocket, nem acrescenta mensagens ao histórico a partir do push.

## Comportamento

- Com o app aberto, não apresenta notificação do sistema. O chat recebe pelo STOMP.
- Em segundo plano, mostra nome do remetente e prévia; na tela bloqueada oferece
  uma versão pública genérica, respeitando as configurações do Android.
- Substitui o aviso anterior da mesma conversa, sem acumular uma entrada por mensagem.
- Usa MessagingStyle e Person do Android: remetente e horário separados, avatar
  circular e até cinco prévias recentes da conversa ao expandir. O histórico
  visual vem somente do aviso ainda ativo, sem persistir novas prévias em disco;
  ao dispensar o aviso ou trocar de conta, a próxima apresentação começa limpa.
- Exibe iniciais imediatamente e tenta enriquecer com a foto HTTPS retornada por
  HumanChatRepository (GET /chats), verificando conversa e participante. Falha de
  rede ou foto ausente mantém as iniciais. A atualização da imagem é silenciosa
  e só ocorre se o aviso ainda existe, pertence à mesma conta/mensagem e o app
  continua em segundo plano; não recria aviso dispensado nem repete mensagens.
- A foto é carregada por ChatNotificationAvatarWorker (WorkManager), com rede
  disponível, trabalho único por conta/conversa e até três tentativas. A consulta
  usa o repository e a sessão Firebase existentes; somente identificadores são
  persistidos no trabalho, sem token ou prévia. Ao chegar outra mensagem, a foto
  do mesmo remetente no aviso ativo é preservada enquanto a nova busca ocorre.
- A atualização da foto reconstrói o aviso com um builder novo, copiando somente
  os identificadores internos, histórico e destino de abertura. Isso evita
  reaproveitar metadados antigos do estilo nativo ao trocar as iniciais pela foto.
- A chave visual do Person inclui uma impressão do conteúdo do avatar. Isso
  invalida a representação anterior quando a foto muda, preservando a mesma
  chave enquanto a imagem permanece igual. A identidade e a autorização do
  remetente continuam usando o identificador da API.
- Não adiciona ações de responder, marcar como lida ou silenciar; desabilita
  sugestões contextuais automáticas. A aparência final depende do Android e do
  fabricante. A versão pública da tela bloqueada continua genérica, sem avatar
  do remetente, nome ou prévia. Não há mudanças no contrato FCM, Worker ou API.
- Filtra UID atual, logout pendente, mensagem repetida, formato e validade de 24h.
  Guarda até 1.024 recibos locais por 24h. O logout remove os avisos e recibos.
- Ao tocar, mantém o destino durante a autenticação. Busca a lista autorizada
  pela API antes de abrir a conversa, usando os dados do participante retornados
  pelo servidor. Outro UID, conversa ausente ou acesso revogado não abre o chat.
- Uma consulta de navegação usa ViewModel e repository próprios; é cancelada ao
  sair do primeiro plano e pode ser refeita no retorno, sem disputar a lista do chat.

## Contrato FCM data-only

Todos os valores são strings: type=chat_message, uid, message_id, conversation_id,
sender_id, sender_name, body e created_at. IDs de mensagem/conversa são UUIDs;
sender_id é o identificador Java, e uid é o Firebase UID do destinatário.
sender_name tem até 80 pontos Unicode, body até 160. created_at é ISO-8601.
Sem notification no FCM: o aplicativo decide se deve apresentar o aviso.
O contrato entre a API e o Worker está em docs/chat-push.md de ambos os serviços.

## Verificação e teste com duas contas

1. Publicar/configurar API e Worker conforme os respectivos documentos. Instalar
   o APK atualizado, entrar como destinatário e permitir notificações Android.
2. Usar duas contas ativas autorizadas a conversar no mesmo workspace, uma em
   outro aparelho ou cliente STOMP já autenticado. Nunca compartilhar ID tokens.
3. Com o destinatário aberto, enviar uma mensagem: aparece no chat, sem aviso Android.
4. Colocar o destinatário em segundo plano e enviar outra: conferir nome/prévia,
   tela bloqueada, toque e abertura da conversa correta.
5. Repetir após encerrar normalmente o processo; conferir login/restauração.
   Não usar Forçar parada: o Android pode bloquear o recebimento nesse estado.
6. Testar rede indisponível/retorno, duplicata do mesmo evento, mensagem nova,
   permissão negada e logout/troca de conta. Avisos antigos não podem abrir outra conta.
7. Revogar acesso e tocar num aviso anterior: a API deve impedir a abertura.

Verificações locais: testDebugUnitTest (156 casos) e assembleDebug aprovados.
ChatPushPolicyTest cobre primeiro plano, UID, duplicata, formato, limites Unicode
e validade. Testes de repository/STOMP existentes também executados.
ChatNotificationFactoryTest verifica estilo de conversa, avatar, ausência de
ações, privacidade, limite e ordem das cinco prévias, atualização silenciosa sem
duplicar mensagens e isolamento entre contas. Os cinco casos passaram no M31,
incluindo a verificação do avatar no Person nativo do Android. O diagnóstico
opt-in ChatNotificationAvatarDeviceTest passou consultando o remetente autorizado
pela API e baixando sua foto HTTPS no aparelho, sem enviar mensagens ou publicar
notificações. Ele requer o argumento avatar_sender_id; sem esse argumento,
é ignorado. O teste não registra tokens, URL da imagem ou conteúdo da conversa.
Sem aparelho conectado nesta etapa: entrega FCM real, toque, processo encerrado
e comportamento visual do Android ainda dependem da instalação e do serviço ativo.
Não há garantia de entrega/exibição: FCM, permissão, rede e sistema operacional
podem atrasar ou impedir o aviso. Aceite FCM não é confirmação de leitura.

## Diagnóstico em aparelho — 30/10/2026

O diagnóstico local opt-in (não incluído nesta PR) utilizou duas contas
expressamente autorizadas e fixture privada no aparelho, preservando a sessão
padrão. Enviou uma mensagem identificada como teste e aguardou um aviso Android
por até 45s. A fixture foi removida ao terminar; o resultado não contém tokens
ou senhas.

No M31, a permissão de notificações e a sincronização do cadastro passaram.
A conta remetente Rosa enviou para a conta Lucas, após validação de contatos
autorizados pelo backend. A mensagem foi confirmada por STOMP e localizada no
Mongo com ID aa4bec92-9021-3b2e-b5f2-b9a94d780264. Não houve aviso em 45s e
nenhum evento dessa mensagem foi encontrado em chat_push_outbox. Portanto,
o teste ponta a ponta falhou antes do encaminhamento ao Worker.

Cloudflare: versão c36083b8-b3e8-48e4-a390-156a61a2096c publicada e ativa.
GitOps declara imagem sha-26ecf94a64be9fa86ad818d7cc8f8f76133f9dda, que inclui
o código da PR API #100; isso não comprova a imagem efetivamente executada na AWS.
A API precisa ser inspecionada quanto à imagem e às variáveis CHAT_PUSH_ENABLED,
CHAT_PUSH_URL e presença de CHAT_PUSH_SECRET. Não registrar o valor do segredo.
Uma checagem isolada de entrega não foi executada: a revalidação SQL falhou por
cadeia de certificados não confiável no computador; TLS permaneceu validado.

### Correção de configuração na AWS

A inspeção no CloudShell confirmou a API no ECS/Fargate, cluster astro-fargate,
serviço astro-api, revisão astro-api-fargate:6. O contêiner api usava a imagem
sha-26ecf94a64be9fa86ad818d7cc8f8f76133f9dda, mas não tinha CHAT_PUSH_ENABLED,
CHAT_PUSH_URL ou CHAT_PUSH_SECRET. O push permanecia desligado por padrão.

Com autorização do usuário, as três configurações foram adicionadas ao Secret
existente do Secrets Manager, com o mesmo segredo do Worker, e injetadas na nova
revisão astro-api-fargate:7. A imagem e os demais parâmetros foram preservados.
O CloudShell validou o segredo e a conectividade HTTPS com o Worker: HTTP 400
para corpo inválido usando o User-Agent do Java. A cópia temporária do segredo
foi removida do CloudShell após o registro e início do rollout.

O usuário optou por executar pessoalmente o próximo teste no celular. Nenhuma
nova mensagem de teste foi enviada nesta etapa de configuração da API.
Verificações após a alteração: aplicação inicializada, revisão 7 com uma tarefa
ativa, zero pendentes e alvo healthy no ALB; revisão anterior em draining.
Outbox criada com índices _id_, done_1_nextAttempt_1 e expiresAt_1. A conclusão
do teste de recebimento FCM permanece a cargo do usuário.
