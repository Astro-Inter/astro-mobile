# Validação de conclusão de evento — SCRUM-289

UI e mocks da subtarefa de [SCRUM-266](https://luizacursino.atlassian.net/browse/SCRUM-266), sem API ou alteração real de conclusões.

## Branch e dependência

- Branch: `feat/SCRUM-289-validacao-evento-ui`.
- Base: `feat/SCRUM-274-responder-formulario-ui`, no commit `dd4f52204508144828495048cfe6ae3d340db1ce`, conforme pedido de empilhamento.
- Ordem de integração: primeiro PR #43; depois realinhar esta branch à main, retirando apenas os commits herdados, antes do seu merge.
- Trabalho anterior recuperado seletivamente de `d631bc459e4a0a8fc792116ee24853835d0b3abb`; esse stash e o snapshot combinado `1aba05a51ef51483d1190bffb54c39e968625ecf` permanecem intactos. A SCRUM-287 não foi incorporada.

## Referências visuais

- [Tela 2590:12529](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12529).
- [Rejeição 2590:12555](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12555).
- [Confirmação 3408:24580](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=3408-24580).

## Implementação

O participante pendente de validação no modo Autodeclaração abre a tela com seu nome, retrato e evento. Cargo, unidade, datas e nome da evidência são mocks do Figma. O modo Atribuição preserva seu destino existente. Cabeçalho com voltar/X, navbar única fixa na Activity e transições push preservados.

Skeleton de 1 segundo, já aprovado no card, oculto na prévia XML. Layout rolável e colunas proporcionais, sem padding duplicado para uma navbar que já fica fora do NavHost. Fragment controla somente apresentação/navegação; não há ViewModel ou Repository vazio para dados estáticos de argumentos.

Rejeitar abre o pop-up do Figma com justificativa editável. O botão só fica ativo com texto não vazio; confirmar apenas retorna à Home, sem persistir ou enviar dados. X e voltar fecham o pop-up. Largura máxima de 370dp, margem de 30dp, fundo escurecido e X com área de toque de 48dp.

Validar abre confirmação mock por 2 segundos, sem navbar, e recria a Home do Gestor com seu loading existente. O layout/check da confirmação do Colaborador é reutilizado sem alterar seu texto ou destino. O timer é cancelado ao sair.

Reutilizados retratos, calendários, relógio, link, backgrounds, estilos e navbar existentes. Dois ícones oficiais preservados do trabalho anterior foram recuperados sem duplicatas binárias. O título `Evidências envidadas` é literal do Figma atual; não foi corrigido por iniciativa própria.

O comprovante abre a imagem de exemplo da SCRUM-287 quando essa branch filha estiver integrada, sem ler um PDF real. A SCRUM-289 isolada mantém o aviso de pendência. Integração e envio da rejeição/validação permanecem na SCRUM-290.

## Plano de testes manuais — executar pelo solicitante

1. Entrar no fluxo Gestor → Eventos → primeiro evento (Autodeclaração) → Ver participantes → primeiro Kevin Parker (validação pendente). Conferir skeleton de 1s, título do evento, retrato, nome e dados de exemplo.
2. Rolar até os botões. Conferir ícones roxos, texto legível e navbar fixa; testar seta de voltar e manutenção da busca/filtro na lista. Reabrir e testar a linha do evento (retorna ao detalhe) e o X (Home).
3. Tocar em Rejeitar. Conferir fundo escuro, posição do X e teclado. Botão desabilitado com campo vazio ou somente espaços, habilitado com justificativa. X/voltar fecham sem sair da validação; confirmar com texto volta à Home sem alterar dados reais.
4. Reabrir e tocar em Validar. Conferir check e texto `Você validou uma conclusão!`, navbar oculta, espera de 2s e retorno à Home do Gestor com skeleton. Não deve voltar à Home do Colaborador.
5. Com a SCRUM-287 integrada, tocar no comprovante deve abrir a imagem de exemplo e sua paginação; voltar preserva a validação. Sem ela, a SCRUM-289 isolada apenas informa a pendência. Nenhum PDF ou upload real faz parte deste mock.
6. Conferir que participantes já validados/ainda sem conclusão não abrem indevidamente a validação. Abrir o segundo evento (Atribuição) e conferir que o participante não concluído continua abrindo Atribuição, não Validação.
7. No tablet em orientação vertical, conferir rolagem, colunas, pop-up limitado a 370dp e ausência de sobreposição com a navbar/teclado. Este roteiro não autoriza iniciar emulador automaticamente.
8. Como o layout de confirmação é compartilhado, conferir manualmente que a conclusão de evento do Colaborador mantém seu texto e retorno à própria Home após 2s.

## Verificação desta entrega

Compilação debug aprovada e diff/recursos revisados estaticamente. Por pedido explícito, não foram criados ou executados testes, lint, navegação, inspeção de tela ou cenários em execução. A instalação é separada dessa validação; registrar seu resultado no SCREEN_STATUS.md. API e validação manual permanecem pendentes.
