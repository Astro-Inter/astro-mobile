# Primeiro cadastro — SCRUM-325

O fluxo atende ao Colaborador pré-cadastrado. O e-mail vem da identificação e o perfil permanece no ViewModel compartilhado da chave. A chave não é enviada para a tela da senha.

## Cadastro e retomada

- A chave validada abre a definição da senha; o botão é **Cadastrar**.
- Senha e confirmação reutilizam os ícones e o controle de mostrar/ocultar do login normal.
- Uma senha nova precisa de 8 a 4096 caracteres, maiúscula, minúscula, número e um dos caracteres especiais aceitos pelo Firebase. A confirmação deve ser igual.
- O Repository cria a conta Firebase. Após o sucesso, envia `POST /activate` com somente `email` e `firebaseUid`, sem token.
- Somente `204` libera a Home. `404` e demais falhas HTTP vão para o mock de erro genérico; ausência de conexão vai para o mock de conexão. Timeout da API é de 15 segundos.
- Se a ativação falhar, a conta Firebase não é apagada. Tentar novamente repete apenas a ativação com o mesmo UID, sem guardar senha ou token no aplicativo.
- Ao reabrir com sessão Firebase válida e status Astro `PRE_CADASTRADO`, a splash abre a conclusão do cadastro. Status `ATIVO` mantém o fluxo normal.
- Sem sessão Firebase, **Já cadastrei minha senha** na tela da chave permite autenticar a conta existente e retomar a ativação. Esse caminho não cria contas. Uma colisão no cadastro também exige autenticação antes de usar o UID existente.
- O contrato confirmado permite repetir a ativação com o mesmo e-mail/UID e receber `204`; assim também é possível recuperar uma resposta perdida.

## Configuração pendente no Firebase

Projeto Android: `astro-ba278`.

Em **Authentication > Settings > Password policy**, configurar mínimo de 8 caracteres, maiúscula, minúscula, número e caractere não alfanumérico. Aplicar a política às novas senhas, sem forçar atualização no login de usuários existentes.

A redefinição continua usando o link de e-mail do Firebase, sem página móvel de nova senha. A regra aplicada à nova senha nesse link depende da configuração do projeto e deve ser conferida manualmente.

Referência: [Política de senha do Firebase Authentication](https://firebase.google.com/docs/auth/android/password-auth#recommended_set_a_password_policy).

A política remota **não foi alterada** nesta implementação; sua configuração ainda precisa ser confirmada pelo responsável pelo Firebase.

## Verificação desta entrega

Em 2026-10-09, a branch foi atualizada com `origin/main` (`ff1a835`), preservando a integração de sessões da IA.
Revisão estática e diff sem erros; `assembleDebug` e `lintDebug` aprovados após essa atualização.
A versão anterior à integração da main foi instalada e aberta no Samsung SM-A566E para teste manual;
o solicitante aprovou a versão. Não foram executados testes automatizados nem cadastros/ativações reais pelo agente.

Na validação manual, conferir senha inválida, confirmação diferente, cadastro completo, falha na ativação seguida de retry, retomada após fechar o app, conta existente sem sessão, conexão ausente e timeout. Usar contas de teste autorizadas, pois cadastro e ativação alteram dados reais.
