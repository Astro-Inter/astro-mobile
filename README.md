# astro-mobile

## Lembrete de eventos pendentes — SCRUM-617

O aplicativo registra os dispositivos autenticados em
`fcm_tokens/{firebaseUid}/devices/{installationId}` no Firestore. Cada documento
contém somente `token` e `updated_at` (timestamp do servidor). Um UUID local
identifica a instalação; ele não usa identificadores físicos do aparelho.

A implementação segue as camadas existentes:

- `data/local/PushPreferences` e `PushSyncPolicy`: cache de UID, token e último ACK,
  identificação da instalação, deduplicação visual e bloqueio persistente de logout.
- `data/firebase/PushTokenRepository`: SDKs oficiais, sincronização após autenticação,
  renovação de token, heartbeat de sete dias e remoção do vínculo no logout.
- `shared/notifications/PushRegistrationViewModel`: contrato da Activity com a camada de dados.
- `AstroMessagingService` e `shared/notifications`: recebimento FCM, canal, apresentação
  e filtragem pelo UID atual e pelo ciclo do lembrete.
- `AstroApplication`: composição dos componentes; `MainActivity`: permissão Android
  e navegação para os eventos somente depois da autenticação.

O cache é atualizado somente após confirmação da escrita no Firestore. Sessões
com o mesmo UID/token não provocam leitura nem escrita no Firestore até o heartbeat.
Tokens renovados sem sessão são mantidos pelo SDK e sincronizados após login.
As preferências de push são excluídas do backup e da transferência entre aparelhos.

Ao sair, a apresentação de push é bloqueada imediatamente. A identidade Firebase
é preservada apenas para remover o próprio dispositivo e invalidar seu token; uma
nova autenticação aguarda essa limpeza. Se estiver offline, o logout fica pendente
e a próxima tentativa retoma a limpeza antes de associar outra conta. Se a sessão
já tiver sido revogada, a rotação do token protege a instalação; registros órfãos
com tokens inválidos podem ser removidos pelo remetente. Outros dispositivos do
mesmo usuário não são removidos.

O serviço envia **mensagens data-only** do tipo `pending_events`. O app verifica o
UID e exibe a notificação no próprio serviço, inclusive em segundo plano; isso
evita a apresentação automática de mensagens da conta anterior. Um ciclo já
exibido não aparece novamente. Tocar no aviso abre os eventos do fluxo atual.
O título é “Eventos pendentes” e o texto é “Você tem eventos pendentes. Abra o
Astro para conferir.”. Não são incluídos detalhes pessoais no texto.

## Configuração Firebase e validação

1. Manter o `app/google-services.json` do projeto existente, ignorado pelo Git.
2. Habilitar Cloud Firestore e FCM HTTP v1 no mesmo projeto da autenticação.
3. Revisar `firestore.rules` e incorporar esse bloco às regras existentes antes de
   publicar. Não substituir regras de outras funcionalidades indiscriminadamente.
   Os clientes só podem ler/escrever seus próprios dispositivos; campos extras,
   token vazio e timestamps arbitrários são rejeitados.
4. Compilar com o toolchain existente: `./gradlew test assembleDebug`. A configuração
   Firebase fictícia do CI permite validar a compilação sem credenciais administrativas.
5. Em Android com Google Play Services, autenticar, conceder a permissão e conferir
   o documento do dispositivo; repetir o login sem alterar token e conferir o cache.
6. Validar dois dispositivos, renovação de token, logout offline, retomada da limpeza,
   troca de conta e rejeição de mensagens cujo UID seja da conta anterior.
7. Validar recebimento aberto/em segundo plano e ao abrir após o processo terminar.
   Aplicativo forçado a parar, permissão negada, ausência de rede ou restrições do
   sistema podem impedir a entrega; aceite do FCM não comprova exibição no aparelho.

As dependências Messaging e Firestore usam a mesma Firebase BoM já adotada pelo app.
Os testes de regras ficam em `firebase-tests`: executar `npm ci` e `npm test` nesse
diretório com Java instalado. O CI executa esse emulador isolado, além do build e
dos testes Android. As ferramentas JavaScript são somente de desenvolvimento e
não entram no APK; não fornecer a elas credenciais administrativas de produção.
Nenhuma chave administrativa ou conta de serviço é incluída no Android. O remetente
vive no [astro-notification-service](https://github.com/Astro-Inter/astro-notification-service),
na mesma SCRUM-617; não é necessário criar uma API de cadastro de tokens.
