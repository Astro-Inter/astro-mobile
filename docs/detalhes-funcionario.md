# Detalhes do funcionário — SCRUM-311

UI e mocks do [card SCRUM-311](https://luizacursino.atlassian.net/browse/SCRUM-311), filho de SCRUM-257. Referência: App V2, frame [2590:12569](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano?node-id=2590-12569).

## Escopo e navegação

- Home do Gestor → Colaboradores → exemplo selecionado → Colaborador.
- Nome, cargo, retrato e contagem acompanham o exemplo da lista. Os outros campos são os exemplos do Figma: Osasco, Presencial e kevin.parker@picpay.com, não dados de uma conta real.
- Voltar retorna à lista com pesquisa/filtro preservados; X retorna à Home do Gestor.
- Navbar única na Activity, fixa durante o push e sem aba destacada nesta tela.
- Conteúdo rolável, largura máxima compartilhada com as telas existentes e colunas proporcionais, sem campos de largura fixa que sobreponham o e-mail.
- Skeleton local de 1 segundo seguindo o padrão atual, oculto no XML, cancelado ao sair; não há requisição nem bloqueio da thread principal.
- Três cartões NR-06, como no frame. Expandem individualmente, sem animação adicional e sem cobrir a próxima NR. O conteúdo expandido ainda está pendente; por ora informa essa ausência em vez de inventar regras/textos normativos.
- Conversar abre a conversa humana mock com o funcionário selecionado, sem chamar o chatbot; veja `docs/conversa-humana.md`. Ver pendências abre os eventos do funcionário preservando o mock selecionado; veja `docs/eventos-funcionario.md`. Ambas as ações foram mantidas ao integrar a main na SCRUM-301. API do detalhe na SCRUM-312.

## Recursos e diferenças de dados

Retrato inicial com SHA-256 idêntico ao export do Figma (`CCCB5B8D072AD8CE22A53730F08CBBEA48F064389A167E0E9BFDB892859F34FD`), reutilizado com os demais retratos da lista. Ícones de cargo, unidade, modalidade, aviso, voltar, fechar, seta, chat e dividers reutilizam os recursos canônicos. Unidade/modalidade/aviso/e-mail são roxos; cargo secundário e ações claras. Chevrons reutilizam o export oficial já presente (`manager_events_chevron_down`, pixels conferidos), no slot compacto de 18dp do componente compartilhado do Perfil.

O campo de e-mail usa o vetor `ic_email_closed`, igual ao ícone de envelope do Perfil, colorido com o roxo do Figma. O PNG anterior `staff_detail_email.png` deixa de ser referenciado pela tela. Não há URLs temporárias em runtime nem novas dependências. O componente de NR do Perfil foi reutilizado sem mudar o comportamento do Perfil integrado.

O ano `1002` foi preservado literalmente na validade do Figma e continua pendente de revisão editorial. O frame mostra 3 eventos; no mock em execução a contagem é a do item escolhido na lista (10/10/0), para não contradizer a seleção. A prévia usa 3 como no frame. Não há cálculo de pendências ou validade no aplicativo.

## Git e sequência

Branch `feat/SCRUM-311-detalhes-funcionario-ui` sobre `feat/SCRUM-313-lista-colaboradores-ui`, base exata `2cee85c`. A main remota consultada continua em `293e7b2`. Publicar e mergear SCRUM-313 antes da SCRUM-311. Antes de rebase/troca de base, consultar `docs/fila-de-prs.md` e conferir o estado remoto. Commit local autorizado pelo solicitante em 15/10/2026 antes de iniciar a SCRUM-309. Sem push ou PR.

## Verificação

`assembleDebug` e `lintDebug` aprovados na revisão final, sem erros; `git diff --check` sem problemas de whitespace. Os avisos locais remanescentes são de possível overdraw do fundo, como nas telas existentes. Sem dispositivo físico conectado; instalação, navegação em execução e comparação visual em telefone/tablet pendentes. Emulador não utilizado. Não foram criados testes nem alteradas configurações de sessão, SDK ou credenciais. Card SCRUM-311 e pai SCRUM-257 em andamento na Sprint 6.
