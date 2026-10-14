# Eventos do gestor — SCRUM-297

UI e mocks, sob a tarefa SCRUM-263. Não há API, criação de eventos, validação ou atribuição real de conclusões.

## Referências

- [Lista no Figma — 2590:12341](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12341).
- [Estado vazio — 2590:12353](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12353).
- [UI — SCRUM-297](https://luizacursino.atlassian.net/browse/SCRUM-297). A integração é a SCRUM-298.

## Visualização manual

1. Entre no fluxo gestor e toque em Eventos na navbar.
2. O skeleton simula um segundo sem bloquear a interface e não aparece na prévia XML.
3. São quatro exemplos do Figma: validação pendente, atribuição de conclusões, nenhuma ação e finalizado.
4. Use o dropdown para mostrar Todos, Em andamento ou Finalizados. É apenas filtragem local: os três exemplos não finalizados pertencem ao grupo Em andamento.
5. Pesquise `EPI` para encontrar os exemplos ou `teste` para mostrar o estado vazio. A busca ignora maiúsculas e acentos.
6. O frame vazio usa os três chips; o preenchido usa dropdown. Ambos controlam o mesmo filtro, mantendo a diferença visual dos frames.
7. Limpe a pesquisa para restaurar a lista. Voltar ou fechar retorna à Home do gestor; a navbar não participa do push.

Os quatro títulos repetidos são os exemplos do Figma, não quatro registros reais. Não há exclusão, conclusão ou persistência de dados.

## Organização

- Fragment, ViewModel, adapter e modelo em `manager/events`, com exemplos em `manager/events/mock`.
- RecyclerView sem ScrollView externo; cabeçalho, busca e filtro ficam acima da área rolável da lista.
- Colunas proporcionais, alturas de texto ajustáveis e largura máxima já usada no projeto, sem duplicar layouts para tablet.
- A Activity seleciona Eventos conforme o fluxo ativo e preserva a lista/calendário do colaborador.
- Sem novas dependências, repositories vazios ou acesso à API/Firebase.

## Verificação e pendências

- Build debug e lint aprovados sem erros. XMLs, diff, referências e hashes dos assets conferidos.
- Novos assets de busca, chevron e caderno exportados dos SVGs oficiais sem redesenho. X e ponto vermelho reutilizados após comparação; check mantém geometria e cor oficiais.
- Nenhum dispositivo físico conectado. Não houve instalação, teste em execução ou uso do emulador disponível. A comparação visual em celular/tablet permanece pendente.
- Os detalhes e seus destinos ficam na SCRUM-294; por enquanto, o toque na linha só informa essa pendência. Foi aprovada a correção do campo para `Atribuição` na futura variante do gestor.
- API, regras definitivas de estados e interações reais ficam na SCRUM-298 e nas tarefas específicas de detalhe/participantes/validação.
