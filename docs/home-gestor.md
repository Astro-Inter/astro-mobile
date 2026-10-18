# Home do Gestor — integração (SCRUM-582)

A Home consulta `GET /home/manager` com `Authorization: Bearer <Firebase ID token>`.
O contrato foi conferido no controller, DTOs e service da [API Astro](https://github.com/Astro-Inter/astro-api/tree/main/src/main/java/com/astro/api/home).
O endpoint exige uma conta ativa com papel GESTOR ou GESTOR_WORKSPACE; não recebe IDs de usuário ou workspace pela interface.

## Dados exibidos

| Campo da API | Uso na Home |
| --- | --- |
| `username` | Saudação, com o nome em negrito |
| `workspace_name` | Nome do workspace |
| `pending_validations` | Prévia de colaborador, título do evento e Novo/Visto |
| `pending_forms` | Prévia de título do formulário, prazo e ícone de estado |
| `indicators.current_events` | Eventos em seu nome ocorrendo |
| `indicators.pending_validations` | Total de validações pendentes |
| `indicators.collaborators_with_pending_events` | Colaboradores com pendências em seus eventos atuais |

As contagens vêm dos indicadores, independentemente da quantidade de itens das prévias.
As listas são montadas a partir da resposta; não há linhas de exemplo em execução.
Listas nulas ou vazias exibem mensagens independentes e ocultam o respectivo Ver mais.
Nome/workspace ausentes ou vazios e indicadores ausentes ou negativos são tratados como resposta inválida.

`days_left` é calculado pela API em dias de calendário no fuso America/Sao_Paulo.
A Home mostra Sem prazo para null, Prazo encerrado para negativos, Vence hoje para zero,
Resta 1 dia para um e Restam N dias para os demais.
O ícone de alerta tem prioridade quando faltam até dois dias, incluindo prazos vencidos;
fora dessa condição, itens não visualizados usam Novo e itens vistos mantêm a coluna sem ícone.

## Carregamento e autenticação

- O skeleton acompanha a requisição real, sem atraso artificial; o dissolve de 250 ms foi mantido.
- A consulta é refeita ao abrir ou retomar a Home, inclusive ao voltar de outra tela.
- Uma resposta 401 força a renovação do token e repete a consulta uma única vez. Outro 401 encerra a sessão.
- HTTP 403/404 mostram indisponibilidade de acesso, sem encerrar a sessão.
- Falhas de conexão, resposta inválida e erros do serviço exibem mensagem e Tentar novamente.
- O cliente compartilhado mantém o limite total de 15 segundos, inclusive na inicialização do Render.
- Ao pausar ou destruir a tela, a consulta e as animações são canceladas; callbacks antigos são descartados.
- O assistente compartilhado aparece depois da resposta e mantém arraste/recolhimento e acesso à IA.

## Navegação e limites

Conversas, notificações, colaboradores, formulários, IA e navbar mantêm os destinos existentes.
As prévias de formulários abrem a lista atual de formulários, cuja integração é uma tarefa separada.
Validações ainda mostram o aviso de funcionalidade pendente: as telas de validação existentes usam dados locais,
e os IDs reais da Home não são vinculados a esses exemplos. Nenhuma conclusão, rejeição ou marcação de visto é enviada.
Esta tarefa integra somente a leitura da Home.

## Estrutura

- `data/api/AstroApi`: rota Retrofit.
- `data/api/dto/ManagerHomeData`: contrato com campos em snake_case, IDs numéricos para validações e textuais para formulários.
- `data/api/ManagerHomeRepository`: token, consulta, renovação, validação da resposta e cancelamento.
- `manager/home/ManagerHomeFragment`: apresentação e ciclo de vida, seguindo a integração da Home do Colaborador.
- `fragment_manager_home.xml`: listas dinâmicas, estados vazios e nova tentativa; dados de exemplo somente no editor.
- `ManagerHomeRepositoryTest`: contrato HTTP/JSON com respostas locais, prazos, indicadores, autenticação, erros e descarte de callbacks.

## Validação em 18/10/2026

`assembleDebug`, `lintDebug` e `testDebugUnitTest` aprovados: 50 testes, incluindo 12 novos
da Home do Gestor, sem falhas; lint sem erros e com 111 avisos no projeto.
XMLs bem formados e diff sem erros de whitespace. APK debug gerado, sem instalação nesta tarefa.
A execução local usou Java 25 e TEMP/TMP em `app/build/tmp` para evitar o limite de caminho
dos sockets locais do Java no diretório temporário do ambiente; nenhuma configuração do projeto foi alterada para isso.

## Validação manual pendente

Com uma conta real de gestor, conferir nome/workspace, indicadores, prévias, estados vazios e prazos.
Verificar nova tentativa após desconexão, retorno à Home, ações rápidas e assistente.
Build, testes locais e lint não substituem essa conferência autenticada no aparelho.
