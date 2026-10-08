# SCRUM-452 — Sessões do SATH no mobile

## Contrato integrado

Implementação baseada no contrato SCRUM-453 fornecido pelo usuário. Base: `https://astro-ai-api-qq6l.onrender.com/`.

| Endpoint | Uso no aplicativo |
| --- | --- |
| `GET /sessions?limit=20&cursor=...` | Lista paginada das conversas da conta; cursor omitido na primeira página. |
| `GET /sessions/{session_id}/messages` | Histórico completo e estado atual, sem reabrir a conversa. |
| `POST /sessions/{session_id}/iniciar` | Continuar explicitamente uma conversa encerrada, mantendo seu UUID. |
| `POST /sessions/{session_id}/encerrar` | Encerrar ou concluir um encerramento pendente, por ação do usuário. |
| `POST /chat/messages?markdown=true` | Primeiro envio sem UUID cria a sessão; próximos envios mantêm o UUID selecionado. |

Todas as chamadas usam `FirebaseUser.getIdToken` no header `Authorization: Bearer`. Uma resposta 401 permite uma renovação forçada e uma única repetição. Não é usado `/auth/login`, nem enviado UID ou workspace na query. Iniciar/encerrar não enviam body.

## Comportamento

### Referência visual

- Lista de Conversas do App V2 no Figma (`3326:5570`), componentes `mobileNav`, `mobileInput`, `mobileFilterGroup` (`2762:7991`), `mobileChatCard` e `mobileButton` (`2762:7514`). Os rótulos e ações foram adaptados para as sessões da IA.
- Reutiliza os tokens Astro, Montserrat, campo de busca com altura mínima de 60 dp/raio de 9 dp, botões com raio de 10 dp, margens de 30 dp, avatar do SATH, divisórias e navbar com Chat selecionado. Busca e ação principal usam os estilos compartilhados de `astro_components.xml`, baseados na composição das telas de acesso e nos componentes do Figma. Cabeçalho único de 64 dp, sem introdução repetida, libera espaço para a lista.
- Linhas de sessão têm altura mínima de 80 dp e avatar de 36 dp. Título e prévia ocupam uma linha cada, com reticências; estado e data compartilham a terceira linha. A conversa atual recebe a superfície purple_gray. Filtros Todas/Em aberto/Encerradas trabalham sobre as páginas carregadas; Em aberto inclui sessões encerrando.
- Os filtros reutilizam `ConversationsFilter`, sem a alteração de altura visual para 48 dp. Cada filtro tem uma área de toque de pelo menos 55 dp, com estado selecionado propagado para o fundo e sem duplicar o rótulo no leitor de tela.
- Nova conversa fica junto à parte inferior da tela, com altura mínima de 60 dp, fundo `bg_login_password_button`, texto centralizado e Plus de 18 dp à esquerda, a 16 dp da borda, conforme a composição de `mobileButton`. Busca com teclado oculta o botão de nova conversa e a navbar para liberar espaço para os resultados. As alturas são mínimas para acomodar o tamanho de fonte configurado no aparelho.
- SVG original do Figma `Hamburguer` (`1011:669`) convertido para PNG com transparência em `drawable-nodpi/ai_sessions_menu.png`. `Plus` (`1011:687`) reutiliza o recurso existente `chat_add.png`. O cabeçalho do chat usa o hambúrguer dentro de uma área de toque de 48 dp, com imagem de 20 dp. Por pedido do usuário, os ícones de atualizar foram removidos da lista e do chat; as telas consultam os dados ao entrar e mantêm as ações de recuperação em caso de erro.

### Lista e navegação

