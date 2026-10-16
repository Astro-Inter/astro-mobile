# Visualização de evidência de evento — SCRUM-287

UI e mocks de [SCRUM-267](https://luizacursino.atlassian.net/browse/SCRUM-267), conforme o [frame 2590:12551 do App V2](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12551).

## Escopo

- Cabeçalho Evidência, seta oficial com ripple circular e imagem local oficial do Figma, sem editar o bitmap.
- O recorte do frame é reproduzido na apresentação: largura da imagem de 146,95%, deslocamento horizontal de -24,52% e exclusão do trecho coberto pelo status/header do desenho. Layout rolável e largura máxima compartilhada para celular/tablet.
- Três seletores de 65dp, raio de 18dp e container de 26dp. Inter Regular 12sp, com licença SIL preservada em `app/src/main/assets/licenses/Inter-OFL.txt`.
- Decisão já aprovada pelo solicitante: os três números mostram a mesma imagem no mock; apenas a seleção roxa muda, com ripple nativo.
- Skeleton mock de 1 segundo, já aprovado, oculto na prévia XML. A espera é cancelada ao sair e não bloqueia a seta.
- Abertura pelo comprovante da validação SCRUM-289; voltar mantém a validação e o participante/evento na pilha. Navbar única fixa na Activity e aba Eventos selecionada (a aba Chat destacada no frame não corresponde a este fluxo, como já registrado no mock anterior).

O Fragment controla apenas recorte, seleção e loading visual. Não foram criados ViewModel/Repository vazios nem novas dependências Gradle. A fonte é um recurso visual necessário ao componente, não uma biblioteca nova.

## Limites

Sem API, Firebase, upload/download, PDF real, zoom ou novos atalhos. O nome PDF na validação é o mock original; esta tela mostra uma imagem de exemplo, não interpreta aquele arquivo. Integração e comportamento de arquivos reais permanecem na SCRUM-288.

## Branch empilhada

- Branch `feat/SCRUM-287-evidencia-evento-ui`.
- Base `feat/SCRUM-289-validacao-evento-ui`, commit `6c5ff85` (PR #44).
- Ordem de integração: PR #43 (formulário) → PR #44 (validação) → esta tarefa. Depois do merge da base, realinhar esta branch à main retirando os commits herdados, sem duplicar alterações.
- Recuperação seletiva do snapshot `1aba05a51ef51483d1190bffb54c39e968625ecf`. Os stashes originais e as branches antigas permanecem intactos.

## Roteiro manual — executar quando houver aparelho

1. Gestor → Eventos → primeiro evento (Autodeclaração) → Ver participantes → primeiro Kevin Parker com validação pendente → tocar no nome do comprovante.
2. Conferir título Evidência, skeleton de 1s e depois a imagem de exemplo com o recorte do Figma. A seta deve funcionar inclusive durante o loading.
3. Tocar em 1, 2 e 3: somente um deve ficar roxo, os demais claros com números pretos. A imagem continua igual, conforme o mock aprovado.
4. Voltar pela seta e pelo Android; conferir mesmo participante/evento na validação e funcionamento de Validar/Rejeitar. Nenhum upload/download ou alteração real deve acontecer.
5. Conferir navbar fixa e Eventos selecionado. Abrir Home/Chat/Perfil pela navbar não deve mover a barra com o push.
6. No tablet em orientação vertical, conferir que imagem, paginação e cabeçalho permanecem acessíveis por rolagem, sem cortar botões ou cobrir conteúdo com a navbar. A prévia XML é aproximada; o recorte exato é aplicado pelo Fragment.

## Verificação desta entrega

Compilação debug aprovada e revisão estática do diff, referências e hashes sem duplicatas binárias. Imagem oficial preservada em 379×559, sem ampliação do arquivo; fonte Inter recuperada com sua licença. Por pedido, sem testes, lint, instalação, emulador ou validação visual em aparelho. Conferência manual e integração permanecem pendentes.
