# Contratos analíticos do chatbot

Referência: [PR #33 da API de IA](https://github.com/Astro-Inter/astro-ai-api/pull/33), revisão `f4e91352903d1465e767c384cbb192d3b0335ecf`, `docs/contrato-dados-analiticos.md` e `app/modules/chat/schemas.py`.

## Implementação

Os modelos `ChatIndicatorsBlock`, `ChatContextBlock`, `ChatDatasetSummary` e `ChatMatrixBlock` ficam em `data/ai`, com campos mapeados por Gson. `ChatAnalyticalContract` valida os novos contratos. A apresentação continua no módulo `chat`, usando a paleta, a fonte e os componentes existentes. Não foram criados endpoints, repositories paralelos ou dependências externas.

- `indicadores` v1: cartões verticais com rótulo, valor original, unidade, base numerador/denominador, critério e universo. `null` com denominador zero aparece como “Sem base”. Não se calculam indicadores pela tabela ou pela página.
- `matriz_conformidade` v1: tabela longa com até 200 pares; identidade de linha por `referencia_pessoa` + `nr`, incluindo homônimos. Não são criadas células para pares ausentes. Os códigos permanecem nos modelos; os rótulos conhecidos são traduzidos só na apresentação.
- `contexto_dados` v1: fonte, referência, período opcional, escopo, filtros permitidos, critérios, avisos e resumo solicitado/recebido/total. Não depende de um campo `titulo`, ausente nesse contrato.
- `grafico_rosca` v1: usa as categorias/contagens originais, abertura central transparente, legenda textual, unidade e fonte. Categorias zero permanecem na legenda. Soma zero mostra “Sem valores para distribuir”, sem fatias artificiais. Segurar abre os detalhes, como nos demais gráficos. Não são acrescentados percentuais calculados às contagens.
- `tabela` v2: reutiliza o paginador de 10 linhas; distingue pedido atendido de corte por quantidade/tamanho. `pagina` e `total_paginas` do envelope não controlam as páginas locais.

Texto pode aparecer em qualquer posição. POST sem `resposta` e histórico usam a mesma renderização. Um bloco inválido/desconhecido não elimina os componentes válidos; a interface mostra um aviso na posição dele e nunca imprime JSON. Se não houver nenhum bloco suportado, usa o texto alternativo ou o aviso genérico.

O paginador modifica somente as linhas da tabela/matriz. Não chama repository, chatbot ou outra página de backend e não altera o gráfico ou os cartões globais. Totais usam representação inteira sem truncamento para caber em `int`. O contexto diferencia total zero de coleta cortada que retornou zero com total positivo.

Fotos e fichas legadas conservam o fallback de iniciais. As URLs R2 temporárias não são registradas nem persistidas no cache de imagens. Os tipos e campos antigos permanecem compatíveis.

## Validação

A fixture compartilhada `app/src/test/resources/chat/analytical-mixed.json` utiliza os discriminadores e exemplos documentados. Só o APK de testes a recebe como asset; não há fixture nem chave de diagnóstico no aplicativo de produção.

Testes de contrato cobrem versões, envelope sem texto legado, igualdade dos blocos POST/histórico, percentuais sem base, homônimos/duplicatas, vazio/corte por tamanho e rosca zerada. Testes de interface medem larguras de 240 e 600 dp, navegam por 100/200 linhas, comparam cartões/gráfico antes e depois da paginação e verificam ordem e leitura acessível. Um teste do fluxo `Fragment → ViewModel → Repository` usa resposta interceptada, mantém o teclado aberto e confirma uma única chamada ao paginar.

Estado de publicação observado no início da implementação: PR #33 mesclada; `/version` de produção retornava `4ffe30020a0f2c229b7ea5019bf7ea32684ed37f`, anterior aos contratos novos. Não se deve homologar os novos tipos como reais nesse ambiente. A disponibilização coordenada de mobile, web e backend continua necessária; este trabalho não publica backend nem faz merge.

## Resultado da homologação de 31/10/2026

- `testDebugUnitTest`: **179 testes**, sem falhas.
- `lintDebug`: **zero erros**, 152 avisos. Builds debug e androidTest aprovadas.
- Samsung M31 (SM-M315F), Astro debug 1.0, versionCode 1, atualizado com `adb install -r`, sem desinstalação ou perda de dados. APK SHA-256 `d8e47b7e4281d1a75d1ea62941828bae6b6f09168780ff9c88b7f4ecdca8921f`.
- **18 testes únicos de interface aprovados**: cinco em `ChatAnalyticalContentDeviceTest`, seis em `ChatStructuredContentDeviceTest` e sete em `ChatScreenTest`. A primeira execução do fluxo encontrou uma incompatibilidade do auxiliar de teste, que forçava a Home do colaborador num aparelho em fluxo gestor; o auxiliar foi corrigido para usar `HomeNavigation.destination`, sem mudar as preferências da conta, e os sete testes foram repetidos com sucesso.
- Capturas simuladas de cartões, rosca e teclado foram conferidas no aparelho. Larguras de 240/600 dp foram verificadas com layout simulado; não representam homologação num tablet físico.

Durante os testes, `/version` mudou para **`3ad06be7bf696bac04d954439e63fd8be5d66c79`**. A comparação no GitHub confirmou que essa revisão contém `f4e9135` (um commit à frente, nenhum atrás). A mudança de implantação não foi executada neste trabalho.

Na API real atualizada:

- “Mostre um grafico de rosca da equipe por perfil” retornou um gráfico legível, com contagens preservadas. Um diagnóstico autenticado somente de leitura confirmou no histórico a sequência texto → tabela → `grafico_rosca` v1 → texto, aceita pelo parser do mobile.
- “Meus dados” continuou apresentando a ficha e rótulos legíveis, sem erro. O histórico forneceu a foto; o download respondeu HTTP 200 e o conteúdo foi decodificado.
- “Mostre 100 usuarios da minha equipe em uma tabela” retornou 100 de 141 disponíveis, com aviso de conjunto parcial. A troca de página sem rede foi verificada pelo teste do fluxo com resposta interceptada; não se usam essas contagens como constantes de teste.
- “Mostre 100 registros da matriz de conformidade da equipe” e “Mostre indicadores de cobertura da equipe” terminaram com “assistente indisponível”, sem componentes analíticos para homologar visualmente. O cliente não teve seu contrato enfraquecido para acomodar essa falha. Investigar logs/erros da API e repetir depois.
- Comparação entre unidades, evolução mensal e resumo de um evento autorizado ainda não foram homologados ponta a ponta nesta rodada; não foi selecionado nem inventado um evento para fabricar dados. Os componentes genéricos usados por essas saídas foram validados com simulação.

O diagnóstico de histórico não registra nomes, e-mails, identificadores de seleção, tokens ou URLs assinadas e não integra a suíte versionada, pois exige sessão real. As evidências ficam no diretório local `.gradle/chat-qa` do checkout principal. Nenhum dado de teste foi enviado a conversas humanas, e não houve alterações em cadastros, permissões, treinamentos ou conformidades.
