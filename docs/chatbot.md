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

- SCRUM-452: lista remota paginada, busca nas conversas carregadas, histórico, nova conversa e retomada/encerramento explícitos conforme SCRUM-453. Contrato, falhas e disponibilidade estão em `docs/ai-sessions.md`.

- A interface segue App V2 do Figma, frame 3326:5641: cabeçalho SATH / IA do Astro, avatar, balões arredondados, campo Mensagem com seta interna e navbar com Chat selecionado. Por pedido do usuário, as duas perguntas sugeridas da implementação anterior aparecem nas boas-vindas e enviam seu texto ao tocar; o robô grande da referência web foi removido. A margem inferior do composer é de 8dp.
- Cabeçalho compacto de altura mínima de 72 dp e avatar de 40 dp. Sessões persistidas exibem uma faixa arredondada de 48 dp com o estado à esquerda e a ação por ícone à direita: Stop (`1011:874`) abre a confirmação de encerramento; Play circle (`1011:786`) permite continuar quando encerrada. Ícones originais do Figma, com descrições para leitores de tela, dicas ao manter pressionado e áreas de toque de 48 dp. A ação fica desabilitada e com opacidade reduzida durante uma operação. O ícone de atualizar foi removido por pedido do usuário; o histórico continua sendo consultado ao entrar na tela.
- Confirmação personalizada baseada em `mobilePopup`, variante de dois botões (`2762:7924`): fundo Astro escuro, cantos de 20 dp, texto em bloco destacado, botão roxo de confirmação e Cancelar contornado, ambos com altura mínima de 60 dp. Cross (`1011:777`) reutiliza `conversations_close.png` em uma área de toque de 48 dp. Largura máxima de 370 dp com margens de pelo menos 24 dp; conteúdo rolável acomoda telas menores e fontes ampliadas. DialogFragment acompanha o ciclo de vida e confirma somente a sessão que originou a janela; trocar a sessão/conta ou encerrar em outra operação invalida a confirmação. Cancelar, voltar, tocar fora e o X apenas fecham a janela.
- Os horários de mensagens novas mostram criação/recebimento local. Mensagens carregadas do histórico não exibem horário, pois a API não retorna timestamps por mensagem. Falha e retry preservam o horário original. O botão de anexos é apenas visual e informa que a funcionalidade está pendente.
- A navbar Chat da Home abre a lista de conversas; o mascote e a IA fixada na lista abrem o mesmo assistente. Na conversa, a navbar permite retornar à Home ou à lista. Com o teclado aberto, ela fica oculta para liberar espaço.
- Respostas, código e tabelas usam superfícies escuras com texto claro; links em lavanda e mensagens de erro na cor de erro do aplicativo.
- Markwon renderiza títulos, negrito, itálico, listas, citações, código, links e tabelas.
- Tabelas na resposta têm cabeçalho destacado, bordas, alinhamento do Markdown e células com texto selecionável. Cada tabela possui rolagem horizontal independente quando excede a largura do balão; colunas longas quebram linhas. O parser distingue tabelas de pipes em blocos de código.
- Mensagens do usuário são exibidas literalmente, sem interpretar Markdown.
- Links HTTP/HTTPS abrem ao tocar; o renderer não usa WebView nem carrega imagens remotas.
- O marcador `[Texto do link](google-calendar-conectar)` é convertido para `/integracoes/google-calendar/conectar`. Também aceita `[google-calendar-conectar](Texto do link)`. Ao tocar, o app faz GET com o token Firebase no header e abre a `authorization_url` HTTPS retornada no navegador. Não conecta automaticamente ao receber a resposta. Uma rejeição 401 renova o token uma vez; sair da tela cancela a operação.
- O campo permite várias linhas e fica acima do teclado, respeitando as barras do Android.
- A conversa e o rascunho ficam em memória no ViewModel da Home, inclusive durante recriação da tela.
- Voltar para a Home e reabrir o chat mantém a conversa. Sair da conta remove a Home e esse estado.
- Após encerrar o processo, as sessões persistidas podem ser selecionadas na lista remota; rascunhos locais não são restaurados.

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
- 6 testes instrumentados passaram: Unicode, Markdown/carregamento/rascunho/recriação, retry sem duplicação, sessão ausente, tabelas com rolagem/alinhamento/formatação inline/pipes escapados e marcadores de Google Calendar fora de código. Os testes de tela iniciam diretamente na Home para evitar disputa com a navegação da Splash.
- O contrato público de Google Calendar foi conferido; a autorização com conta Google real não foi executada na validação.
- Conversa real via Firebase e API de IA validada no Samsung SM-M315F; campo de envio visível acima do teclado.
- A suíte geral apresentou 3 falhas em testes de login não alterados: dois testes de `EmailVerificationViewModelTest` e `EmailVerificationRepositoryTest.networkFailureIsConnectionError`.
- Essas 3 falhas foram corrigidas na atualização do CI: a suíte completa passou com 20 testes unitários. A configuração fictícia de Firebase usada no runner está documentada em `docs/ci.md`.