- Lista com título, prévia em texto simples, data de atualização e status ativa/encerrando/encerrada. A ordem fornecida pela API é preservada; UUIDs repetidos entre páginas são deduplicados.
- Paginação automática ao alcançar as duas últimas linhas visíveis, com indicador no rodapé e uma requisição por vez. Páginas adicionais também preenchem uma lista curta ou sem correspondências de busca/filtro, até ocupar a tela ou esgotar o cursor. O botão Carregar mais foi removido.
- Busca local nas conversas carregadas; a API não fornece pesquisa global. Novas páginas são filtradas com a mesma busca/estado. A ausência de resultados só é exibida após esgotar as páginas disponíveis.
- Atualizações incrementais com ListAdapter/DiffUtil preservam os itens e a posição de rolagem ao acrescentar uma página. Falhas interrompem a paginação automática e oferecem Tentar novamente; cursores repetidos interrompem o carregamento para evitar um ciclo de requisições.
- A lista é atualizada ao abrir/retornar à tela e depois de enviar, continuar ou encerrar. Um cursor rejeitado reinicia a primeira página.
- Selecionar uma conversa busca o histórico antes de navegar. Ler uma conversa encerrada não a reabre; o botão Continuar conversa chama /iniciar.
- Uma conversa encerrando fica disponível para leitura e oferece Concluir encerramento. O campo de envio só é liberado quando ativa e sem operação ou envio incerto pendente.
- Sair da tela não encerra a sessão. Encerrar exige uma ação explícita com confirmação.
- Histórico usa `mensagens`, com `role` user/assistant e `content`, validando o total e o UUID. Markdown e tabelas continuam usando o renderer do chat.
- Mensagens históricas não exibem horários inventados: esse contrato não possui timestamp por mensagem. A data da lista vem de updated_at.
- Rascunhos são separados por conversa e permanecem somente em memória; iniciar outra conversa não reutiliza o UUID anterior. A lista remota é a fonte das conversas persistidas, inclusive depois de reiniciar o aplicativo.
- Troca de conta/logout limpa mensagens, rascunhos, lista e cursor, cancela requisições e invalida callbacks antigos. Tokens, mensagens e UUIDs não são gravados em disco pelo mobile.

## Falhas e envios incertos

- Rede, timeout, autenticação, permissão, formato inválido, limite de uso, conflito e conversa indisponível recebem estados de erro.
- 404 não recria uma sessão com o UUID antigo. A lista é atualizada e a conversa indisponível não aceita envios.
- 409 oferece atualização do histórico sem repetir automaticamente o POST. A mensagem rejeitada pode voltar ao rascunho para envio manual após resolver o estado.
- Após falha de envio em sessão conhecida, Tentar novamente consulta o histórico primeiro. Se a pergunta já estiver persistida, o envio pendente é resolvido sem reenvio. Caso contrário, uma confirmação precede o reenvio manual.
- Se o primeiro envio falhar antes de retornar um UUID, a lista é atualizada e a confirmação orienta conferir as conversas antes de reenviar, pois o servidor pode ter criado uma sessão.
- Não há repetição automática de POST por falhas de rede. As operações possuem cancelamento e prazo total de 150 segundos, incluindo a obtenção do token.

## Disponibilidade e validação

Na primeira consulta de 08/10/2026, GET /sessions ainda não aparecia no OpenAPI. Durante a conferência visual posterior no Samsung SM-M315F, a lista já carregou sessões reais no mesmo endereço usando a sessão Firebase do usuário.

A versão anterior foi compilada e instalada no Samsung SM-M315F. Conferidos visualmente lista e histórico reais, filtros/estado vazio, busca com teclado, menu hambúrguer, ícones e botão Nova conversa. Capturas locais em `.gradle/sessions-preview/`.

A revisão para maior densidade e paginação automática compilou com sucesso (`:app:assembleDebug`). Com a reconexão posterior do Samsung SM-M315F, a lista compacta foi conferida ao revisar o cabeçalho do chat. O filtro Encerradas preencheu a tela automaticamente com resultados das páginas seguintes. Não foram adicionados ou executados novos testes automatizados; os POSTs de continuar e encerrar não foram exercitados nesta revisão visual.

Padronização dos controles de 08/10/2026: `:app:assembleDebug` aprovado e APK instalada no SM-M315F. Busca e botão Nova conversa conferidos com altura de 60 dp; filtros com seleção correta e identificação como botões na acessibilidade. Busca por NRs filtrou os resultados e manteve o campo acima do teclado; o termo foi removido e o filtro Todas restaurado ao finalizar. A tela exibiu cinco conversas completas no aparelho. Capturas em `.gradle/sessions-preview/astro-controls.png` e `astro-controls-search.png`. Não foram adicionados ou executados testes automatizados nessa padronização.
