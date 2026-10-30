# Notificações de mensagens — SCRUM-674

O chat continua usando STOMP e histórico REST. O novo aviso Android usa o Firebase
Messaging e o cadastro Firestore de dispositivos já existentes. Não abre uma
segunda conexão WebSocket, nem acrescenta mensagens ao histórico a partir do push.

## Comportamento

- Com o app aberto, não apresenta notificação do sistema. O chat recebe pelo STOMP.
- Em segundo plano, mostra nome do remetente e prévia; na tela bloqueada oferece
  uma versão pública genérica, respeitando as configurações do Android.
- Substitui o aviso anterior da mesma conversa, sem acumular uma entrada por mensagem.
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
Sem aparelho conectado nesta etapa: entrega FCM real, toque, processo encerrado
e comportamento visual do Android ainda dependem da instalação e do serviço ativo.
Não há garantia de entrega/exibição: FCM, permissão, rede e sistema operacional
podem atrasar ou impedir o aviso. Aceite FCM não é confirmação de leitura.
