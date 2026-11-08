# OdontoLivreVal

Aplicativo Android livre e gratuito, projetado para que um cirurgião-dentista possa trabalhar offline em um único dispositivo. A versão **0.10.0** acrescenta duração das consultas, prevenção de horários sobrepostos e retornos vinculados.

> **Versão de desenvolvimento.** Use apenas nomes fictícios. O app segue em validação com dados fictícios.

## O que funciona hoje

- **Início** com atalhos para as áreas do aplicativo.
- **Configurações** com tema claro de fundo branco e detalhes roxos, tema escuro com detalhes lilás ou a opção de seguir o sistema. O DataStore salva a preferência para a próxima abertura.
- **Segurança e backup** em Configurações, com bloqueio opcional e exportação ou restauração de uma cópia criptografada.
- **Sobre** com a versão instalada e a licença.
- **Pacientes** com cadastro, busca sem distinção de acentos, edição e exclusão confirmada. Os registros de teste continuam salvos após fechar o app.
- **Prontuário** ao tocar no nome do paciente: ficha com nascimento, telefone, queixa, anamnese e observações; diagnóstico e conduta por consulta; histórico de evoluções.
- **Odontograma** dentro do prontuário, com dentições permanente e decídua. Aceita marcação no dente inteiro e cárie ou restauração em faces individuais. A evolução salva pode ser exportada em PDF.
- **Radiografias** no prontuário, importadas em JPG/PNG ou fotografadas, com visualização e remoção.
- **Receitas odontológicas** no prontuário, com importação de arquivos PDF/DOCX já emitidos e opção de salvar uma cópia.
- **Agenda** diária e mensal, com duração da consulta, prevenção de sobreposição e retornos vinculados. A visão mensal mostra quantas consultas ativas há por dia.

A interface está em português do Brasil. A versão atual funciona sem conta, servidor, anúncios ou serviços de análise. O app declara acesso à autenticação biométrica do Android para o bloqueio opcional.

## Compilar e testar no celular

O APK roda em **Android 8.0 ou superior** (API 26). Para compilar, use JDK 17 ou superior, Android SDK API 37 e Build Tools 36.0.0. O repositório inclui o Gradle Wrapper. A primeira compilação precisa de internet para baixar as dependências.

1. Clone o repositório e abra sua pasta no Android Studio ou no terminal.
2. Instale os componentes do SDK pelo SDK Manager. Configure `ANDROID_HOME` ou crie um `local.properties` na raiz com `sdk.dir=/caminho/do/sdk`.
3. Na raiz do projeto, gere o APK de desenvolvimento:

   ```sh
   ./gradlew :app:assembleDebug
   ```

   No Windows, use `gradlew.bat` no lugar de `./gradlew`.

4. Envie `app/build/outputs/apk/debug/app-debug.apk` ao celular, por exemplo pelo LocalSend. Abra o APK no Android e, se o sistema pedir, autorize a instalação pelo aplicativo que abriu o arquivo. Também é possível instalar com `./gradlew :app:installDebug` em um dispositivo conectado via ADB.
5. No celular, abra **Pacientes → Novo paciente** e salve um nome fictício. Toque no nome para abrir o prontuário, marque um dente e use **Salvar ficha**. Volte à **Agenda → Nova consulta**, selecione esse paciente e marque um horário.

## Testar duração e retornos na agenda

1. Em **Agenda → Nova consulta**, informe o horário e a duração em minutos. Tente marcar outra consulta que ocupe parte do mesmo intervalo e confira se o app bloqueia o conflito.
2. Numa consulta ativa, toque em **Marcar retorno**. O paciente fica vinculado à consulta original. Ajuste a data, o horário e a duração conforme necessário.
3. Na atualização, consultas existentes preservam a hora de início e recebem até 30 minutos, limitados pela próxima consulta ativa do dia ou pela meia-noite.

## Testar anexos e odontograma em PDF

1. Em **Pacientes**, abra um prontuário fictício. No odontograma, marque **Canal** em um dente e toque em **Salvar ficha**. Depois escolha **Exportar odontograma em PDF**.
2. Em **Radiografias**, escolha **Anexar imagem** ou **Tirar foto**. Use uma imagem fictícia. A foto é redimensionada ao salvar.
3. Em **Receitas odontológicas**, anexe um PDF ou DOCX existente. Use **Salvar cópia** para abrir o arquivo em um leitor ou compartilhá-lo pelo Android.

