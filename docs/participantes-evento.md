# Participantes do evento — SCRUM-291

UI e mocks da subtarefa de [SCRUM-265](https://luizacursino.atlassian.net/browse/SCRUM-265). Branch `feat/SCRUM-291-participantes-evento-ui`, aberta sobre a SCRUM-294 no commit `e3f2121`, conforme aprovação. Sem API, usuários reais, validação ou atribuição de conclusão.

## Referências

- [UI — SCRUM-291](https://luizacursino.atlassian.net/browse/SCRUM-291).
- [Autodeclaração — 2590:12465](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12465).
- [Atribuição — 2846:4281](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2846-4281).
- Integrações permanecem nas SCRUM-292 e SCRUM-293; o destino de validação é outra tarefa.

## Como visualizar

1. No fluxo gestor, abra Eventos, escolha um exemplo e toque em `Ver participantes` no detalhe.
2. O primeiro/terceiro eventos mostram autodeclaração: filtros Todos, Validados e Não validados.
3. O segundo/quarto mostram atribuição: aviso destacado e filtros Todos, Concluído e Não concluído.
4. São os três exemplos de cada frame, com nome repetido e retratos oficiais do Figma. A busca filtra o nome ao digitar; `Kevin` encontra os exemplos e `teste` mostra a mensagem vazia provisória.
5. Filtros e pesquisa são combinados somente nos mocks locais, sem estabelecer regras definitivas de status do backend.
   O status de quem ainda não terminou usa “Ainda não concluiu o evento”, conforme o texto atualizado pelo solicitante.
6. A primeira abertura mostra skeleton por 1 segundo. Ele está oculto na prévia XML; conteúdo rolável na RecyclerView e navbar fixa na Activity.
7. Voltar preserva o detalhe. Fechar retorna à Home do fluxo ativo. A linha de validação pendente informa que seu destino ainda está pendente, sem alterar conclusões. Na SCRUM-285, participantes ainda não concluídos do modo Atribuição passam a abrir a tela de atribuição com seus dados locais.

## Organização

Fragment, ViewModel, adapter e modelo em `manager/events/participants`, exemplos em `mock/ParticipantSamples`. Sem novos repositories ou dependências. O título e o modo vêm dos argumentos do detalhe; estado local de busca/filtro/loading fica no ViewModel.

Retratos exportados dos PNGs oficiais, sem alteração. Busca, X, ponto vermelho e check reutilizam recursos canônicos existentes, preservando suas proporções e tamanhos. Colunas da lista são proporcionais; os textos podem quebrar linha em celular/tablet.

## Pendências

- Mensagem de busca vazia é provisória: os dois frames não definem esse estado visual.
- Datas/contagens e usuários são mocks; não reconciliam os contadores demonstrativos da lista/detalhe.
- Integração da lista, autorização, validação e atribuição de conclusão não pertencem à UI.

## Correções adicionais pedidas pelo usuário

Registradas separadamente do escopo da SCRUM-291; os textos do commit e da PR ficam focados na UI de participantes, conforme pedido:

- `EmployeeEventConclusionFragment` espera 2 segundos enquanto a confirmação está visível. Ao retornar, remove a pilha de eventos e recria a Home, disparando seu skeleton existente de 2 segundos. A espera é cancelada ao sair da confirmação.
- O mês do calendário do colaborador usa `pt-BR`, como o seletor já usava. Os cabeçalhos são Dom, Seg, Ter, Qua, Qui, Sex e Sáb; datas numéricas continuam no padrão brasileiro `dd/MM/yyyy`.
- Nenhuma chamada à API, autenticação ou sessão foi alterada. A conclusão continua mock/local e a Home permanece com seus exemplos próprios, sem simular dados vindos do backend.

## Verificação

- Build debug e lint aprovados novamente após o texto atualizado; nesta UI há apenas o aviso informativo de overdraw do fundo também existir no tema.
- XMLs, diff, referências e hashes conferidos; os três retratos são idênticos nos dois frames e não duplicam bitmaps existentes. Ícones canônicos reutilizados e geometria/cor do check conferidas.
- Instalação e abertura realizadas no SM-A566E, preservando os dados. A restauração existente da sessão mostrou erro genérico; depois, o aparelho saiu do Astro. Sem alteração da API/autenticação para contornar esse estado.
- Conferência visual dos dois modos, calendário traduzido e sequência confirmação → Home/skeleton em execução permanece pendente, assim como previews de celular/tablet. Emulador não utilizado.
- Publicação autorizada pelo solicitante na branch empilhada da SCRUM-291; os dois ajustes do colaborador permanecem registrados separadamente acima.
