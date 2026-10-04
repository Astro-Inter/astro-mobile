# CI do Android

O workflow `.github/workflows/ci.yml` executa `./gradlew test` e `./gradlew assembleDebug` em pull requests para `main` e pushes nessa branch.

- Instala Temurin 25, conforme `gradle/gradle-daemon-jvm.properties`.
- Gera `app/google-services.json` com identificadores fictícios antes dos testes. O plugin Google Services exige esse arquivo mesmo nos builds usados pelos testes unitários.
- O arquivo real continua no `.gitignore`; o CI não depende da configuração Firebase de um desenvolvedor nem de secrets indisponíveis em PRs externos.
- O APK gerado serve para validar a compilação. Essa configuração fictícia não autentica usuários nem representa um APK de distribuição; builds para uso real precisam da configuração Firebase apropriada.
- Publica os relatórios dos testes como artifact `unit-test-reports`, inclusive quando algum teste falha.

Os testes unitários usam respostas locais. Falhas de DNS são classificadas como conexão; uma `IOException` genérica mantém a classificação de erro interno. O ViewModel de verificação de e-mail cria o timeout Android somente enquanto existe uma consulta pendente, permitindo testar respostas síncronas sem depender de `Looper` no JVM.

Validação local de 04/10/2026: `test assembleDebug` concluiu com 20 testes unitários aprovados, inclusive usando o JSON fictício gerado pelo workflow como configuração temporária do build debug.
