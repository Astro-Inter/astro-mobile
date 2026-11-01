# Arquivo PDF e homologação da PR #34

## Revisões e escopo

Referência examinada: [PR #34 da API de IA](https://github.com/Astro-Inter/astro-ai-api/pull/34), head `60dfa7cfada3d9f36595fb03eb24fe9b12664ea8`. A PR estava mesclada em 01/11/2026. A comparação com `main` encontrou somente o merge posterior `a7fbe680520c90355f17f766fb6d21efce4e8f38`. Contratos lidos dessa revisão: `docs/arquivos-chat.md`, `docs/homologacao-ampliada-pr34.md` e `app/modules/chat/schemas.py`.

No início da rodada, produção (`https://ia.astro-app.solutions/version`) ainda retornava `3ad06be7bf696bac04d954439e63fd8be5d66c79`, anterior à correção. Merge não comprova publicação. Não há deploy ou merge executado por este trabalho. O repositório desta tarefa é Android; o cliente web não foi alterado nem homologado aqui.

## Implementação

- `data/ai/ChatFileBlock`: modelo Gson próprio, validação de arquivo v1 e estado de disponibilidade calculado com a expiração original. UUID opaco, nome seguro com até 100 caracteres, MIME PDF, tamanho opcional de 1 a 2.000.000 bytes, URL HTTPS opcional sem credenciais embutidas e data com fuso. Nenhum endpoint ou URL é construído pelo cliente.
- `chat/ChatFileContent`: cartão separado do texto, ícone de documento, nome, tamanho quando disponível e botão Baixar PDF. Indisponível/expirado não oferecem ação. Expiração é conferida na renderização, ao retornar à tela, enquanto visível e novamente ao tocar. O relógio local ajuda a interface; não substitui autorização do servidor. Metadados e URL não aparecem como texto copiável nem entram em logs. Detalhes da foto continuam no carregador específico de avatar.
- `data/ai/ChatFileRepository`: download somente após toque, sem Firebase/AiApiClient, Authorization, cookies ou redirecionamentos. Cliente HTTPS dedicado, sem retry automático, prazo de 45 segundos, limite de 2 MB, MIME e assinatura `%PDF-` verificados antes de salvar. Armazena o PDF solicitado em Downloads/Astro via MediaStore, sem permissões adicionais de armazenamento no Android 11+. Registros parciais são removidos em falhas. Não grava a URL assinada no MediaStore, não altera a assinatura e não busca preview automaticamente. Downloads simultâneos do mesmo UUID são impedidos enquanto ativos; o usuário pode repetir explicitamente após conclusão/falha.
- `ChatStructuredContent`: aceita `arquivo` sem exigir um título inexistente no contrato. Mantém a mesma ordem e o mesmo caminho para POST/histórico. Um arquivo inválido não apaga os demais blocos. Mensagens legadas não são migradas ou regeneradas.

Indicadores, matriz, contexto, ficha, gráficos e tabela v1/v2 continuam existentes. O paginador usa os ícones Figma já implementados e não chama o chatbot. Nenhum gráfico é recalculado a partir da página visual. “Cadê o PDF?” depende da recuperação pelo backend; o mobile apresenta o id/URL/prazo recebidos sem renovação. Falha de geração sem bloco não cria cartão de sucesso.

## Testes simulados

- JVM: validação dos campos/versão, limites, URL/nome inválidos, estado/prazo, tamanho ausente e igualdade do arquivo no POST/histórico.
- Instrumentação: ordem texto/arquivo/bloco desconhecido, cartão indisponível, URL ausente, data vencida, tamanho ausente, ausência de cartão quando geração falha e expiração enquanto a tela permanece aberta. Captura visual usa dados fictícios e endereço `files.example`; o cartão não dispara rede para esse endereço.
- Download: transporte HTTPS interceptado, sem chamada externa, com persistência real no MediaStore; confirma conteúdo, ausência de Authorization/Cookie e limpeza dos arquivos criados pelo teste. Casos HTTP 403/404/500/302, conteúdo inválido, tamanho excessivo e metadados indisponíveis não criam PDF de sucesso. Esse teste não comprova acesso real ao armazenamento da API.
- Fluxo Fragment/ViewModel/Repository: mudanças de intenção, pedidos de 100 e depois 500 (fixture retorna 141 disponíveis), gráficos agregados e linhas com 12 zeros, tabela de eventos com referência, texto de ambiguidade e repetição do mesmo arquivo no fim da conversa. É uma resposta de API simulada, sem provar roteamento/SQL/Juiz/persistência do backend. Acesso negado é simulado por HTTP 403. O teste anterior verifica que paginar não aumenta chamadas nem altera o gráfico.

## Repetição real após publicação

Confirme que `/version` contém a revisão corrigida antes de homologar:

1. Na mesma conversa: cobertura → matriz → comparação de unidades → listagem. Verifique os blocos e reabra o histórico.
2. Liste 100 usuários; depois peça 500. Confira solicitada/retornada/total/limite, sem assumir que existem 500 pessoas.
3. Peça barras por cargo com base na listagem. A agregação deve consultar todo o escopo autorizado e permanecer igual ao trocar páginas locais.
4. Peça evolução mensal das próprias conclusões. Preserve os 12 zeros quando vierem do backend, sem preencher meses por conta própria em outros fluxos.
5. Teste vazio/sem base/rejeição/indisponibilidade sem fabricar cadastros ou valores.
6. Liste eventos, escolha uma referência presente e autorizada, peça o resumo; nomes ambíguos devem pedir esclarecimento. Não escolha pessoa/evento automaticamente.
7. Peça explicitamente PDF de uma consulta autorizada. Confira cartão, download em Downloads/Astro, “Cadê o PDF?”, histórico e expiração original. Não publique a URL temporária nas evidências. Arquivo já baixado não é revogado por expirar o link.

Os testes reais de geração/upload, reuso/persistência e leitura do arquivo remoto permanecem dependentes da publicação corrigida e de dados/conta autorizados. Não contorne guardrails ou permissões para produzir um caso positivo.

## Resultados da rodada de 01/11/2026

Durante o trabalho, `/version` passou para `a7fbe680520c90355f17f766fb6d21efce4e8f38`, que contém a referência `60dfa7c`. A publicação não foi executada por esta tarefa. As consultas seguintes foram feitas pela interface real no Samsung SM-T225, Astro debug 1.0 (1), com sessão autorizada já aberta pelo usuário.

- Cobertura: resposta explícita de não validação, sem liberar componentes. Não se declara homologação positiva dos indicadores desse pedido; o mobile apresentou a recusa normalmente.
- Matriz: indicadores + matriz + contexto, 100 de 272 pares; todos os tipos aceitos pelo parser no histórico. Comparação posterior: indicadores + tabela de três unidades + barras + contexto.
- Listagem de 100 seguida de 500 na mesma conversa: 100/141 e depois 141/141, com `solicitada=500`, `limitado=false` e `tem_mais=false` no segundo pedido. Os valores são um retrato, não constantes do aplicativo.
- Barras baseadas na listagem: resposta real informou agregação completa pelos filtros, incluindo critério de UID. Categorias somaram 141. O teste instrumentado com transporte interceptado confirma que paginar não chama rede nem altera o gráfico; não se instrumentou contagem HTTP da produção.
- Série pessoal: diagnóstico somente GET e reabertura do histórico confirmaram `grafico_linhas` com 12 categorias e soma zero. Um pedido de evolução mensal anterior na conversa recebeu a série de equipe; o cliente não muda escopo nem completa meses por conta própria. A leitura visual revelou um viewport de gráfico largo sem desenho visível: o `HorizontalScrollView` mede o filho com largura `UNSPECIFIED`, e a View customizada usava largura mínima zero. Foi definida a largura mínima do gráfico e explicitada a altura de 190 dp/260 dp. A regressão de medição/desenho para 12 zeros reproduziu a largura incorreta antes da correção.
- Eventos: a consulta real recebeu recusa explícita de validação. Sem listagem aprovada, não foi selecionada uma referência nem homologado o resumo real. Referências/ambiguidade/acesso negado foram verificados com respostas simuladas.
- PDF: o pedido real terminou com assistente indisponível, sem cartão de sucesso. Não houve download remoto nem homologação real de “Cadê o PDF?”/expiração de assinatura. Cartão, download para MediaStore, falhas, expiração e reuso do mesmo id/prazo foram verificados com simulação fiel.
- Um diagnóstico opt-in, somente GET, confirmou os blocos do histórico sem salvar nomes, referências de seleção, tokens ou URLs assinadas. Esse diagnóstico local não integra os testes de CI, pois depende de sessão e ambiente reais.

Verificações automatizadas: 184 testes unitários passaram; lint com minSdk oficial 31 passou com zero erros e 152 avisos. O lint com override local para Android 11 encontrou oito incompatibilidades preexistentes exclusivamente em `FormLocationRepository`; esse módulo não foi alterado. A build de tablet usa init script local, sem reduzir o minSdk versionado. Os testes instrumentados de contratos/cartão/download/telas passaram inicialmente em 29 casos; a regressão adicional de viewport largo é registrada após sua execução. PDFs simulados do teste são apagados ao final, sem apagar arquivos do usuário.

Capturas e relatório sanitizado estão no diretório local `.gradle/chat-qa` do checkout principal. Não foram publicados dados pessoais. Alterações locais anteriores, inclusive paginação com chevrons e correção de altura das células, foram preservadas. Sem merge, deploy, alteração de cadastros ou mensagens para outras pessoas.

Validação final do viewport: a regressão nova passou e os dez testes relacionados de cartão/gráficos foram repetidos com sucesso (11 na execução final). Ao todo, 30 casos únicos de interface passaram na rodada, além do diagnóstico real somente GET. A captura `tablet-pr34-lines-fixed.png` confirma no histórico real os eixos, a linha horizontal, os pontos em zero e a legenda. O gráfico agora permite rolagem horizontal para as 12 categorias, sem fabricar valores.

APK final instalado no tablet sem nova desinstalação: SHA-256 `8a042322737090f7d6185a6cb69754a80b3b54f9328a3e9e658cf099ec14f681`. Os 184 testes JVM passaram. O lint oficial foi aprovado antes do ajuste final de medição, que apenas define largura mínima/altura do viewport; não se afirma nova rodada de lint após esse ajuste. O cliente web não está neste repositório e requer sua própria validação desktop/estreita. Os arquivos permanecem locais, sem commit/push adicional nesta rodada.
