# OdontoLivreVal

Aplicativo Android livre e gratuito, projetado para que um cirurgião-dentista possa trabalhar offline em um único dispositivo. A versão **0.4.0** protege o cadastro e a agenda de teste e acrescenta bloqueio e backup.

> **Versão de desenvolvimento.** Use apenas nomes fictícios. O app segue em validação com dados fictícios.

## O que funciona hoje

- **Início** com atalhos para as áreas do aplicativo.
- **Configurações** com tema claro de fundo branco e detalhes roxos, tema escuro com detalhes lilás ou a opção de seguir o sistema. O DataStore salva a preferência para a próxima abertura.
- **Segurança e backup** em Configurações, com bloqueio opcional e exportação ou restauração de uma cópia criptografada.
- **Sobre** com a versão instalada e a licença.
- **Pacientes** com cadastro, busca por nome e edição. Os registros de teste continuam salvos após fechar o app.
- **Agenda** diária com marcação, remarcação e cancelamento de consultas fictícias. A agenda impede duplicar um horário ocupado.
- **Odontograma** acessível pela navegação, com a tela marcada como “Em desenvolvimento”.

A interface está em português do Brasil. A versão atual funciona sem conta, servidor, anúncios ou serviços de análise. O app declara acesso à autenticação biométrica do Android para o bloqueio opcional.

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
5. No celular, abra **Pacientes → Novo paciente** e salve um nome fictício. Em **Agenda → Nova consulta**, selecione esse paciente e marque um horário. Feche o app e abra a agenda no mesmo dia para conferir se a consulta foi salva.

## Testar segurança e backup

1. Atualize por cima da 0.3.0 e abra **Pacientes** ou **Agenda**. Verifique se os registros fictícios aparecem. A primeira abertura migra os registros anteriores para o arquivo criptografado.
2. Em **Configurações → Segurança e backup**, ative o bloqueio com a credencial do Android. Saia do app e volte para conferir o desbloqueio. Em Android 11 ou superior, o sistema também pode oferecer biometria forte.
3. Em **Exportar backup**, escolha onde salvar o arquivo e confirme uma senha própria de pelo menos 12 caracteres. Para restaurar em outro aparelho, transfira o arquivo e use **Restaurar backup** com a mesma senha.
4. A restauração substitui os cadastros e consultas atuais. Teste primeiro uma senha incorreta e confirme que os dados atuais permanecem disponíveis.

## Verificações

| Comando | Resultado na versão 0.3.0 |
| --- | --- |
| `./gradlew :app:assembleDebug` | APK de desenvolvimento gerado |
| `./gradlew test` | 3 testes unitários aprovados |
| `./gradlew lint` | 0 erros e 6 avisos no módulo `:app` |
| `./gradlew :app:assembleDebugAndroidTest` | APK de testes instrumentados gerado |
| `./gradlew :app:connectedDebugAndroidTest` | Execução pendente em um celular ou emulador funcional |

Os testes instrumentados incluem navegação, cadastro de teste e migração do banco com marcação, remarcação, cancelamento e conflito de horário. Os testes unitários cobrem a persistência do tema e o tratamento de falha ao salvar.

## Estrutura do código

A interface usa Kotlin, Jetpack Compose e Material 3. As versões das dependências ficam no [catálogo Gradle](gradle/libs.versions.toml).

| Módulo | Responsabilidade |
| --- | --- |
| `:app` | Inicialização, navegação, Início e Sobre |
| `:core:model` | Modelo da preferência de tema |
| `:core:designsystem` | Tema visual compartilhado |
| `:feature:settings` | Tela, estado e persistência das configurações |
| `:core:demo` | Armazenamento criptografado compartilhado e migração do SQLite de teste |
| `:feature:patients` | Cadastro, busca e edição de pacientes de teste |
| `:feature:schedule` | Agenda diária de teste e gerenciamento de consultas |

Consulte a [arquitetura](docs/ARCHITECTURE.md), as [decisões de segurança](docs/SECURITY_DECISIONS.md) e o [histórico de versões](CHANGELOG.md).

## Próximas etapas

1. Validar a migração, o bloqueio e a restauração entre aparelhos com dados fictícios e revisar a proteção antes do uso clínico.
2. Completar o fluxo clínico com dados de contato, histórico, anamnese, evolução e odontograma FDI por dente e superfície. Vincular fotos e radiografias ao paciente ou ao dente.
3. Ampliar a agenda com visão mensal, retornos e lembretes. Mostrar as próximas consultas e pacientes recentes no Início.
4. Adicionar perfil profissional e CRO, receitas, atestados, orçamentos e consentimentos em PDF. Usar o compartilhamento do Android para permitir o envio pelo WhatsApp.
5. Tratar assinatura digital em etapa própria e preparar a distribuição pelo F-Droid.

## Licença

O código do OdontoLivreVal é licenciado sob a [GNU General Public License v3.0 ou posterior](LICENSE) (`GPL-3.0-or-later`). As dependências mantêm suas próprias licenças.

O emoji 🦷 do ícone foi adaptado de [Twemoji](https://github.com/jdecked/twemoji/blob/main/assets/svg/1f9b7.svg), de Twitter e colaboradores, sob a licença [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). A arte foi convertida em um vetor Android e recebeu um fundo roxo.