Cada anexo aceita até 10 MiB, com limite de 512 MiB somados para radiografias e receitas. O índice e os registros do prontuário continuam com um limite separado de 4 MiB. Anexos existentes são migrados no primeiro acesso aos dados e entram no backup criptografado. O PDF do odontograma e as cópias de receitas que você salva ficam fora da proteção do app.

## Testar segurança e backup

1. Atualize por cima da 0.4.0 e abra **Pacientes** ou **Agenda**. Verifique se os registros fictícios aparecem. A primeira abertura migra os registros anteriores para o arquivo criptografado.
2. Em **Configurações → Segurança e backup**, ative o bloqueio com a credencial do Android. Saia do app e volte para conferir o desbloqueio. Em Android 11 ou superior, o sistema também pode oferecer biometria forte.
3. Em **Exportar backup**, escolha onde salvar o arquivo e confirme uma senha própria de pelo menos 12 caracteres. O formato 3 guarda a versão e o custo do PBKDF2 e processa cada anexo separadamente.
4. Para restaurar em outro aparelho, transfira o arquivo e use **Restaurar backup** com a mesma senha. Os formatos 1 e 2 continuam aceitos. Durante a restauração, é necessário espaço livre para os novos anexos enquanto os atuais ainda estão guardados.
5. A restauração substitui os cadastros e consultas atuais. Teste primeiro uma senha incorreta e confirme que os dados atuais permanecem disponíveis.

## Verificações

| Comando | Resultado na versão 0.10.0 |
| --- | --- |
| `./gradlew :app:assembleDebug` | APK de desenvolvimento gerado |
| `./gradlew test` | 20 testes unitários aprovados |
| `./gradlew lint` | 0 erros e 3 avisos nos relatórios dos módulos |
| `./gradlew :app:assembleDebugAndroidTest` | APK de testes instrumentados gerado |
| `./gradlew :app:connectedDebugAndroidTest` | Execução pendente em um celular ou emulador funcional |

Os testes instrumentados cobrem navegação, cadastro de paciente, abertura do prontuário, marcação de dente, migração do banco, agenda, geração de PDF e restauração de anexos. Os testes unitários cobrem tema, criptografia e formato de backup, pacientes, prontuário e agenda.

## Estrutura do código

A interface usa Kotlin, Jetpack Compose e Material 3. As versões das dependências ficam no [catálogo Gradle](gradle/libs.versions.toml).

| Módulo | Responsabilidade |
| --- | --- |
| `:app` | Inicialização, navegação, Início, Sobre e proteção da janela |
| `:core:model` | Modelo da preferência de tema |
| `:core:designsystem` | Tema visual compartilhado |
| `:feature:settings` | Tela, estado e persistência das configurações |
| `:core:demo` | Contrato `ClinicStore`, armazenamento criptografado, backup e migração do SQLite |
| `:feature:patients` | Pacientes, prontuário, evoluções e odontograma |
| `:feature:schedule` | Agenda diária de consultas |

Consulte a [arquitetura](docs/ARCHITECTURE.md), as [decisões de segurança](docs/SECURITY_DECISIONS.md) e o [histórico de versões](CHANGELOG.md).

## Próximas etapas

1. Validar em aparelhos reais a migração, o bloqueio, a restauração e o comportamento em diferentes tamanhos de tela e escalas de fonte.
2. Validar o volume e a recuperação de anexos em aparelhos reais e migrar o índice e os registros para um banco criptografado antes do uso clínico.
3. Incluir lembretes e o painel de próximas consultas no Início.
4. Adicionar perfil profissional com CRO.
5. Implementar emissão de receitas, atestados, orçamentos e consentimentos, com exportação e compartilhamento pelo Android. Tratar assinatura digital em etapa própria.
6. Preparar a distribuição pelo F-Droid.

## Licença

O código do OdontoLivreVal é licenciado sob a [GNU General Public License v3.0 ou posterior](LICENSE) (`GPL-3.0-or-later`). As dependências mantêm suas próprias licenças.

O emoji 🦷 do ícone foi adaptado de [Twemoji](https://github.com/jdecked/twemoji/blob/main/assets/svg/1f9b7.svg), de Twitter e colaboradores, sob a licença [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). A arte foi convertida em um vetor Android e recebeu um fundo roxo.
