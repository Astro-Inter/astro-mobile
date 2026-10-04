# Chatbot no Android

## Conexão e autenticação

- API de IA: `https://astro-ai-api-qq6l.onrender.com/` (`BuildConfig.AI_API_BASE_URL`).
- Contrato: <https://astro-ai-api-qq6l.onrender.com/docs>.
- O chatbot usa a sessão Firebase já aberta no aplicativo. Não chama `/auth/login`.
- `FirebaseIdTokenProvider` obtém o ID token com `FirebaseUser.getIdToken(false)`.
- As requisições autenticadas usam `Authorization: Bearer <ID token>`.
- Tokens e senhas não são persistidos nem registrados pelo chatbot.
- O cliente da IA é separado da API de contas, com limite de 120 segundos por chamada.
- Redirecionamentos HTTP não são seguidos, evitando encaminhar o token para outro destino.

## Envio de mensagens

- `POST /chat/messages?markdown=true`, com `message` e `session_id` opcional no JSON.
- O primeiro envio omite `session_id`; a resposta retorna o UUID para os próximos envios.
- A resposta da IA vem em `resposta`, já em Markdown.
- Mensagens em branco ou com mais de 4.000 caracteres Unicode são rejeitadas localmente.
- Uma resposta `401` força a renovação do token e repete a chamada uma única vez.
- Cancelamentos também invalidam callbacks de obtenção de token ainda pendentes.
- Erros de rede, timeout, sessão, permissão, limite de uso e formato inválido são separados.

## Tela no celular

- A aba de chat e o balão do assistente na Home abrem o mesmo chatbot.
- A interface usa o padrão visual do mobile: fundo escuro, Montserrat, campos de 9–10 dp, ação no roxo principal e mascote da Home. Mantém perguntas sugeridas e balões por autor da referência web.
- Respostas, código e tabelas usam superfícies escuras com texto claro; links em lavanda e mensagens de erro na cor de erro do aplicativo.
- Markwon renderiza títulos, negrito, itálico, listas, citações, código, links e tabelas.
- Mensagens do usuário são exibidas literalmente, sem interpretar Markdown.
- Links HTTP/HTTPS abrem ao tocar; o renderer não usa WebView nem carrega imagens remotas.
- O campo permite várias linhas e fica acima do teclado, respeitando as barras do Android.
- A conversa e o rascunho ficam em memória no ViewModel da Home, inclusive durante recriação da tela.
- Voltar para a Home e reabrir o chat mantém a conversa. Sair da conta remove a Home e esse estado.
- A conversa não é restaurada depois de encerrar o processo do aplicativo.

## Carregamento e falhas

- Enquanto uma mensagem está pendente, novos envios ficam bloqueados; o próximo rascunho pode ser digitado.
- Após 15 segundos, a interface informa que a resposta pode demorar mais.
- Uma espera total de 150 segundos cancela a operação, incluindo a obtenção do token.
- Falhas marcam o balão como “sem resposta” e exibem uma orientação conforme a causa.
- Tentar novamente reutiliza o balão da pergunta. Não cria uma segunda mensagem na tela.
- Timeouts e respostas inválidas podem acontecer depois de o servidor receber a mensagem. A interface informa essa possibilidade antes de um reenvio manual.
- Não há repetição automática de POST após falhas de rede; apenas `401` renova o token e repete uma vez.
- Sessão inválida oferece “Entrar novamente”; isso encerra a sessão local e retorna ao login.
- Sem permissão ou com mensagem inválida, a interface orienta a correção sem oferecer retry automático.

## Validação

Comandos de build e testes locais:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.astro_mobile.data.ai.AiChatRepositoryTest"
```

`ChatScreenTest` pode ser executado via AndroidJUnitRunner no emulador. Usa respostas locais e tokens fictícios, sem chamar a API real. As capturas são gravadas em `getExternalFilesDir("chat-qa")` no aparelho de teste.

Validação de 04/10/2026:

- 9 testes do repositório da IA passaram: header, Markdown, sessão, validação, renovação, cancelamento e erros.
- 4 testes instrumentados passaram: Unicode, Markdown/carregamento/rascunho/recriação, retry sem duplicação e sessão ausente.
- Conversa real via Firebase e API de IA validada no Samsung SM-M315F; campo de envio visível acima do teclado.
- A suíte geral apresentou 3 falhas em testes de login não alterados: dois testes de `EmailVerificationViewModelTest` e `EmailVerificationRepositoryTest.networkFailureIsConnectionError`.

Os commits correspondem às subtarefas `SCRUM-419` (conexão), `SCRUM-420` (envio), `SCRUM-421` (tela/Markdown) e `SCRUM-422` (carregamento/falhas).
