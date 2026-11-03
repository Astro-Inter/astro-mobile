# Astro Mobile — Status de implementação das telas

Retrato do código baseado na main `119a25a`, atualizado em 2026-11-02. CONCLUÍDO indica implementação presente; não certifica deploy do backend nem aprovação de todo o QA integrado.

Limite de mensagens nos chats: **CONCLUÍDO**; limite de 4.000 unidades UTF-16 e contador a partir de 3.600 nos dois fluxos. Build debug e testes unitários existentes aprovados; instalado no Samsung para validação manual.

Refatoração de arquitetura e limpeza: **CONCLUÍDO** nesta branch. Recursos órfãos e mock sem uso removidos; consultas da IA e identidade Firebase isoladas da apresentação; recuperação de erros separada por fluxo. Builds debug/release e 194 testes unitários aprovados; 25 testes instrumentados locais aprovados no Samsung SM-A566E. Lint: 0 erros, 118 avisos e nenhum recurso sem uso. Release otimizado abriu a Home com dados reais; licença Montserrat preservada. Versão debug final instalada com dados locais preservados. Isso não substitui QA completo de todos os fluxos com API real.

| Tela | Fluxo | XML | Lógica | Navegação | Integração | Resumo atual | Atualizado em |
|---|---|---|---|---|---|---|---|
| Visualização de evidência de evento | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Evidências reais: imagens no Astro; demais arquivos em aplicativo compatível. | 2026-11-02 |
| Validação de conclusão de evento | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Consulta e análise de envios feitos até o prazo da turma, inclusive após o encerramento; bloqueio local quando as datas não permitem confirmar o prazo. Status cancelado é consultado pelo mobile; a garantia de bloqueio no servidor depende do backend. | 2026-11-02 |
| Confirmação de validação de evento | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Confirmação da validação e retorno ao contexto do gestor. | 2026-11-02 |
| Fora da localização do formulário | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Aviso de saída do raio de 100 m e retorno aos detalhes com respostas preservadas. | 2026-11-02 |
| Resposta do formulário de inspeção | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Perguntas dinâmicas, Outros, câmera, multipart e revisão das fotos enviadas; GPS somente local. | 2026-11-02 |
| Conversa entre gestor e colaborador | Gestor e Colaborador | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Contatos, histórico paginado, STOMP, leitura, fotos e acesso ao perfil pelo gestor. | 2026-11-02 |
| Eventos do funcionário selecionado | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Eventos reais do colaborador; filtros e detalhe conforme modo de conclusão. | 2026-11-02 |
| Detalhes do funcionário | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Perfil real por ID, foto, NRs e acesso às pendências. | 2026-11-02 |
| Lista de colaboradores | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Consulta mobile com contagem de pendências, busca e filtros locais. | 2026-11-02 |
| Atribuição de conclusão de evento | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Detalhe e atribuição reais; bloqueio local quando a turma encerrou ou o prazo é indisponível. Status cancelado é consultado pelo mobile; a garantia no servidor depende do backend. | 2026-11-02 |
| Lista de formulários | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Lista de recebidos real; reprovado com X e busca instantânea. | 2026-11-02 |
| Detalhes do formulário de inspeção | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Detalhe real; sem unidade oculta o campo; com unidade exige localização precisa e raio de 100 m. | 2026-11-02 |
| Alertas | Gestor | NÃO SE APLICA | NÃO SE APLICA | NÃO SE APLICA | NÃO SE APLICA | Funcionalidade removida do fluxo de gestor por decisão de produto; preferência de push preservada no perfil. | 2026-11-02 |
| Alertas | Colaborador | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Lista, detalhe e confirmação de leitura reais; acesso direto pela Home e indicação de não lidos. | 2026-11-02 |
| Participantes do evento | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Participantes reais; na atribuição, mostra prazo encerrado quando a última turma terminou; envios pendentes de validação preservam seu estado. Build e testes aprovados; caso vencido conferido no Samsung. | 2026-11-02 |
| Eventos do Gestor | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Lista por modo/status, contagem e filtros; prazo encerrado depende de lastClassLimitDate no retorno da API. | 2026-11-02 |
| Detalhes do evento do Gestor | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Detalhe real e acesso aos participantes conforme modo de conclusão. | 2026-11-02 |
| Detalhe de evento do Colaborador | Colaborador | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Detalhe real, material, evidência obrigatória/opcional e envio de conclusão. | 2026-11-02 |
| Confirmação de conclusão de evento | Colaborador | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Confirmação após resposta bem-sucedida da API. | 2026-11-02 |
| Eventos do Colaborador | Colaborador | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Lista real independente do calendário; busca, filtros e prioridade de estados. | 2026-11-02 |
| Home do Gestor | Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Dados reais, atalhos e validação direta por IDs do evento e participante. | 2026-11-02 |
| Splash | Inicialização | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Restaura a sessão e encaminha ao fluxo autorizado. | 2026-11-02 |
| Identificação de e-mail | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Verificação do e-mail e encaminhamento para login ou primeiro acesso. | 2026-11-02 |
| Perfil | Colaborador e Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Dados reais, avatar, NRs e preferência de notificações no dispositivo. | 2026-11-02 |
| Alteração de senha — solicitação | Colaborador e Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Solicita recuperação ao serviço Astro; redefinição concluída no Web. | 2026-11-02 |
| Alteração de senha — confirmação | Colaborador e Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Confirmação genérica da solicitação sem revelar existência da conta. | 2026-11-02 |
| Erro de conexão | Compartilhado | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Apresentação compartilhada; retorno preserva tela carregada e retry pertence ao fluxo de origem. | 2026-11-02 |
| Erro genérico | Compartilhado | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Fallback compartilhado; autenticação separada das consultas autenticadas. | 2026-11-02 |
| Tela informativa da chave de acesso | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Conteúdo estático da chave de acesso. | 2026-11-02 |
| Chave do primeiro acesso | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Validação da chave e encaminhamento ao cadastro. | 2026-11-02 |
| Login com senha | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Firebase Authentication e encaminhamento ao fluxo autorizado. | 2026-11-02 |
| Solicitação de redefinição de senha | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Serviço Astro separado, timeout de 70 s, sem retry automático, erros por código e X-Request-ID. | 2026-11-02 |
| Confirmação de redefinição de senha | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Mensagem genérica após confirmação da API; troca de senha pelo navegador. | 2026-11-02 |
| Definição da senha inicial | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Criação da conta Firebase e ativação com a chave. | 2026-11-02 |
| Escolha de fluxo | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Seleção dos fluxos permitidos e preferência local. | 2026-11-02 |
| Home do Colaborador | Colaborador | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Eventos, formulários e Alertas reais; atalhos e estado de leitura. | 2026-11-02 |
| Astrinho | Colaborador | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Astrinho integrado: mensagens, blocos estruturados, arquivos e calendário por Repository. | 2026-11-02 |
| Conversas | Colaborador e Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Lista real de conversas, busca instantânea e avatares. | 2026-11-02 |
| Pesquisa de nova conversa | Colaborador e Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Contatos reais; cria conversa somente após enviar a primeira mensagem. | 2026-11-02 |
| Sessões do Astrinho | Colaborador e Gestor | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | Histórico real do Astrinho, skeleton, busca e abertura de conversas. | 2026-11-02 |

A validação completa de UI, dados, permissões e envio de ações continua exigindo QA integrado. Exemplos usados somente na prévia XML usam `tools:` e não representam dados em execução.
