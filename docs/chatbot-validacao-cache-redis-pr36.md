# Compatibilidade mobile com o cache Redis — 01/11/2026

## Resultado

Não foi encontrada incompatibilidade que exija alteração no código do aplicativo. A otimização é interna ao backend: o Android continua recebendo e renderizando os mesmos blocos, sem acessar Redis, reconstruir URLs ou renovar assinaturas. Foram acrescentados somente testes de compatibilidade e este relatório; alterações locais anteriores foram preservadas.

## Revisões verificadas

- [API PR #36](https://github.com/Astro-Inter/astro-ai-api/pull/36): mesclada, head `a5df1bb5a47bd4abe1bc7115d5b8c030e4e45fa0`.
- [Catálogo Redis PR #62](https://github.com/Astro-Inter/astro-database/pull/62): mesclada, head `cfa38128e5a65302a5a6bc4babae64fab5c39f4c`.
- Produção informou `bf2f90998aba17d8b3480cfcbb6b0f65039b2f76`. A comparação com o head da PR confirmou seis commits à frente e nenhum atrás, incluindo os merges de #35/#36. Portanto a premissa de PR ainda aberta/publicação ausente já não valia nesta rodada. Não foi executado merge ou deploy.
- Lidos `docs/cache-redis.md`, os testes da PR e as definições `redis/keys/user-avatar.json` e `redis/keys/ai-public-source-cache.json` nas revisões acima. O catálogo confirma a mesma chave `usuario:foto:{id_usuario}`, autorização anterior ao MGET e ausência de renovação em hits.

O clone local original da IA estava numa branch antiga e com alterações não relacionadas. Ele não foi trocado nem editado. A revisão exata da PR foi extraída para uma cópia isolada em `.gradle/api-pr36` do checkout mobile principal. Os testes de backend foram executados nessa cópia usando o Python já existente, sem carregar o `.env` privado do clone original. Não foi iniciado um backend local com credenciais reais ou Redis real; os endpoints locais são exercitados por FastAPI TestClient/ASGI e dependências simuladas. O tablet usou o backend publicado que já contém a alteração.

## Revisão do cliente

- `ChatStructuredContent`: ficha, célula de tabela/matriz e galeria legada compartilham o comportamento de avatar. Iniciais são renderizadas antes da imagem; nome ausente usa avatar neutro.
- O carregador usa somente a URL HTTPS recebida, valida a origem R2, desabilita cache HTTP e redirecionamentos, limita bytes/dimensões e preserva o fallback quando o HTTP falha ou a imagem não decodifica. Não cria assinatura nem usa cliente autenticado da API para baixar a foto. Não interpreta o cache como autorização.
- Blocos válidos usam o mesmo renderer no POST/histórico. Não se acrescenta frontend cache para URLs assinadas ou respostas privadas.
- `ChatViewModel` observa a conta Firebase. Na mudança de dono, cancela operações, invalida callbacks e limpa mensagens, rascunhos, sessões e listas em memória. O carregamento de uma imagem antiga atua no ImageView antigo, não no cartão da conta nova.
- Não foram alterados contratos, contagens, indicadores, permissões, URLs ou arquivos PDF. A paginação local e os gráficos agregados permanecem independentes.

## Testes locais do backend da PR

Todos com Redis/MCP/signer/armazenamento e contas simulados, sem inferências pagas ou acesso ao cache real:

1. `tests/test_avatar_urls.py` + `tests/test_public_source_cache.py`: **26 passaram**. Cobrem autorização antes do cache, MGET de fotos, prazo restante, hit sem assinatura/renovação, allowlist, TTL, ausência de cache negativo e fallback/cooldown diante de falha/lentidão do Redis.
2. `tests/test_chat.py`, `tests/test_chat_delivery_regressions.py`, `tests/test_chat_replay_regressions.py` e `tests/test_chat_privacy.py`: **323 passaram**. Incluem endpoints locais, blocos POST/histórico, fotos efêmeras reautorizadas, acesso por sessão, tabelas/gráficos/arquivos e recusas explícitas.

Foram emitidos avisos já das dependências Python 3.14/Pydantic V1 e futura mudança de serialização LangGraph. Nenhuma falha nessas suítes. Esses resultados não medem latência nem comprovam um hit real de Redis.

## Testes no tablet

Samsung SM-T225, Android 11/API 30, Astro debug 1.0 (versionCode 1). A instalação existente foi mantida; somente o APK de instrumentação foi atualizado, sem desinstalar, apagar dados ou instalar outra versão do aplicativo.

- `ChatAvatarCacheCompatibilityTest`: **5 passaram**. Testa ficha própria/terceiro com imagem válida, igualdade POST/histórico, URL ausente, HTTP negado/expirado, erro de transporte e imagem inválida, iniciais/avatares neutros, tabela/galeria, callback de imagem atrasado e limpeza do estado ao mudar o dono. Os casos de imagem usam um host fictício interceptado exclusivamente no processo de teste; não desabilitam TLS no aplicativo. Verificam uma única tentativa, cache HTTP desativado, redirecionamento desativado e ausência de Authorization. As Views são anexadas a uma Activity real para executar corretamente os callbacks de imagem.
- Regressões em processo separado: **30 passaram** em `ChatFileContentDeviceTest`, `ChatFileRepositoryDeviceTest`, `ChatAnalyticalContentDeviceTest`, `ChatStructuredContentDeviceTest` e `ChatScreenTest`. Incluem paginação local sem nova requisição, agregações preservadas, gráficos largos/zeros, PDF/expiração/falha e estado da conversa. Os PDFs criados pelas fixtures são removidos ao final dos testes.
- JVM mobile: **184 passaram**, sem falhas.
- Diagnóstico de histórico real, somente GET: **passou nas duas contas**, em execuções separadas após cada login. Fichas com foto retornaram origem permitida, HTTP 200 e bytes decodificáveis; os blocos não vazios conferidos foram aceitos pelo parser. Mensagens antigas de falha com `blocos=[]` não são tratadas como nova incompatibilidade de cache; o conteúdo legado continua disponível. O relatório salva apenas tipos, presença de foto, HTTP e flags, sem nomes, e-mails, tokens ou URLs assinadas.

### Verificação real pela interface

- Ficha própria na primeira conta: foto visível e campos legíveis, confirmados por captura local.
- Pedido de ficha de terceiro na primeira conta: backend respondeu que a pessoa não foi encontrada no escopo. Não foram alteradas permissões nem substituída a pessoa automaticamente.
- Pedido de tabela de dez pessoas com foto: backend recusou a validação, sem liberar componentes. Esse cenário positivo foi coberto com simulação; não se atribui a recusa ao cache sem logs da API.
- O usuário realizou logout/login manualmente com outra conta autorizada. O chatbot abriu a tela inicial, sem a pergunta anterior; a nova ficha mostrou o nome/foto da segunda conta, diferentes dos anteriores. Não houve reutilização visível de mensagens ou avatar entre as contas. Nenhuma senha foi solicitada ou registrada.
- Nova tentativa de ficha de terceiro na segunda conta: recusa explícita do Juiz, sem componentes liberados. Não se declara homologação positiva dessa ficha no ambiente real; a imagem válida de terceiro e sua igualdade POST/histórico passaram com dados simulados no tablet, e as regras de autorização/reautorização passaram na suíte local do backend.

As capturas reais ficam exclusivamente no diretório local `.gradle/chat-qa` do checkout principal; não são versionadas para evitar publicação de dados pessoais. O teste de transporte usa somente identidades/imagens fictícias.

## Limitações e pendências

- Sem acesso ao Redis real e sem métricas/hits do servidor: **ganho de velocidade não homologado**. Os testes confirmam contrato, fallback e semântica de hits simulados; tempos de respostas de IA não isolam o efeito do cache.
- Foto expirada/HTTP 403 e falha de transporte foram simulados, sem esperar uma URL real expirar nem alterar sua assinatura.
- A aprovação/recusa do Juiz e a autorização continuam sendo do backend. Não houve tentativa de obter foto fora do escopo.
- O cliente web não está neste repositório e não foi modificado ou homologado.
- Não houve mudança de configuração remota, consulta direta a chaves Redis, limpeza de cache, upload de fotos, alteração de cadastro, merge ou deploy.
