# Fluxo de formulários — Astro Mobile

## Sequência de UI

- SCRUM-279: lista de formulários do Gestor, com busca, filtros e dados mockados.
- SCRUM-277 (filha da SCRUM-272): detalhes do formulário de inspeção e permissão precisa solicitada pelo checkbox.
- SCRUM-273: realização/preenchimento do formulário; UI na SCRUM-274, API na SCRUM-275 e câmera/evidências/GPS na SCRUM-276. A coleta GPS foi antecipada na SCRUM-274 por solicitação explícita.

As telas ainda sem API devem continuar usando dados locais identificados como mocks. O skeleton de 1 segundo é apenas uma espera visual provisória até a integração.

## Detalhes e consentimento — SCRUM-277

Os itens usam título, prazo, remetente e unidade de exemplo. O terceiro item não exige endereço específico. A antiga flag mock de endereço correto/incorreto foi removida: agora há uma leitura real ao abrir os detalhes e outra ao tocar em Responder formulário. Sem leitura válida, permissão precisa, consentimento e posição dentro do raio, o avanço fica bloqueado.

Sem permissão precisa (inclusive com apenas a aproximada), o card informa que é necessário permitir a localização precisa, antes de apresentar qualquer um dos três cenários de endereço. O checkbox continua disponível para solicitar a permissão. Ao retornar das configurações, o aviso é atualizado. A ausência de endereço obrigatório não dispensa a permissão para registrar a localização durante o formulário.

O checkbox solicita `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION` juntos, como exige o Android para oferecer as duas opções de precisão. Apenas `ACCESS_FINE_LOCATION` concedida marca o checkbox; Responder formulário depende também da leitura e do raio. Recusa ou permissão apenas aproximada exibem erro abaixo do checkbox; o link Abrir configurações permite corrigir a permissão quando o sistema deixar de mostrar o pedido. Desmarcar o checkbox bloqueia novamente o botão. A permissão e a localização são conferidas ao retornar das configurações e antes de avançar.

O componente é um MaterialCheckBox quadrado, não um círculo desenhado. Após recusar, ele permanece clicável e o toque seguinte solicita a permissão novamente quando o Android permite. Se o sistema bloquear novos diálogos, tocar nele abre as configurações do aplicativo; ao voltar com localização precisa permitida, o aceite é atualizado. Nenhuma permissão é concedida automaticamente pelo aplicativo.

A tela de detalhes faz leituras pontuais, sem rastreamento periódico. Responder formulário abre a resposta (SCRUM-274), preservando título e exigência de endereço específico. **Até existir API, o usuário autorizou fixar a primeira posição obtida nos detalhes como referência provisória. Isso não comprova presença na unidade Osasco.** Essa referência é mantida durante a tentativa, inclusive após sair do raio; não é recalculada ao voltar aos detalhes. Substituí-la pelas coordenadas da unidade na integração futura (SCRUM-342/275), sem inventar contrato agora.

## Resposta do formulário — SCRUM-274

A UI do frame 2590:12417 usa três perguntas de escolha única, texto de riscos, data e fotos de evidência. Todos os campos são obrigatórios, inclusive ao menos uma evidência, com limite de três. As opções de cada pergunta são exclusivas entre si; as perguntas não interferem umas nas outras. A numeração foi regularizada para 1–6, pois o frame trazia 6 antes de 5. A seleção de data usa dd/MM/aaaa; não foi inventada uma restrição de data futura.

Anexar evidências abre a câmera instalada com `ActivityResultContracts.TakePicture` e `content Uri` de um FileProvider não exportado, restrito a `cache/form_evidences/`. Não solicita permissão de câmera ou de armazenamento, pois usa o aplicativo externo. Após confirmar a foto, exibe miniatura e remoção; cancelar a câmera não adiciona evidência e apaga o arquivo vazio. Não há galeria, upload ou Firebase Storage nesta etapa. As fotos são temporárias e são removidas ao sair da tentativa (fechar os detalhes/X); voltar aos detalhes preserva a tentativa e seus arquivos.

Concluir faz uma última leitura GPS e finaliza somente o protótipo local, com aviso explícito, sem simular sucesso da API ou inventar outra tela de confirmação. Ao concluir, as respostas ficam bloqueadas e as leituras param. Voltar/X e abrir a câmera interrompem a coleta; retornar à resposta faz uma nova leitura.

Skeleton mock de 1 segundo, oculto no preview XML. Layout rolável com largura máxima compartilhada (telefone/tablet), sem navbar nessa tela, como no Figma. Estado das respostas, referência e registros no ViewModel vinculado à entrada dos detalhes na pilha; não há restauração após encerrar o processo. Arquivos de foto ficam somente no cache privado, sem envio.

## GPS antecipado da SCRUM-276

Decisões confirmadas pelo usuário: primeira posição capturada nos detalhes como referência provisória, comparação no raio de **100 m**, pausa em segundo plano e leitura imediata ao voltar. A referência não é o endereço da unidade. Os registros, coordenadas e distâncias não aparecem na interface.

O Fragment programa a leitura ao entrar e a cada 60 segundos em primeiro plano. O ViewModel orquestra a leitura e mantém cópias dos registros de `Location` (coordenadas, horário e precisão reportados pelo provedor). `FormLocationRepository` usa LocationManager do Android, com alta acurácia solicitada, permissão precisa obrigatória, cancelamento e limite nativo de 15 segundos para cada tentativa; não há dependência nova, foreground service ou permissão de segundo plano.

Para formulários com endereço obrigatório, cada posição é comparada à referência provisória com `Location.distanceTo`. Ultrapassar 100 m impede concluir e abre o frame **4604:4506** por **3 segundos**, sem navbar. Depois retorna aos detalhes, preservando respostas e fotos. Os detalhes permitem verificar novamente; somente ao retornar ao raio e obter uma leitura válida é possível continuar. Esse comportamento foi escolhido com autorização do usuário, em lugar de descartar a tentativa. Sem endereço específico, a localização continua registrada, mas o raio não bloqueia o formulário. Permissão ausente/aproximada, localização desativada e ausência de leitura bloqueiam avanço/conclusão e mostram configurações/nova tentativa. A permissão precisa não garante uma margem de erro do sensor; a precisão fica guardada no registro e sua política definitiva depende do contrato real.

Nenhuma coordenada é mostrada na interface, enviada à API ou escrita em logs. O painel de contagem/distância foi removido. A integração futura deve substituir a referência inicial pela localização do formulário recebida da API; o formato desse contrato continua pendente.

Validar manualmente: campos obrigatórios, exclusividade das escolhas, data, câmera real/retorno/cancelamento, limite de três fotos e remoção; leitura nos detalhes e antes do avanço, leituras a cada minuto/ao concluir, pausa/retomada, GPS desligado, permissão revogada/aproximada, aviso de 3s fora dos 100 m e retomada com respostas/fotos preservadas ao retornar. Não alterar permissões do usuário automaticamente para esses testes. Sem emulador.
