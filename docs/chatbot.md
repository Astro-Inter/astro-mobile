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
