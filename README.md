# OdontoLivreVal

Aplicativo Android livre e gratuito, projetado para que um cirurgião-dentista possa trabalhar offline em um único dispositivo. A versão **0.1.0** entrega a base do aplicativo para testes de navegação e aparência.

> **Versão de desenvolvimento.** Use apenas dados fictícios durante os testes. O aplicativo ainda não oferece cadastro de pacientes nem armazenamento clínico.

## O que funciona hoje

- **Início** com atalhos para as áreas do aplicativo.
- **Configurações** com tema claro, escuro ou do sistema. O DataStore salva a preferência para a próxima abertura.
- **Sobre** com a versão instalada e a licença.
- **Pacientes, Agenda e Odontograma** acessíveis pela navegação, com telas marcadas como “Em desenvolvimento”.

A interface está em português do Brasil. A versão atual funciona sem conta, servidor, anúncios ou serviços de análise e não solicita permissões do Android.

## Compilar e testar no celular

O APK roda em **Android 8.0 ou superior** (API 26). Para compilar, use JDK 17, Android SDK API 37 e Build Tools 36.0.0. O repositório inclui o Gradle Wrapper. A primeira compilação precisa de internet para baixar as dependências.

1. Clone o repositório e abra sua pasta no Android Studio ou no terminal.
2. Instale os componentes do SDK pelo SDK Manager. Configure `ANDROID_HOME` ou crie um `local.properties` na raiz com `sdk.dir=/caminho/do/sdk`.
3. Na raiz do projeto, gere o APK de desenvolvimento:

   ```sh
   ./gradlew :app:assembleDebug
   ```

   No Windows, use `gradlew.bat` no lugar de `./gradlew`.

4. Envie `app/build/outputs/apk/debug/app-debug.apk` ao celular, por exemplo pelo LocalSend. Abra o APK no Android e, se o sistema pedir, autorize a instalação pelo aplicativo que abriu o arquivo. Também é possível instalar com `./gradlew :app:installDebug` em um dispositivo conectado via ADB.
5. No celular, percorra as telas, selecione outro tema em **Configurações**, feche o aplicativo e reabra para conferir se a escolha foi salva.

## Verificações

| Comando | Resultado na versão 0.1.0 |
| --- | --- |
| `./gradlew :app:assembleDebug` | APK de desenvolvimento gerado |
| `./gradlew test` | 3 testes unitários aprovados |
| `./gradlew lint` | 0 erros e 7 avisos no módulo `:app` |
| `./gradlew :app:assembleDebugAndroidTest` | APK de testes instrumentados gerado |
| `./gradlew :app:connectedDebugAndroidTest` | Execução pendente em um celular ou emulador funcional |

O teste instrumentado cobre a navegação, a tela Sobre e a restauração do estado após recriar a tela. Os testes unitários cobrem a persistência do tema e o tratamento de falha ao salvar.

## Estrutura do código

A interface usa Kotlin, Jetpack Compose e Material 3. As versões das dependências ficam no [catálogo Gradle](gradle/libs.versions.toml).

| Módulo | Responsabilidade |
| --- | --- |
| `:app` | Inicialização, navegação, Início e Sobre |
| `:core:model` | Modelo da preferência de tema |
| `:core:designsystem` | Tema visual compartilhado |
| `:feature:settings` | Tela, estado e persistência das configurações |

Consulte a [arquitetura](docs/ARCHITECTURE.md), as [decisões de segurança](docs/SECURITY_DECISIONS.md) e o [histórico de versões](CHANGELOG.md).

## Próximas etapas

1. Validar a interface em dispositivos reais, incluindo rotação, fontes ampliadas e TalkBack.
2. Definir como proteger e recuperar os dados locais antes de implementar o cadastro de pacientes.
3. Implementar Pacientes, Agenda e Odontograma.
4. Preparar a assinatura e os metadados para uma futura distribuição pelo F-Droid.

## Licença

O código do OdontoLivreVal é licenciado sob a [GNU General Public License v3.0 ou posterior](LICENSE) (`GPL-3.0-or-later`). As dependências mantêm suas próprias licenças.
