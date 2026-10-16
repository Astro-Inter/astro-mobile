# Fluxo de formulários — Astro Mobile

## Sequência de UI

- SCRUM-279: lista de formulários do Gestor, com busca, filtros e dados mockados.
- SCRUM-272: detalhes do formulário de inspeção.
- SCRUM-273: realização/preenchimento do formulário.

As telas ainda sem API devem continuar usando dados locais identificados como mocks. O skeleton de 1 segundo é apenas uma espera visual provisória até a integração.

## Requisito para a tela de realização (SCRUM-273)

Durante o preenchimento, obter a localização atual ao iniciar, a cada minuto e novamente ao enviar. Comparar a posição com o local esperado que vier da API e interromper as leituras ao concluir ou cancelar o formulário.

Não inventar o formato do local, a precisão/raio permitido nem o comportamento quando a localização estiver indisponível: confirmar esses detalhes com o contrato da API antes de implementar a validação. Também definir, antes da implementação, se a coleta deve continuar quando o app estiver em segundo plano. Essa coleta e validação pertencem à tela de realização, não à lista nem aos detalhes.
