# Astro Mobile

O Astro é um aplicativo Android para colaboradores e gestores acompanharem atividades de trabalho em um só lugar. Ele reúne eventos, formulários, informações da equipe e conversas, ajudando as pessoas a identificar pendências e acompanhar suas atividades.

## Conheça o aplicativo

Colaboradores podem consultar eventos e seus materiais, enviar conclusões e evidências, preencher formulários, receber alertas e conversar com gestores ou com o Astrinho, assistente de IA do Astro.

Gestores podem acompanhar eventos e participantes, atribuir ou validar conclusões, consultar colaboradores e usar as conversas. As opções disponíveis dependem do perfil e das permissões da conta.

O desenho das telas e os fluxos podem ser consultados no [Figma do Astro](https://www.figma.com/design/qdCrRXAVuiNhNkBuim95zz/Astro-2%C2%B0-ano).

## Antes de começar

Para compilar e executar o projeto, você precisa de:

- Android Studio e Android SDK com a plataforma API 37 instalada.
- JDK 25 para o processo do Gradle, conforme `gradle/gradle-daemon-jvm.properties`. O código Java do app usa compatibilidade Java 11.
- Um aparelho Android com API 30 ou superior, com depuração USB autorizada, ou um dispositivo virtual Android.
- Acesso à configuração Firebase do projeto e uma conta autorizada para entrar no aplicativo.
- Conexão com os serviços do Astro para utilizar as funcionalidades integradas.

O Gradle Wrapper baixa a versão configurada no repositório e pode provisionar o toolchain definido para o projeto.

## Executar o projeto

1. Clone o repositório:

   ```bash
   git clone https://github.com/Astro-Inter/astro-mobile.git
   cd astro-mobile
   ```

2. Obtenha a configuração Firebase apropriada com a equipe e coloque o arquivo `google-services.json` dentro de `app/`. O arquivo é ignorado pelo Git.

3. Abra o projeto no Android Studio, aguarde a sincronização do Gradle, selecione um dispositivo e execute a configuração do módulo `app`.

Para compilar e instalar pelo terminal, com um dispositivo conectado:

```powershell
# Windows
.\gradlew.bat :app:installDebug
```

```bash
# macOS ou Linux
./gradlew :app:installDebug
```

## Configuração dos serviços

As bases dos serviços são definidas em `app/build.gradle.kts` e disponibilizadas ao app pelo `BuildConfig`:

| Campo | Serviço |
| --- | --- |
| `API_BASE_URL` | API principal do Astro |
| `CHAT_WEBSOCKET_URL` | WebSocket do chat humano |
| `AI_API_BASE_URL` | Serviço do Astrinho |
| `PASSWORD_RESET_API_BASE_URL` | Serviço de recuperação de senha |

O projeto não carrega arquivos `.env`: alterar um arquivo desse tipo não muda as configurações do aplicativo. Para usar outro ambiente, ajuste as bases na configuração do Gradle e forneça o `google-services.json` correspondente. URLs Retrofit precisam terminar com `/`.

Não adicione ao aplicativo chaves privadas, credenciais SMTP ou tokens administrativos.

## Tecnologias e organização

O app é desenvolvido em **Java e XML**, com uma Activity e navegação entre Fragments. A organização usa **MVVM**: Fragments apresentam as telas, ViewModels coordenam o estado e Repositories concentram o acesso aos dados.

As principais bibliotecas são Android Navigation e ViewModel, Retrofit com Gson para HTTP, Firebase Authentication, Cloud Firestore e Firebase Cloud Messaging, Glide para imagens e WorkManager para trabalhos em segundo plano. O chat humano usa WebSocket com STOMP. Os fluxos usam callbacks; os chats existentes também usam LiveData observado pelo ciclo de vida para compartilhar o estado de mensagens. Essa escolha é mantida para preservar o comportamento atual, sem estender o padrão às demais telas.

Chamadas HTTP e Firebase ficam em `data/`, inclusive a obtenção de avatares assinados e a conexão do calendário do Astrinho. A tela de erro compartilha a apresentação e delega a recuperação ao fluxo de origem.

```text
app/src/main/java/com/example/astro_mobile/
├── auth/           autenticação e acesso
├── chat/           conversas e Astrinho
├── data/           APIs, Firebase e dados locais
├── employee/       telas do colaborador
├── manager/        telas do gestor
├── navigation/     entrada e navegação
├── notifications/  alertas
└── shared/         recursos compartilhados

app/src/main/res/   layouts, imagens, textos e temas
docs/               documentação das funcionalidades
```

## Gerar APK

Para gerar uma versão debug sem instalá-la:

```powershell
# Windows
.\gradlew.bat :app:assembleDebug
```

```bash
# macOS ou Linux
./gradlew :app:assembleDebug
```

O arquivo gerado fica em `app/build/outputs/apk/debug/app-debug.apk`. A distribuição de uma versão release requer configurar uma assinatura apropriada. O build release habilita R8 e otimização de recursos; as regras de preservação dos contratos Gson ficam em `app/src/main/keepRules/serialization.keep`. A licença da fonte Montserrat é preservada no pacote.

## Contribuição

Crie uma branch para a alteração, descreva o que mudou e abra um pull request para revisão. A documentação das funcionalidades está na pasta [docs](docs/).

## Projeto e licença

Desenvolvido pela equipe [Astro Inter](https://github.com/Astro-Inter). O projeto está sob a [licença MIT](LICENSE).
