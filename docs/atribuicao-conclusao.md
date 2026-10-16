# Atribuição de conclusão de evento — SCRUM-285

UI e mocks da subtarefa de [SCRUM-268](https://luizacursino.atlassian.net/browse/SCRUM-268). Branch `feat/SCRUM-285-atribuicao-conclusao-ui`, criada diretamente da main atualizada no commit `293e7b2`. Sem API nem alteração real de conclusões.

## Referências

- [UI — SCRUM-285](https://luizacursino.atlassian.net/browse/SCRUM-285).
- [App V2 — 3287:16969](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=3287-16969).
- Integração e atribuição real permanecem na SCRUM-286. A validação de evidências é outra tela/tarefa.

## Como visualizar

1. No fluxo Gestor, abra Eventos e escolha o segundo ou quarto exemplo, no modo de atribuição pelo gestor.
2. No detalhe, toque em `Ver participantes` e escolha alguém que ainda não concluiu o evento.
3. A tela mostra o nome e retrato selecionados e o título do evento. Cargo, unidade e data de término são exemplos locais do frame.
4. O skeleton fica visível por 1 segundo, sem bloquear a interface. Na prévia XML, ele permanece oculto e o conteúdo aparece diretamente.
5. Voltar retorna à lista, preservando seus filtros. A linha do evento retorna ao detalhe, e o X retorna à Home do fluxo ativo.
6. `Atribuir conclusão` oferece feedback de toque e um aviso de integração pendente. O botão não tem destino definido no protótipo; não foi inventada uma confirmação nem alterado o estado do participante.

## Organização

Fragment em `manager/events/assignment`, layout e skeleton nos diretórios Android de recursos. Argumentos simples carregam os dados selecionados; não há ViewModel ou Repository vazio para essa tela estática.

A navbar continua na Activity, com Eventos selecionado, e não participa do push. Conteúdo rolável, colunas proporcionais e largura máxima usam o padrão atual de celular/tablet.

Retratos, ícones e estilos existentes foram reutilizados. Apenas o relógio com seta foi exportado do Figma, preservando sua proporção; o retrato do frame é idêntico ao recurso canônico existente.

## Verificação

- `:app:assembleDebug` e `:app:lintDebug` aprovados, sem erros. Há somente o aviso informativo de overdraw do fundo no novo layout, já usado pelo tema.
- Diff, referências tipadas e hashes dos recursos novos conferidos, sem órfãos ou duplicatas novas.
- APK recompilada e instalada com sucesso no Samsung SM-A566E em 2026-10-14, preservando os dados. Navegação e comparação visual da tela ainda não conferidas. Emulador não utilizado.
- Previews de telefone/tablet ainda não conferidos: o Android Studio estava em outro checkout e a interação foi interrompida por uso simultâneo da IDE.

## Pendências fora da UI

- Contrato da API, carregamento real do participante/evento e atribuição de conclusão na SCRUM-286.
- Destino ou confirmação do botão de atribuir conclusão, não configurado no frame consultado.
