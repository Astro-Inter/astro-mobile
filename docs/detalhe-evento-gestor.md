# Detalhes do evento do gestor — SCRUM-294

UI e mocks da subtarefa de SCRUM-264. A branch `feat/SCRUM-294-detalhes-evento-gestor` foi iniciada sobre a SCRUM-297, por solicitação, e atualizada por fast-forward para a `main` em `28a99c3`, após o merge da PR #31. Nenhuma chamada real, atribuição, validação ou envio de arquivo foi acrescentado.

## Referências

- [Autodeclaração — 2590:12439](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12439).
- [Atribuição do gestor — 2846:3855](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2846-3855).
- [UI — SCRUM-294](https://luizacursino.atlassian.net/browse/SCRUM-294). As lógicas específicas continuam nas SCRUM-295 e SCRUM-296.

## Visualização manual

1. Entre no fluxo gestor e abra Eventos na navbar.
2. O primeiro e o terceiro exemplos abrem autodeclaração, com os três indicadores do Figma. O segundo e o quarto abrem atribuição, sem esses indicadores.
3. Na primeira abertura de cada detalhe, o skeleton simula 1 segundo, sem bloquear a UI. Na prévia XML ele permanece oculto.
4. Confira título, descrição, anexo, participantes, prazo, validade, modo e remetente. Os valores são exemplos locais dos frames, não registros reais.
5. `Visualizar anexo` avisa que sua ação está pendente. `Ver participantes` abre a UI/mock da SCRUM-291 no mesmo modo de conclusão do evento.
6. Voltar retorna à lista preservando busca/filtro. Fechar retorna à Home do fluxo ativo. A navbar da Activity permanece fixa no push e mantém Eventos selecionado.

## Organização e visual

- Fragment e ViewModel em `manager/events`; modo de conclusão no modelo `ManagerEvent`, definido explicitamente em `mock/ManagerEventSamples`.
- O ViewModel guarda apenas se a espera mock já terminou; não há Repository, API, dependência nova nem alteração da autenticação.
- Um layout adapta as duas variantes com textos ajustáveis, colunas proporcionais e largura máxima já usada pelo projeto. Conteúdo rolável e navbar fora do Fragment.
- Emblema, olho e ampulheta exportados dos SVGs oficiais. A seta reutiliza `chat_send_arrow`, com hash idêntico ao export oficial. Calendário, relógio, notas e usuário reutilizados após comparação de geometria; check circular também conferido. Grupo usa o vetor canônico existente, com tint claro.
- O componente de olho foi conferido isoladamente: o contexto do frame retornava o asset do ponto padrão, não o ícone da instância. Foi usado o SVG correto da instância, preservando a proporção.
- O campo da variante de atribuição mostra `Atribuição`, corrigindo o texto incorreto do Figma conforme decisão aprovada.

## Pendências fora desta UI

- Os frames têm exemplos inconsistentes: a lista exibe 4 validações pendentes, e o detalhe tem 5 pendentes, 4 feitas e 1 conclusão pendente. Os números do detalhe foram mantidos como referência visual; unificar esses dados depende do contrato/design final.
- A UI/mock de participantes está na SCRUM-291. Sua integração, validações e atribuição real continuam em tarefas próprias. Anexos são somente referências visuais.
- API, regras de autorização e cálculo de indicadores não estão implementados neste card.
- Revisão visual em execução e previews de celular/tablet dependem de acesso ao dispositivo/editor. Nenhum emulador deve ser utilizado.

## Verificação

- `:app:assembleDebug` e `:app:lintDebug` aprovados sem erros. Quatro avisos informativos nesta tela: três sugestões de compound drawables e um de overdraw; não impedem o build.
- XMLs, diff e referências dos três assets novos conferidos; nenhuma duplicata binária nova. A seta idêntica já existente foi reutilizada.
- Apenas o emulador Pixel Tablet apareceu no ADB; ele não foi utilizado. Não houve instalação ou teste de navegação. Previews e comparação visual em celular/tablet permanecem pendentes.
- Com a PR #31 mergeada e a base atualizada, a PR da SCRUM-294 aponta diretamente para `main`, contendo somente os detalhes novos. A publicação não inclui merge automático; a revisão visual em dispositivo permanece pendente.