Correção visual de 05/10/2026:

- Balões da IA usam a largura disponível; os do usuário se ajustam ao texto dentro do limite da tela. O modo de medição é atualizado ao reutilizar os itens da lista.
- Build debug aprovado e instalado no Samsung SM-A566E. O teste existente `markdownLoadingDraftAndRecreation` passou com resposta local; capturas de boas-vindas, Markdown e tabela conferidas no aparelho, sem envio à API real de IA.

Revisão do cabeçalho de 08/10/2026:

- `:app:assembleDebug` aprovado e APK instalada no Samsung SM-M315F. Faixa de ações conferida em históricos reais de sessões ativa e encerrada, com Refresh/Stop/Play circle originais do Figma e áreas de toque de 48 dp.
- Atualizar recarregou o histórico. Encerrar abriu o diálogo de confirmação, cancelado durante a conferência. O botão Continuar conversa teve ícone e descrição conferidos sem executar a retomada. Capturas em `.gradle/sessions-preview/header-active.png` e `header-closed.png`.
- Não foram adicionados ou executados testes automatizados nessa revisão.

Confirmação personalizada de 08/10/2026:

- `:app:assembleDebug` aprovado e APK instalada no Samsung SM-M315F. Diálogo baseado no `mobilePopup` de dois botões do Figma conferido com a fonte configurada no aparelho: texto sem cortes, Cross de 20 dp, botões roxo/contornado e fundo escurecido. Cancelar fechou a janela mantendo a conversa ativa; não foi confirmado nenhum encerramento. Captura em `.gradle/sessions-preview/end-dialog.png`. Sem testes automatizados adicionados ou executados.

Os commits correspondem às subtarefas `SCRUM-419` (conexão), `SCRUM-420` (envio), `SCRUM-421` (tela/Markdown) e `SCRUM-422` (carregamento/falhas).

## Revisão arquitetural — 05/10/2026

- O envio principal segue `ChatFragment → ChatViewModel → AiChatRepository → API/Firebase`, com criação manual das dependências. Java, XML, Activity, NavHost e grafo únicos foram preservados. A listagem estática não precisa de Repository ou ViewModel vazio enquanto não houver requisição.
- O pacote funcional `chat` reúne apresentação, modelo local de mensagem e renderização; os recursos estão nos diretórios oficiais de `res`. Não há necessidade de duplicar o chatbot por perfil. Os DTOs HTTP (`ChatRequest`, `ChatResponse`, `GoogleCalendarConnectResponse`) estão em `data/ai`, enquanto a skill orienta `data/api/dto`; essa organização ainda precisa ser padronizada, sem misturar os contratos da IA e da API de contas.
- Há desvios existentes: `ChatViewModel` usa LiveData em vez dos callbacks do padrão Astro; `ChatCalendarLinkHandler` faz HTTP e obtém token diretamente na apresentação, sem Repository/ViewModel. São pendências de refatoração, não corrigidas nesta revisão visual.
- O estado e a navbar do chat estão vinculados a `employeeHomeFragment`; o logout também usa esse destino no `popUpTo`. O fluxo do Gestor ainda é mock em `FlowChoiceFragment`, portanto o chat não está pronto para ser aberto pela futura Home do Gestor sem ajustar esse vínculo.
- No Figma, as listagens de Gestor (`3272:11270`) e Colaborador (`3326:5570`) compartilham estrutura e filtros, mas dizem, respectivamente, “Acesse aqui as suas conversas com os colaboradores” e “Acesse aqui as suas conversas com os gestores”. A implementação atual tem apenas o texto do Colaborador. A escolha deve seguir o fluxo ativo, não somente o tipo da conta, pois um Gestor pode atuar como Colaborador.
- `ConversationsFragment` ainda é apenas UI: nenhuma conversa humana fictícia, requisição, busca ou filtro real foi implementado. A integração de conversas humanas deve permanecer separada do contrato da IA. Não há razão para adicionar um loading artificial ao chatbot; o skeleton da listagem está preparado para a futura requisição.
