# Astro Mobile — Status de implementação das telas

Este arquivo registra somente telas cuja implementação já começou. A ausência de uma tela não significa que ela não exista no Figma ou no escopo do aplicativo.

Estados: `NÃO INICIADO`, `EM ANDAMENTO`, `CONCLUÍDO`, `BLOQUEADO` e `NÃO SE APLICA`.

| Tela | Fluxo | XML | Lógica/mock | Navegação | Integração | Resumo atual | Atualizado em |
|---|---|---|---|---|---|---|---|
| Splash | Inicialização | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Emblema com área segura no splash nativo e na tela animada; movimento com overshoot e escala bouncy validado em telefone e tablet. | 2026-09-20 |
| Destino mock pós-splash | Temporário | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Tela temporária implementada e alcançada após a animação, sem manter a splash no back stack. | 2026-09-20 |
| Identificação de e-mail | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Layout XML do App V2; “Prosseguir” abre a chave do primeiro acesso e usa o mesmo efeito de toque; build debug aprovado. | 2026-09-22 |
| Tela informativa da chave de acesso | Autenticação | EM ANDAMENTO | NÃO INICIADO | EM ANDAMENTO | NÃO INICIADO | Layout do frame 2590:12283, entrada por “Saiba mais”, retorno por “Voltar”/“Entendi” e seta vetorial transparente com ripple circular; aguardando validação visual. | 2026-09-22 |
| Chave do primeiro acesso | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Estados padrão e inválido dos frames 2590:12512 e 2590:12497, entrada de seis dígitos, retorno inválido local e seta vetorial transparente com ripple circular; build aprovado, aguardando validação visual. | 2026-09-22 |
