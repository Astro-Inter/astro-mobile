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
