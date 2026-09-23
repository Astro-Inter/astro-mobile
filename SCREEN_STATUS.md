# Astro Mobile — Status de implementação das telas

Este arquivo registra somente telas cuja implementação já começou. A ausência de uma tela não significa que ela não exista no Figma ou no escopo do aplicativo.

Estados: `NÃO INICIADO`, `EM ANDAMENTO`, `CONCLUÍDO`, `BLOQUEADO` e `NÃO SE APLICA`.

| Tela | Fluxo | XML | Lógica/mock | Navegação | Integração | Resumo atual | Atualizado em |
|---|---|---|---|---|---|---|---|
| Splash | Inicialização | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Emblema com área segura no splash nativo e na tela animada; movimento com overshoot e escala bouncy validado em telefone e tablet. | 2026-09-20 |
| Destino mock pós-splash | Temporário | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO SE APLICA | Tela temporária implementada e alcançada após a animação, sem manter a splash no back stack. | 2026-09-20 |
| Identificação de e-mail | Autenticação | CONCLUÍDO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Mock: valor 1 abre a informação da chave de acesso; valor 2 abre o login com senha; outros valores exibem erro no campo. | 2026-09-23 |
| Tela informativa da chave de acesso | Autenticação | EM ANDAMENTO | NÃO INICIADO | EM ANDAMENTO | NÃO INICIADO | Layout XML iniciado a partir do frame 2590:12283, com entrada por “Saiba mais”, retorno por “Voltar”/“Entendi” e transição push padronizada; aguardando validação visual. | 2026-09-22 |
| Login com senha | Autenticação | EM ANDAMENTO | CONCLUÍDO | CONCLUÍDO | NÃO INICIADO | Estados padrão e incorreto dos frames 2590:12478 e 2590:12486, alternância de senha com ícones aberto/fechado, erro mock e retorno implementados; Firebase pertence à SCRUM-329. | 2026-09-23 |
