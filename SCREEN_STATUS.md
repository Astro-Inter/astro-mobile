# Astro Mobile — Status de implementação das telas

Este arquivo registra somente telas cuja implementação já começou. A ausência de uma tela não significa que ela não exista no Figma ou no escopo do aplicativo.

Estados: `NÃO INICIADO`, `EM ANDAMENTO`, `CONCLUÍDO`, `BLOQUEADO` e `NÃO SE APLICA`.

| Tela | Fluxo | XML | Lógica/mock | Navegação | Integração | Resumo atual | Atualizado em |
|---|---|---|---|---|---|---|---|
| Splash | Inicialização | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Splash animada e dissolve preservados; ao abrir o app, a sessão local mock direciona para a escolha de perfil (Gestor) ou Home (Colaborador). Sem sessão, segue para identificação de e-mail; Splash sai do back stack. | 2026-09-28 |
| Identificação de e-mail | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | POST /verify-email via Retrofit; vazio/inválido bloqueado no front, não cadastrado e DESATIVADO com mensagens vermelhas distintas sob o input, conexão e erro interno em destinos mock. E-mail e tipo propagados. | 2026-09-28 |
| Erro de conexão (mock) | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Destino mock reutilizável: tentar novamente repete o POST e voltar preserva o e-mail. Sem tela final do Figma por decisão do usuário. | 2026-09-25 |
| Erro interno (mock) | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Destino mock reutilizável: tentar novamente repete o POST e voltar preserva o e-mail. Sem tela final do Figma por decisão do usuário. | 2026-09-25 |
| Tela informativa da chave de acesso | Autenticação | EM ANDAMENTO | NÃO INICIADO | EM ANDAMENTO | NÃO INICIADO | Layout do frame 2590:12283, entrada por “Saiba mais”, retorno por “Voltar”/“Entendi” e seta vetorial transparente com ripple circular; aguardando validação visual. | 2026-09-22 |
| Chave do primeiro acesso | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Estados padrão e inválido dos frames 2590:12512 e 2590:12497, entrada de seis dígitos e retorno inválido local; a chave mock 111111 abre a definição da senha. Build aprovado, aguardando validação visual. | 2026-09-23 |
| Login com senha | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Senha não vazia mantém o mock; Toast mostra tipo bruto da API. Colaborador abre Home, Gestor escolhe perfil. Sessão local mock persiste até Sair; sem autenticação real (Firebase pertence à SCRUM-329). | 2026-09-28 |
| Definição da senha inicial | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Senhas iguais e preenchidas mantêm o mock; Toast mostra tipo bruto da API. Colaborador abre Home, Gestor escolhe perfil. Sessão local mock persiste até Sair. | 2026-09-28 |
| Escolha de fluxo | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Gestor/Gestor Workspace chegam após senha mock, com tipo preservado; Colaborador abre Home e atualiza destino persistido da sessão mock; Gestor segue sem Home implementada. Persistência real ainda pertence à SCRUM-323. | 2026-09-28 |
| Home do Colaborador | Colaborador | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Estados preenchido/vazio e interações mock; skeleton de 2 s e crossfade, balão da IA temporário/arrastável. Botão provisório Sair limpa sessão local mock e retorna à identificação de e-mail. Eventos, Chat, Perfil, notificações e IA aguardam telas/integrações. | 2026-09-28 |
