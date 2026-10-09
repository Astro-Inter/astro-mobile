# Home do Gestor — UI (SCRUM-340)

## Escopo

UI e mocks do App V2, frame `2590:12241`, subtarefa da SCRUM-243.
Implementação em `manager/home/ManagerHomeFragment` e `fragment_manager_home.xml`.
Nome, workspace, indicadores e prévias são mocks de `manager_home_strings.xml`.
A integração da Home permanece na SCRUM-341; nenhum endpoint novo foi implementado.

## Comportamento

- Escolher Gestor abre sua Home; Colaborador mantém a Home existente.
- Skeleton de 1 segundo, sem bloquear a interface, seguido de dissolve de 250ms.
  O editor XML mostra a Home; retornar à mesma instância não repete a espera.
- Ações rápidas com rolagem horizontal, prévias com títulos flexíveis e indicadores pareados.
- Chat da navbar e atalho Conversar abrem a listagem; o mascote abre a IA diretamente.
- Conversas muda seu texto conforme o fluxo ativo; chat, sessões e encerramento
  compartilham o ViewModel da Home correta.
- Navbar única na Activity, fixa durante as transições e oculta com o teclado no chat.
- `HomeAiBubble` é reutilizada nas duas Homes: texto aberto por 3 segundos,
  recolhimento para a direita em 500ms e arraste entre cabeçalho e navbar.
- O último fluxo fica em `FlowPreferences`; isso não substitui a sessão Firebase
  nem a autorização do backend.
- Destinos de validações, formulários, colaboradores e notificações continuam pendentes,
  com aviso local ao toque.

## Separação do Perfil

Branch `feat/SCRUM-340-home-gestor-main`, criada diretamente de `origin/main` (`a430b9d`).
A PR #25 entrou na branch do Perfil, não na main. Esta versão reaplica somente a Home
e os componentes de navegação necessários, sem alterar aquela branch.

Não inclui tela de Perfil, GET /user/me, avatar, toggle de notificações, alteração de senha
logada ou pop-up de logout. A aba Perfil continua com o aviso de funcionalidade pendente.
O botão Sair provisório da Home do Colaborador permanece até o Perfil entrar na main.
O ícone User Group é reutilizado na Home; não adiciona funcionalidade de Perfil.

## Verificação

Em 2026-10-09: assembleDebug, lintDebug e testDebugUnitTest aprovados,
com 20 testes existentes sem falhas. Não foram criados novos testes, usados emuladores
ou executadas instalações nesta separação. A validação visual anterior da Home é preservada;
o novo encaixe sobre a main não foi repetido em dispositivo.
