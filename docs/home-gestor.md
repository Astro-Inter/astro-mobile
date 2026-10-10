# Home do Gestor — UI (SCRUM-340)

## Escopo

Subtarefa de UI da SCRUM-243. Referência: App V2, frame `2590:12241` do Figma Astro.
Implementação em `manager/home/ManagerHomeFragment` e `fragment_manager_home.xml`.
Nome, workspace, contagens e prévias vêm de mocks identificados em `manager_home_strings.xml`.
Nenhum endpoint da Home foi inventado ou implementado; a integração fica para a SCRUM-341.

## Comportamento

- Escolher Gestor abre esta Home; escolher Colaborador preserva a Home existente.
- Skeleton simula 1 segundo sem bloquear a interface e faz um dissolve de 250ms.
  Fica oculto no XML para visualizar a Home no editor. Ao retornar de outra tela, não repete a espera na mesma instância.
- Atalho Conversar e aba Chat abrem a mesma listagem; o mascote abre diretamente a IA existente.
- A listagem usa o texto do frame `3272:11270`: “Acesse aqui as suas conversas com os colaboradores”.
  Quando o Gestor escolhe atuar como Colaborador, volta ao texto do fluxo de Colaborador.
- Perfil, alteração de senha e logout reutilizam a implementação da SCRUM-255.
  A navbar continua na Activity, fora das transições dos Fragments.
- O assistente reutiliza `shared/home/HomeAiBubble` nas duas Homes: texto aberto por 3 segundos,
  recolhimento para a direita em 500ms e arraste limitado entre cabeçalho e navbar.
  Ao retornar à Home, reabre o texto; ao sair, cancela os callbacks e animações.
- O último fluxo escolhido é mantido como preferência de interface e limpo no logout.
  Não substitui o tipo da conta, a sessão Firebase nem a autorização do backend.
- Validações, formulários, colaboradores e notificações exibem apenas o aviso de funcionalidade pendente.
  As prévias têm feedback de clique, mas não abrem telas inexistentes.

## Estrutura e adaptação

Atalhos têm rolagem horizontal; prévias e indicadores usam altura flexível e textos com quebra de linha.
A largura máxima e a tipografia reutilizam os recursos de telefone/tablet já existentes.
As prévias são seis itens curtos dentro da rolagem da Home, não uma lista paginada de API.
O ícone Notes veio do Figma e foi convertido em VectorDrawable, sem duplicata PNG.

Para a integração, substituir os mocks por dados do ViewModel/Repository e retirar o atraso artificial.
Não foi criado ViewModel ou Repository vazio para uma UI estática.

### Ajustes visuais de 2026-10-07

- Cabeçalhos roxos usam setas explícitas de 20dp junto ao texto e ícones de 18dp à direita.
- As prévias do Gestor usam `item_manager_home_feed_row`, com altura livre para títulos longos,
  status em uma linha e espaço de ícone reservado mesmo quando não há ícone.
- Indicadores mantêm altura mínima de 117dp, cartões pareados com a mesma altura e rótulos na base.
- Removido um contêiner redundante; retirados o logout provisório da Home do Colaborador e suas referências sem uso.
  O logout real no Perfil permanece intacto.
- `assembleDebug` e `lintDebug` aprovados, sem erros de lint. A instalação desta revisão não foi possível:
  o Samsung desconectou. Conferência visual da Home autenticada ainda pendente; nenhum emulador foi usado.

## Base Git e revisão

Branch: `feat/SCRUM-340-home-gestor-ui`, na worktree `home-gestor-ui`.
Alinhada por fast-forward ao commit `a290f8b` do Perfil (PR #24), em 2026-10-09.
O snapshot anterior foi preservado em stash, e somente o delta da Home foi reaplicado.
Cadastro Firebase/ativação e sessões da IA atuais permanecem intactos.
A PR da Home tem como base `feat/SCRUM-255-perfil-usuario`, sem repetir o diff do Perfil.
Não mergear a Home na branch do Perfil: aguardar a PR #24 entrar na main e então atualizar
a base da PR da Home para main, alinhando o histórico se necessário.

### Validação para a PR — 2026-10-09

- `assembleDebug`, `lintDebug` e `testDebugUnitTest` aprovados: 20 testes, sem falhas.
- Lint sem erros; 76 avisos no projeto, sendo cinco nos novos layouts
  (pesos aninhados, possíveis fundos sobrepostos e quantidade de Views).
- Recursos novos referenciados e sem duplicatas por hash; diff sem conflitos ou erros de whitespace.
- Chat, sessões e confirmação de encerramento usam o ViewModel da Home do fluxo ativo.
- Nenhum novo teste, emulador ou instalação executado nesta preparação.

Revisão estática: XMLs bem formados, referências locais de recursos resolvidas,
classes dos destinos presentes e diff sem erros de whitespace.
A implementação inicial foi entregue sem build ou instalação, conforme o pedido daquele turno.
Em 2026-10-07, após autorização de instalação, assembleDebug passou e a APK foi instalada
no Samsung SM-A566E com atualização sem apagar os dados do app. Não houve abertura,
testes de navegação ou uso do emulador. Validação visual e de execução em telefone/tablet continuam pendentes.

## Integração com a main — 2026-10-10

A branch feat/SCRUM-255-perfil-usuario incorporou a main ed742e1 por merge.
As seções de base Git acima registram a preparação anterior das PRs.
Nesta branch, Perfil, alteração de senha e logout permanecem integrados;
a navegação também inclui Eventos do Colaborador e as telas de erro atuais da main.
