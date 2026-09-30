# Astro Mobile — Status de implementação das telas

Este arquivo registra somente telas cuja implementação já começou. A ausência de uma tela não significa que ela não exista no Figma ou no escopo do aplicativo.

Estados: `NÃO INICIADO`, `EM ANDAMENTO`, `CONCLUÍDO`, `BLOQUEADO` e `NÃO SE APLICA`.

| Tela | Fluxo | XML | Lógica/mock | Navegação | Integração | Resumo atual | Atualizado em |
|---|---|---|---|---|---|---|---|
| Splash | Inicialização | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Prioriza sessão Firebase válida e consulta tipo/status na API antes de restaurar o fluxo; sem sessão, preserva temporariamente o primeiro acesso mock ou abre identificação. Sem teste em device neste turno. | 2026-09-30 |
| Identificação de e-mail | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | POST /verify-email via Retrofit; limite de 15s cancela chamada pendente e abre erro interno, sem confundir timeout do Render com falta de internet. Build aprovado e app instalado/aberto no SM-A566E; timeout não foi disparado manualmente. | 2026-09-30 |
| Erro de conexão (mock) | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Destino reutilizado pela API e pelo Firebase; tentar novamente repete o POST ou retorna ao login/splash conforme a origem. Sem tela final do Figma. | 2026-09-30 |
| Erro interno (mock) | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Destino para respostas internas e timeout da API; tentar novamente repete o POST conforme a origem. Sem tela final do Figma. | 2026-09-30 |
| Tela informativa da chave de acesso | Autenticação | EM ANDAMENTO | NÃO INICIADO | EM ANDAMENTO | NÃO INICIADO | Layout do frame 2590:12283, entrada por “Saiba mais”, retorno por “Voltar”/“Entendi” e seta vetorial transparente com ripple circular; aguardando validação visual. | 2026-09-22 |
| Chave do primeiro acesso | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Estados padrão e inválido dos frames 2590:12512 e 2590:12497, entrada de seis dígitos e retorno inválido local; a chave mock 111111 abre a definição da senha. Build aprovado, aguardando validação visual. | 2026-09-23 |
| Login com senha | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Login pelo Firebase; credencial inválida no campo, conexão/interno nos destinos mock. Recuperação redireciona para fluxo próprio. Build aprovado. | 2026-09-30 |
| Solicitação de redefinição de senha | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Formulário pré-preenchido, validação local, envio pelo Firebase e erros de conexão/interno nas telas mock; resposta neutra para conta inexistente/desativada. Build aprovado, sem validação em device neste turno. | 2026-09-30 |
| Confirmação de redefinição de senha | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Exibe confirmação neutra com o e-mail solicitado; botão e Voltar retornam à identificação de e-mail, inclusive após nova tentativa na tela de erro. Build aprovado; lint bloqueado por dois erros preexistentes no skeleton da Home. | 2026-09-30 |
| Definição da senha inicial | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Senhas iguais e preenchidas mantêm o mock; Toast mostra tipo bruto da API. Colaborador abre Home, Gestor escolhe perfil. Sessão local mock persiste até Sair. | 2026-09-28 |
| Escolha de fluxo | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | EM ANDAMENTO | Gestor/Gestor Workspace escolhem Colaborador e guardam somente o fluxo ativo na sessão Firebase; primeiro acesso segue mock e Home do Gestor ainda não existe. | 2026-09-30 |
| Home do Colaborador | Colaborador | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | EM ANDAMENTO | Home e skeleton ainda mock; Sair encerra Firebase, limpa estado local e volta ao e-mail sem retorno pela pilha. Eventos, Chat, Perfil, notificações e IA aguardam integração. | 2026-09-30 |
