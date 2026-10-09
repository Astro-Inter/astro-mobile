# Perfil — SCRUM-255

Implementação Java/XML compartilhada por Colaborador e Gestor, em
`shared/profile`. API/Firebase ficam nos repositories e o estado do perfil
no `ProfileViewModel`, limitado à entrada de navegação do Perfil.

## Dados e erros

- `GET /user/me`, sem corpo, com `Authorization: Bearer <ID token Firebase>`.
- Reutiliza `ApiResponse<UserProfileData>`, Retrofit e o limite HTTP existente de 15s.
- HTTP 200 com `success=true` e `data` preenchido apresenta o perfil e as NRs.
- HTTP 401 renova o token uma vez; outro 401 encerra a sessão e abre a identificação.
- 404, demais falhas HTTP, rede, timeout ou resposta inválida abrem o erro genérico existente.
- Tentar novamente retorna ao Perfil e repete a consulta. Voltar retorna à Home.
- NRs não possuem regra local de vencimento. Datas ISO são exibidas em dd/MM/yyyy.
- A lista curta de NRs é uma coluna na rolagem do Perfil. Não há RecyclerView
  dentro do ScrollView: expandir um cartão reposiciona os próximos, sem escondê-los.
- Skeleton acompanha a requisição real; não há espera artificial. Os exemplos e
  a ocultação do skeleton no editor usam somente atributos `tools:`.

## Ações

- Sair exige o mesmo diálogo nos dois papéis; Cancelar/X não alteram a sessão.
- Popup com espaço de 12dp entre X e painel; fundo escurecido em 80%.
- Confirmar encerra Firebase, limpa o contexto local de autenticação e remove a pilha.
- “Trocar tipo de usuário”, com o ícone Transaction do Figma, aparece somente
  para GESTOR/GESTOR_WORKSPACE, inclusive na Home
  de Colaborador. Abre a escolha existente; Gestor continua mock. Escolher
  Colaborador guarda o fluxo e substitui a pilha antiga.
- Avatar padrão não é editável; integração pendente da decisão do time.
- Notificações são apenas uma preferência local por UID, inicialmente desativada.
  Não há FCM, envio real ou pedido de permissão. Integração futura: SCRUM-434.
- Toggle ativo roxo com círculo claro à direita; desativado cinza com círculo à esquerda.
  O círculo mede 24dp, com 6dp de espaço interno em cada extremidade e na vertical.
  O fundo declara o mesmo padding vertical da bolinha para o SwitchCompat não
  reduzir sua altura; a linha também tem 8dp de padding acima e abaixo.
  A expansão das NRs continua imediata, sem animação.

## Navbar fixa

A MainActivity contém uma única navbar fora do NavHostFragment. Home, Conversas,
Assistente e Perfil ocupam somente a área de conteúdo; o push de ida e volta não
move a barra, inclusive na lista de sessões da IA. A seleção acompanha o destino atual, e as abas reutilizam os destinos
presentes na pilha sem duplicá-los. Eventos permanece mock.

O tratamento de insets fica na Activity: no Assistente, a navbar se oculta enquanto
o teclado está aberto e retorna ao fechar, mantendo o campo de mensagem acessível.
Na lista de sessões, a Activity também controla o espaço do teclado e a navbar;
os argumentos da conta são preservados entre Conversas, Sessões e Assistente.
Autenticação, escolha de fluxo, telas de senha e mocks de erro continuam sem navbar.
O espaço e o limite de arraste do mascote da Home consideram a área útil acima da barra.

## Alteração de senha — textos para o Figma

Fluxo autenticado separado de “Esqueci minha senha”. E-mail obtido da conta
Firebase atual, sem input editável.

**Primeira tela**

- Título: Alterar senha
- Texto: Enviaremos um link para {email} com as instruções para você criar uma nova senha.
- Botão: Enviar link
- Seta: retorna ao Perfil.

**Segunda tela**

- Título: Confira seu e-mail
- Texto: Enviamos um link para {email}. Abra o e-mail e siga as instruções para alterar sua senha.
- Botão: Voltar ao perfil

Somente o sucesso do SDK abre a confirmação. Falha usa o erro genérico e retry
repete o envio. Enviar o e-mail não confirma que a senha foi alterada nem encerra
a sessão. O retorno remove os passos de envio e preserva o Perfil.

A confirmação substitui a tela anterior pela composição do frame `4071:7550`:
ícone de e-mail em círculo de 100dp, textos centralizados e botão roxo.
`include_password_email_confirmation.xml` é compartilhado com a recuperação do
login. A recuperação usa a mensagem neutra do Figma e mantém “Voltar à identificação
de e-mail”; o Perfil mantém “Voltar ao perfil”. Não há reenvio automático.

## Referências e recursos

- App V2 Gestor: https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz?node-id=2590-12314
- App V2 Colaborador: https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz?node-id=2593-17020
- Logout: https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz?node-id=2903-3567
- Confirmação de e-mail: https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz?node-id=4071-7550
- Ícones oficiais exportados do Figma e convertidos por Svg2Vector do Android Studio:
  UserGroup, Building, Tag, Shield, ChevronDown, ChevronUp, Lock (1011:880), Logout (2771:17)
  e Transaction (botão 4239:4560).
- Reutilizados e-mail, voltar, fechar, avatar padrão e navbar existentes.
- Layout fluido/rolável; dimensões de tablet e insets centralizados existentes preservados.
- Novas telas de senha seguem o padrão visual do app; não foram criadas no Figma.

## Verificação desta entrega

Compilação debug e lint sem erros. APK instalada no Samsung SM-A566E; perfil com
dados reais, cores dos ícones, toggle ligado/desligado, segunda NR após expansão
da primeira e popup fechado pelo X conferidos. Preferência original do toggle
restaurada. Nenhum logout confirmado, emulador usado ou e-mail real enviado.
Confirmações de senha revisadas estruturalmente contra o Figma; sua execução após
envio do e-mail fica para o teste manual.

Últimos ajustes: padding vertical do fundo do toggle corrigido e 8dp acima/abaixo
da linha. Bolinha com margem visível conferida no SM-A566E; botão “Trocar tipo de
usuário” com ícone Transaction preservado. Build/lint sem erros e APK reinstalada.
Home, Conversas, Assistente e Perfil usam a navbar única na Activity: a mesma posição
foi conferida nos quatro destinos. Teclado do chat abre espaço para o input, oculta
a navbar e, ao fechar, restaura a barra à posição original. Nenhuma mensagem,
redefinição de senha ou logout foi enviada durante esta validação.

Preparação da PR em 09/10/2026: main a430b9d integrada, preservando a ativação
da SCRUM-325 e as sessões da IA. Chevrons atualizados para 18dp com traço 1.5,
como na última versão aprovada. Build debug e lint aprovados (zero erros,
71 avisos). Recursos novos conferidos sem órfãos nem duplicatas binárias.
Os 20 testes unitários existentes também passaram, sem falhas ou erros.
Sem nova instalação, envio de e-mail ou teste de fluxo em dispositivo nesta preparação.
