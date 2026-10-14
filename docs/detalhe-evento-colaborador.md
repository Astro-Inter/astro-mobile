# Detalhe de evento do colaborador — SCRUM-281

Escopo: UI e mocks. Não há chamada de API, leitura de documentos ou envio de evidência.

## Referências

- Detalhe: [Figma 2593:17113](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2593-17113).
- Confirmação: [Figma 2593:17152](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2593-17152).
- UI: [SCRUM-281](https://luizacursino.atlassian.net/browse/SCRUM-281). A integração pertence à SCRUM-282.

## Como visualizar

1. Abra Eventos no fluxo colaborador.
2. Toque em uma linha da lista ou em um evento do resumo de um dia do calendário.
3. Na primeira abertura do detalhe, o skeleton fica visível por um segundo. O atraso não bloqueia a interface.
4. O evento pendente mostra a evidência de exemplo e a ação de concluir. Remover e recolocar a evidência só altera o protótipo.
5. Concluir abre a confirmação com o check verde por 2 segundos. Depois, retorna à Home do colaborador, recriada para executar novamente seu skeleton existente. Voltar pelo Android antes do retorno automático mantém o comportamento da lista e cancela o temporizador.

A conclusão não é persistida: trocar o mês ou reabrir o fluxo pode recarregar os mocks originais. A validade de dois anos, o remetente e o arquivo são exemplos, não regras de negócio nem dados reais. Eventos em análise ou concluídos não repetem a ação de conclusão neste protótipo.

## Estrutura

- Fragments e ViewModel em `employee/events`; dados locais em `employee/events/mock`.
- Argumentos simples no único `nav_graph.xml`; a navbar continua na `MainActivity`, fora da transição.
- A confirmação não exibe navbar, conforme seu frame. O retorno automático de 2 segundos foi solicitado posteriormente; ele remove a pilha de eventos e recria a Home sem alterar a autenticação nem adicionar outro loading.
- Skeleton oculto no XML para permitir a visualização da tela real no editor.
- Layout rolável, largura máxima do projeto e textos sem alturas fixas para permitir quebra em celular e tablet.

## Pendências

- API e contrato dos detalhes, anexos e conclusão.
- Escolha, leitura e envio real de evidências.
- Revisão visual em dispositivo físico e nas prévias de celular/tablet do Android Studio.

## Verificação executada

- `:app:assembleDebug` e `:app:lintDebug` aprovados, sem erros, em 2026-10-13.
- `git diff --check`, parsing dos XMLs, referências dos novos recursos e hashes dos bitmaps conferidos; nenhuma duplicata binária encontrada.
- Ícones novos exportados sem redesenho a partir dos assets do Figma. Calendário e relógio reaproveitados após conferir sua geometria.
- O lint mantém avisos informativos no projeto; nos novos layouts, apenas alertas de possível overdraw pelo fundo também existir no tema.
- Nenhum celular físico conectado. Não houve instalação, teste em execução nem uso do emulador disponível.
