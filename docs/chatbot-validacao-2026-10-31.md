# Validação do chatbot — 31/10/2026

## Ambiente

- Samsung Galaxy M31 (SM-M315F), conectado por USB.
- Astro debug 1.0, versionCode 1, baseado em `9a2e7f9` com alterações locais.
- APK SHA-256: `18b235bc5630f4ca83f24830d088d72a231771aa9ab6b7133ec0fd4d1987a00f`.
- A atualização final foi instalada com `adb install -r`, preservando o login.
- API: `https://ia.astro-app.solutions`, `/version` retornou `4ffe30020a0f2c229b7ea5019bf7ea32684ed37f`.
- A PR de backend #31 estava mesclada. A comparação de revisões confirmou que a revisão publicada contém `e7a387f`.

## Testes reais pela interface

| Caso | Resultado |
|---|---|
| Meus dados | Ficha estruturada, rótulo “Gestor do workspace”, modalidade, sem Status/Cadastrado em. O rótulo Modalidade coube em uma linha após o ajuste de largura. |
| Foto própria | Iniciais exibidas. Uma consulta autenticada ao histórico confirmou `photoPresent=false`; a API ainda não forneceu URL. |
| Lista de 100 funcionários | Tabela visual, 100 retornados de 118 disponíveis e aviso de conjunto parcial. Controle Próxima trocou os registros localmente. |
| Barras por cargo | Gráfico e legendas legíveis, valores 8, 74, 52 e 10. |
| Setores por perfil | Gráfico com valores 121 e 23, percentuais legíveis. |
| Pendências por NR | Tabela e gráfico de barras; seis categorias, fonte, unidade e aviso de pessoas contadas por categoria preservados. |
| Vencimentos por faixa | Tabela, gráfico e texto de contexto nessa ordem. Histórico autenticado confirmou os três tipos nessa sequência. |
| Histórico | Sair e reabrir a conversa manteve os blocos; ficha reaberta também foi renderizada. |
| Detalhes | Toque prolongado abriu o gráfico real de barras; fechar retornou à conversa. |
| Teclado/rolagem | Com teclado aberto, envio manteve o teclado e posicionou o fim das novas respostas acima do campo. Fechar teclado ampliou a área de leitura. |
| Contexto: lista → gráfico | Falhou em duas tentativas: “assistente indisponível”. Não houve gráfico para renderizar. Uma tentativa pediu barras por cargo com base nos dados; outra usou “Gere um grafico com base nesses dados”. Não foram reenviadas automaticamente as mensagens pendentes. |

Os totais das consultas diretas por cargo/perfil somaram 144, enquanto a listagem informou total 118. Não foi possível confirmar os filtros efetivos das ferramentas a partir da interface; conferir escopo e filtros na API antes de tratar as consultas como conjuntos equivalentes. O mobile não ajustou esses totais.

## Testes automatizados e simulados

- `testDebugUnitTest`: 172 testes, zero falhas.
- `lintDebug`: zero erros, 152 avisos.
- `assembleDebug` e `assembleDebugAndroidTest`: aprovados.
- `ChatStructuredContentDeviceTest`: seis testes aprovados no M31. Dados fictícios cobrem ordem dos blocos, fallback textual, foto ausente/iniciais, campos ausentes, tabela com 200 linhas e paginação de 10 em 10, três gráficos em largura de 240 dp, legendas longas e abertura/fechamento dos detalhes.
- `ChatBlockContractProbeTest`: um teste aprovado, somente leitura do histórico autenticado. Relatório limitado a tipos/chaves de blocos e disponibilidade da foto, sem dados pessoais, tokens ou URLs.
- O teste de rótulos confirma que o código original permanece no JSON e que códigos desconhecidos são preservados.

## Limites e pendências

- Foto própria depende de a API fornecer uma URL temporária; conferir configuração privada do backend.
- Investigar a falha reproduzida do gráfico com base na listagem, usando os logs da API. A mensagem apresentada não permite afirmar o status HTTP ou a causa interna.
- Gráfico de linhas foi testado com dados simulados, não com série temporal real.
- Não houve nova homologação no tablet, execução manual do TalkBack, teste real de foto válida/HTTP 403 nem busca de terceiros por nome nesta rodada.
- Não foram alteradas permissões, contagens, percentuais ou os códigos armazenados nos modelos. Sem commits ou publicação remota nesta rodada.

## Diagnóstico do avatar no ECS

Após renovar o acesso AWS, o serviço `astro-ai-api` foi localizado no cluster `astro-fargate`, região `us-east-1`, usando task definition revisão 6 e imagem `sha-4ffe30020a0f2c229b7ea5019bf7ea32684ed37f`.

O container `api` recebe as configurações R2 comuns e Redis, mas não recebe `R2_BUCKET_AVATARS`. Essa variável é obrigatória no resolvedor de fotos e não tem fallback para `R2_BUCKET_NAME`, pois os buckets têm finalidades diferentes. Os logs filtrados das duas horas anteriores mostraram 15 ocorrências de `avatar_unavailable reason=configuration`. A task definition da API Java contém `R2_BUCKET_AVATARS`.

Foi preparado no CloudShell o arquivo privado `/tmp/astro-ai-avatar-taskdef.json`, copiando somente a referência dessa configuração da API Java para o container `api` da IA. A imagem e as demais configurações foram preservadas. Nenhum valor de segredo foi exibido. A revisão não foi registrada nem aplicada ao serviço; publicação pendente de autorização, pois o pedido desta rodada foi consultar o ambiente.

## Aplicação autorizada e confirmação da foto

Após autorização explícita do usuário, a alteração preparada foi comparada à configuração corrente: somente a referência `R2_BUCKET_AVATARS` foi adicionada ao container `api`. Registrada a task definition `astro-ai-api-fargate:7`, o serviço `astro-ai-api` foi atualizado mantendo a imagem `4ffe300`. A referência da revisão anterior ficou registrada privadamente no CloudShell para rollback.

O waiter do ECS confirmou o serviço estável, com uma task em execução de uma desejada, usando a revisão 7. Nenhuma configuração da API Java foi alterada.

O diagnóstico autenticado executado no M31 confirmou, para as duas fichas do histórico recente: `photoPresent=true`, `allowedOrigin=true`, `photoHttp=200` e `decodable=true`. Ao reabrir a conversa, a fotografia foi exibida na ficha real do aplicativo; conferência visual concluída. A pendência de foto própria descrita acima está resolvida para essa conta e ambiente. Não foi necessário recompilar ou reinstalar o mobile.

Evidência local do diagnóstico, sem URLs ou dados pessoais: `.gradle/chat-qa/photo-after-config-2026-10-31.txt` no checkout principal do mobile. A captura visual ficou somente no diretório local de QA, sem inclusão no Git, por conter dados da conta. Não foram expostos tokens, credenciais ou URLs assinadas.

Nos próximos deploys, preservar a configuração adicionada à task definition do ECS; esta aplicação não alterou templates de infraestrutura nem publicou commits.
