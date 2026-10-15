# Conversa entre Gestor e Colaborador — SCRUM-301

UI e mocks do [card SCRUM-301](https://luizacursino.atlassian.net/browse/SCRUM-301), filho da SCRUM-261. Fontes App V2: [Gestor — 3272:11287](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=3272-11287) e [Colaborador — 3326:5594](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=3326-5594).

## Implementado

- Gestor: Home → Colaboradores → Colaborador → Conversar. Nome/cargo/retrato acompanham o mock selecionado. A seta retorna ao detalhe.
- Variante Colaborador: cabeçalho Tyler, the Creator/Engenheiro de segurança e uma mensagem recebida, conforme o frame. Entrada pela lista de Conversas aprovada pelo solicitante: um contato humano de exemplo por fluxo, mantendo a IA fixada.
- Cinco mensagens iniciais no Gestor e uma no Colaborador; textos e horário fictício `20:99` preservados literalmente do Figma, pendentes de revisão editorial. Novas mensagens locais usam o horário atual HH:mm.
- Balões enviados à direita e recebidos à esquerda com avatar; texto literal e horário, sem Markdown/serviços do chatbot. Cores e raios reutilizados; tamanhos se adaptam à largura disponível para quebrar textos longos.
- Input Mensagem, botão + com ripple e indicação de anexo pendente, envio pelo botão/seta ou ação do teclado. Texto vazio/espaços não gera mensagem; não há limite artificial nem resposta automática.
- Mensagens e rascunho ficam no ViewModel enquanto a tela permanece na pilha. Sair da demonstração não salva histórico real.
- Skeleton local de 1s seguindo o padrão atual, oculto no XML, cancelado ao sair, sem bloquear a thread principal.
- Navbar única da Activity, aba Chat selecionada, fixa nas transições. Ao abrir teclado, usa o mesmo comportamento já existente no chat da IA (oculta a navbar); sem duplicar insets no Fragment.

## Fontes, recursos e escopo

Reutilizados `item_chat_message`, fundos `bg_chat_user/assistant/input`, cores, fontes, seta de voltar, plus e seta de envio. O adapter humano preenche apenas texto/horário/avatar; `ChatFragment`, `ChatViewModel`, adapter e serviços da IA não foram modificados.

Hashes dos retratos dos exports idênticos aos `participant_avatar_1/2`. Plus e seta têm pixels idênticos aos `chat_add` e `chat_send_arrow`. Nenhum bitmap, fonte ou dependência nova. Novos drawables apenas compõem as duas camadas de retrato do Figma e o fundo circular do contador mock.

Header de 89dp; retrato 48dp no Gestor e 50dp no Colaborador; avatars recebidos 37/41dp; textos 17/16sp e hora 12sp; raios 16dp. Plus tem raiz visual 18dp dentro do alvo de toque, seta 20dp. Previews dos balões usam o item compartilhado; nenhum arquivo de screenshot foi usado como asset da implementação.

Também conferidos os frames de listagem 3272:11270 e 3326:5570. Gestor vê Kevin Parker; Colaborador vê Gestor - Tyler, the Creator. Retrato de 44dp, margem de 20dp, preview de uma linha e contador fictício de 29dp no Colaborador. IA fixada preservada. Pesquisa/filtros da lista, notificações reais de leitura, anexos, API, Firebase, persistência e autorização não foram implementados nesta UI. Lógica do Gestor/Colaborador pertence às SCRUM-302/303.

## Branches e revisão

Branch `feat/SCRUM-301-conversa-gestor-colaborador-ui` criada da SCRUM-311 em `e888e83`, sem incluir SCRUM-309. A SCRUM-309 foi commitada com autorização explícita em `2e25cbb` e permanece intacta na branch irmã. Ordem e conflitos esperados em `docs/fila-de-prs.md`.

`assembleDebug` e `lintDebug` aprovados sem erros após ligar as duas entradas da lista. Avisos locais: overdraw e reticências literais dos previews do Figma. `git diff --check` sem problemas de whitespace. Nenhum físico conectado; instalação, teclado/navegação em execução e comparação visual em celular/tablet pendentes. Emulador não utilizado, testes não criados, credenciais/SDK preservados. Sem commit, push ou PR da SCRUM-301. Card atualizado em Em andamento, Sprint 6.
