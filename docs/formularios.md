# Fluxo de formulários — Astro Mobile

## Sequência de UI

- SCRUM-279: lista de formulários do Gestor, com busca, filtros e dados mockados.
- SCRUM-277 (filha da SCRUM-272): detalhes do formulário de inspeção e permissão precisa solicitada pelo checkbox.
- SCRUM-273: realização/preenchimento do formulário; UI na SCRUM-274, API na SCRUM-275 e câmera/evidências/GPS na SCRUM-276. A coleta GPS foi antecipada na SCRUM-274 por solicitação explícita.

As telas ainda sem API devem continuar usando dados locais identificados como mocks. O skeleton de 1 segundo é apenas uma espera visual provisória até a integração.

## Detalhes e consentimento — SCRUM-277

Na lista mock, o primeiro item abre a variante de endereço correto, o segundo a de endereço incorreto e o terceiro a de formulário sem endereço específico. O endereço é um cenário visual local; conceder permissão não prova que o aparelho está na unidade. Título e prazo são preservados a partir do item selecionado; remetente e unidade usam os exemplos do Figma.

Sem permissão precisa (inclusive com apenas a aproximada), o card informa que é necessário permitir a localização precisa, antes de apresentar qualquer um dos três cenários de endereço. O checkbox continua disponível para solicitar a permissão. Ao retornar das configurações, o aviso é atualizado. A ausência de endereço obrigatório não dispensa a permissão para registrar a localização durante o formulário.

O checkbox solicita `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION` juntos, como exige o Android para oferecer as duas opções de precisão. Apenas `ACCESS_FINE_LOCATION` concedida marca o checkbox e habilita Responder formulário, desde que a variante mock permita responder. Recusa ou permissão apenas aproximada exibem erro abaixo do checkbox; o link Abrir configurações permite corrigir a permissão quando o sistema deixar de mostrar o pedido. Desmarcar o checkbox bloqueia novamente o botão. A permissão é conferida ao retornar das configurações e antes de avançar.

O componente é um MaterialCheckBox quadrado, não um círculo desenhado. Após recusar, ele permanece clicável e o toque seguinte solicita a permissão novamente quando o Android permite. Se o sistema bloquear novos diálogos, tocar nele abre as configurações do aplicativo; ao voltar com localização precisa permitida, o aceite é atualizado. Nenhuma permissão é concedida automaticamente pelo aplicativo.

A tela de detalhes não inicia rastreamento. Responder formulário agora abre a resposta (SCRUM-274), preservando título e exigência de endereço específico. A comparação com a unidade real ainda depende da localização fornecida pela API; não confundir os avisos mock do detalhe com uma geolocalização validada.

## Resposta do formulário — SCRUM-274

A UI do frame 2590:12417 usa três perguntas de escolha única, texto de riscos, data e evidências de exemplo. Todos os campos são obrigatórios, inclusive ao menos uma evidência, com limite de três. As opções de cada pergunta são exclusivas entre si; as perguntas não interferem umas nas outras. A numeração foi regularizada para 1–6, pois o frame trazia 6 antes de 5. A seleção de data usa dd/MM/aaaa; não foi inventada uma restrição de data futura.

Anexar evidências adiciona referências locais explicitamente identificadas como exemplos; não abre câmera ou galeria nem envia arquivos. A câmera real permanece na SCRUM-276. Concluir faz uma última leitura GPS e finaliza somente o protótipo local, com aviso explícito, sem simular sucesso da API ou inventar outra tela de confirmação. Ao concluir ou cancelar pelo raio, as respostas ficam bloqueadas e as leituras param. Voltar/X saem da tela e interrompem a coleta.

Skeleton mock de 1 segundo, oculto no preview XML. Layout rolável com largura máxima compartilhada (telefone/tablet), sem navbar nessa tela, como no Figma. Estado das respostas e registros mantido somente na memória do ViewModel; não há persistência após encerrar o processo.

## GPS antecipado da SCRUM-276

Decisões confirmadas pelo usuário: primeira posição capturada como referência provisória, comparação no raio de **100 m**, pausa em segundo plano e leitura imediata ao voltar. A referência não é o endereço da unidade; a interface explicita que se trata de teste.

O Fragment programa a leitura ao entrar e a cada 60 segundos em primeiro plano. O ViewModel orquestra a leitura e mantém cópias dos registros de `Location` (coordenadas, horário e precisão reportados pelo provedor). `FormLocationRepository` usa LocationManager do Android, com alta acurácia solicitada, permissão precisa obrigatória, cancelamento e limite nativo de 15 segundos para cada tentativa; não há dependência nova, foreground service ou permissão de segundo plano.

Para formulários com endereço obrigatório, cada posição é comparada à referência provisória com `Location.distanceTo`; ultrapassar 100 m cancela a tentativa local. Sem endereço específico, a localização continua registrada, mas esse limite não cancela o formulário. Permissão ausente/aproximada, localização desativada e ausência de leitura bloqueiam a conclusão e mostram a ação adequada de configurações/nova tentativa. A permissão precisa não garante, por si só, uma margem de erro do sensor; a precisão fica guardada no registro e sua política definitiva depende do contrato real.

Nenhuma coordenada é mostrada na interface, enviada à API ou escrita em logs. O painel de teste informa apenas quantidade de registros e distância. A integração futura deve substituir a referência inicial pela localização do formulário recebida da API; o formato desse contrato continua pendente. A checagem inicial da unidade no detalhe pertence à SCRUM-342.

Validar manualmente: campos obrigatórios, exclusividade das escolhas, data, inclusão/remoção dos três exemplos; leitura inicial, incremento após um minuto, pausa/retomada, leitura final, GPS desligado, permissão revogada/aproximada e cancelamento fora de 100 m. Não alterar permissões do usuário automaticamente para esses testes. Sem emulador.
