# Fluxo de formulários — Astro Mobile

## Sequência de UI

- SCRUM-279: lista de formulários do Gestor, com busca, filtros e dados mockados.
- SCRUM-277 (filha da SCRUM-272): detalhes do formulário de inspeção e permissão precisa solicitada pelo checkbox.
- SCRUM-273: realização/preenchimento do formulário.

As telas ainda sem API devem continuar usando dados locais identificados como mocks. O skeleton de 1 segundo é apenas uma espera visual provisória até a integração.

## Detalhes e consentimento — SCRUM-277

Na lista mock, o primeiro item abre a variante de endereço correto, o segundo a de endereço incorreto e o terceiro a de formulário sem endereço específico. O endereço é um cenário visual local; conceder permissão não prova que o aparelho está na unidade. Título e prazo são preservados a partir do item selecionado; remetente e unidade usam os exemplos do Figma.

Sem permissão precisa (inclusive com apenas a aproximada), o card informa que é necessário permitir a localização precisa, antes de apresentar qualquer um dos três cenários de endereço. O checkbox continua disponível para solicitar a permissão. Ao retornar das configurações, o aviso é atualizado. A ausência de endereço obrigatório não dispensa a permissão para registrar a localização durante o formulário.

O checkbox solicita `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION` juntos, como exige o Android para oferecer as duas opções de precisão. Apenas `ACCESS_FINE_LOCATION` concedida marca o checkbox e habilita Responder formulário, desde que a variante mock permita responder. Recusa ou permissão apenas aproximada exibem erro abaixo do checkbox; o link Abrir configurações permite corrigir a permissão quando o sistema deixar de mostrar o pedido. Desmarcar o checkbox bloqueia novamente o botão. A permissão é conferida ao retornar das configurações e antes de avançar.

O componente é um MaterialCheckBox quadrado, não um círculo desenhado. Após recusar, ele permanece clicável e o toque seguinte solicita a permissão novamente quando o Android permite. Se o sistema bloquear novos diálogos, tocar nele abre as configurações do aplicativo; ao voltar com localização precisa permitida, o aceite é atualizado. Nenhuma permissão é concedida automaticamente pelo aplicativo.

Esta etapa não lê coordenadas nem inicia rastreamento. Responder formulário apenas sinaliza a próxima tela, que será implementada na SCRUM-273. O raio informado no texto é de **100 m**, conforme decisão do usuário; a medição real ainda depende da integração GPS/API.

## Requisito para a tela de realização (SCRUM-273)

Durante o preenchimento, obter a localização atual ao iniciar, a cada minuto e novamente ao enviar. Comparar a posição com o local esperado que vier da API e interromper as leituras ao concluir ou cancelar o formulário.

O raio permitido é de 100 m. Confirmar com o contrato da API o formato do local e o tratamento da precisão da leitura e da localização indisponível. Também definir, antes da implementação, se a coleta deve continuar quando o app estiver em segundo plano. A validação inicial do endereço está prevista na SCRUM-342; a coleta periódica pertence à realização do formulário, não à lista.
