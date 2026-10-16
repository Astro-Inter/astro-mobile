# Eventos do funcionário selecionado — SCRUM-309

Somente UI e mocks do [card SCRUM-309](https://luizacursino.atlassian.net/browse/SCRUM-309), filho da SCRUM-258. Referência App V2: [frame 2590:12590](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12590).

## Fluxo e composição

Home do Gestor → Colaboradores → Colaborador → Ver pendências → Eventos.

- Título Eventos; subtítulo Eventos de **nome do exemplo escolhido** e descrição literal do Figma.
- Dropdown Filtrar por estado. Sem adicionar busca, calendário ou chips inexistentes neste frame.
- Três exemplos Treinamento de EPI: Pendente (6/07/2067, sem ícone); Concluído (6/07/2000, check verde); Prazo expirado (6/07/2000, X vermelho).
- Filtro local Todos/Pendente/Concluído/Prazo expirado: mostra 3/1/1/1 linhas, sem alterar o status dos exemplos. Menu nativo, como nas listas existentes; não há variante aberta do menu no contexto obtido.
- A seta retorna ao detalhe; X retorna à Home do Gestor. Navbar compartilhada na Activity, fixa e sem aba destacada nesta tela, como no frame.
- Skeleton mock de 1 segundo, oculto na prévia XML, cancelado ao sair e sem bloquear a thread principal. O ViewModel preserva filtro e carregamento enquanto a tela está no histórico.
- Conteúdo limitado pela largura padrão do app, colunas proporcionais e RecyclerView com rolagem acima da navbar. Não há larguras fixas de texto que excedam a linha.

## Limites e pendências

Os eventos são exemplos visuais, não dados vinculados a uma conta. O mesmo conjunto de três exemplos aparece para cada funcionário selecionado e não representa sua contagem real de pendências. As datas e os estados são literais do Figma: o front não calcula expiração nem conclusão.

Só a linha pendente é interativa no frame. Ela dá ripple e sinaliza que seu detalhe ainda não está disponível, sem abrir a tela de outro fluxo ou inventar autorização. As linhas concluída/expirada são informativas. Destino/ação do evento pendente e contrato de integração ficam pendentes. API pertence à SCRUM-310; sem chamadas à API, Firebase, backend ou alterações de dados.

## Recursos e organização

Classes em `manager/staff/events`; exemplos isolados em `mock/StaffEventSamples`. Fragment cuida de Views/navegação, ViewModel de filtro/carregamento e Adapter da apresentação. Sem Repository vazio, novas bibliotecas, LiveData ou testes criados.

Reutilizados `item_manager_event`, textos e estilos equivalentes, dividers, seta, fechar, chevron e check oficial `ic_employee_event_done` (raiz 18.2×18, verde #51BF1E). Status em #BCBACC; check não foi trocado por ícone circular.

Export de fechar e chevron tem pixels idênticos a `conversations_close` e `manager_events_chevron_down`. O único novo asset é `staff_event_expired.png`, renderizado do SVG oficial a 4x (raiz 18×18, vermelho #FF4D4D), sem duplicata PNG idêntica no projeto. Ícones do status mantêm suas proporções de raiz; chevron fica no slot de 20dp do componente. Sem URLs temporárias no código.

## Git e validação

Branch `feat/SCRUM-309-eventos-funcionario-ui` empilhada na SCRUM-311, base `e888e83`. A SCRUM-311 foi commitada com autorização explícita antes de iniciar esta tarefa; SCRUM-313 em `2cee85c` permanece sua base. Publicar/mergear **313 → 311 → 309**; fila completa em `docs/fila-de-prs.md`. Commit local da SCRUM-309 autorizado em 15/10/2026 para iniciar a SCRUM-301 em branch irmã, sem push ou PR.

`assembleDebug` e `lintDebug` aprovados, sem erros; somente aviso local de overdraw do fundo, também existente nas outras telas. `git diff --check` sem problemas de whitespace. Sem físico conectado: instalação/navegação e comparação visual em telefone/tablet pendentes. Emulador não utilizado. Sessão, credenciais e SDK preservados.
