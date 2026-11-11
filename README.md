# OdontoLivreVal

Aplicativo Android livre e gratuito, projetado para que um cirurgião-dentista possa trabalhar offline em um único dispositivo. A versão de desenvolvimento **0.13.0** (versionCode 13) reúne as etapas 13A–15B; ainda não foi lançada nem submetida ao F-Droid. A inclusão no repositório principal não está elegível até cumprir o período de manutenção previsto na política ou demonstrar uma exceção.

> **Versão de desenvolvimento.** Use apenas nomes fictícios. O app segue em validação com dados fictícios.

## O que funciona hoje

- **Início** com próximas consultas e atalhos para as áreas do aplicativo.
- **Configurações** com tema claro de fundo branco e detalhes roxos, tema escuro com detalhes lilás ou a opção de seguir o sistema. O DataStore salva a preferência para a próxima abertura.
- **Segurança e backup** em Configurações, com bloqueio opcional e exportação ou restauração de uma cópia criptografada.
- **Perfil profissional** com nome, CRO e dados da clínica.
- **Sobre** com a versão instalada e a licença.
- **Pacientes** com cadastro, busca sem distinção de acentos, edição e exclusão confirmada. Os registros de teste continuam salvos após fechar o app.
- **Prontuário** ao tocar no nome do paciente: ficha com nascimento, telefone, queixa, anamnese e observações; diagnóstico e conduta por consulta; histórico de evoluções.
- **Odontograma** dentro do prontuário, com dentições permanente e decídua. Aceita marcação no dente inteiro e cárie ou restauração em faces individuais. A evolução salva pode ser exportada em PDF.
- **Radiografias** no prontuário, importadas em JPG/PNG ou fotografadas, com visualização e remoção.
- **Receitas odontológicas** no prontuário, com importação de arquivos PDF/DOCX já emitidos e opção de salvar uma cópia.
- **Documentos** com modelos de receita, atestado, orçamento e consentimento, exportação em PDF e arquivo de PDFs devolvidos pelo assinador.
- **Atestados e declarações** (atestado odontológico, declaração de comparecimento, declaração de acompanhamento) no prontuário, com ciclo de vida rascunho/finalizado/cancelado, nova versão para correções, PDF A4 e compartilhamento.
- **Planos de tratamento e orçamentos** no prontuário: criar planos, adicionar procedimentos vinculados a dentes, definir valores e etapas, gerar orçamento com cálculos corretos em PDF A4, registrar aprovação e acompanhar execução, consultar versões anteriores.
- **Termos de consentimento (TCLE)** no prontuário: criar termos a partir de 9 modelos, editar conteúdo, registrar decisão do paciente (aceito/recusado), revogar, criar revisões, gerar PDF A4 com espaços para assinatura, compartilhar.
- **Registros de confirmação e arquivo de PDFs** no prontuário. A confirmação digitada registra nome/data/modalidade localmente; não captura assinatura manuscrita, não certifica identidade e não é assinatura criptográfica. PDFs selecionados podem ser arquivados criptografados por paciente; o hash SHA-256 é uma referência e não valida a assinatura. A modalidade criptográfica está indisponível.
- **Agenda** diária e mensal, com duração da consulta, prevenção de sobreposição, retornos vinculados e lembretes locais. A visão mensal mostra quantas consultas ativas há por dia.

A interface está em português do Brasil. A versão atual funciona sem conta, servidor, anúncios ou serviços de análise. O app declara acesso à autenticação biométrica do Android para o bloqueio opcional. Veja [PRIVACY.md](PRIVACY.md) para tratamento de dados e limitações.

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

## Assinar e arquivar um documento

1. Em **Configurações → Perfil profissional**, preencha o nome e o CRO. Em seguida, abra **Pacientes → Prontuário → Documentos**.
2. Escolha o modelo e use **Salvar PDF sem assinatura**. O PDF gerado contém a identificação profissional. A exportação do odontograma também inclui nome e CRO quando preenchidos.
3. Assine no serviço adequado ao tipo de documento. O [assinador GOV.BR](https://www.gov.br/governodigital/pt-br/identidade/assinatura-eletronica) fornece assinatura avançada. O [portal de Prescrição Eletrônica do CFO](https://prescricao.cfo.org.br/index) oferece emissão com certificado ICP-Brasil. A [Anvisa](https://www.gov.br/anvisa/pt-br/assuntos/medicamentos/controlados/sncr/perguntas-e-respostas/perguntas-e-respostas-rdc-1000-2025-1-ed/%40%40download/file) diferencia os requisitos conforme o tipo de receita.
4. Confira o PDF final no [VALIDAR do ITI](https://validar.iti.gov.br/). De volta ao prontuário, escolha **Importar PDF do assinador** para guardá-lo criptografado, sem alterar o arquivo. O arquivamento é independente da verificação no portal.

A assinatura é aplicada pelo serviço externo. A interface abre o navegador e a seleção do arquivo é feita pelo usuário no próprio serviço. Para validar o fluxo, use arquivos com dados fictícios.

## Testar segurança e backup

1. Atualize por cima da 0.4.0 e abra **Pacientes** ou **Agenda**. Verifique se os registros fictícios aparecem. A primeira abertura migra os registros anteriores para o arquivo criptografado.
2. Em **Configurações → Segurança e backup**, ative o bloqueio com a credencial do Android. Saia do app e volte para conferir o desbloqueio. Em Android 11 ou superior, o sistema também pode oferecer biometria forte.
3. Em **Exportar backup**, escolha onde salvar o arquivo e confirme uma senha própria de pelo menos 12 caracteres. O formato 3 guarda a versão e o custo do PBKDF2 e processa cada anexo separadamente.
4. Para restaurar em outro aparelho, transfira o arquivo e use **Restaurar backup** com a mesma senha. Os formatos 1 e 2 continuam aceitos. Durante a restauração, é necessário espaço livre para os novos anexos enquanto os atuais ainda estão guardados.
5. A restauração substitui os cadastros e consultas atuais. Teste primeiro uma senha incorreta e confirme que os dados atuais permanecem disponíveis.

## Verificações

| Comando | Resultado da candidata 0.13.0 |
| --- | --- |
| `./gradlew :app:assembleDebug` | ✅ Sucesso — APK debug em `~/Downloads/OdontoLivreVal-0.13.0-debug.apk` |
| `./gradlew :app:assembleRelease` | O build sem variáveis de assinatura gera APK release sem assinatura de desenvolvedor; o F-Droid assina o APK distribuído. Para distribuição própria, configure uma chave segura por variáveis de ambiente. |
| `./gradlew test` | ✅ Sucesso — 20 testes unitários passaram |
| `./gradlew lint` | ✅ Sucesso — sem problemas no código do app (apenas aviso interno `ObsoleteLintCustomCheck` do AndroidX Lifecycle 2.11.0) |
| `./gradlew :app:assembleDebugAndroidTest` | ✅ Sucesso — APK de testes compilado; execução conectada: 11 testes passaram (Motorola Edge 70 Fusion) |

Os testes instrumentados existentes cobrem cenários específicos com registros fictícios. Em 09/11/2026, a execução conectada no Motorola passou (11 testes); a validação manual ampliada continua no roteiro. Consulte [docs/ROTEIRO_TESTES_MANUAIS.md](docs/ROTEIRO_TESTES_MANUAIS.md) e o [estado de prontidão](docs/ETAPA_15B_RELEASE_READINESS.md).

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
| `:feature:schedule` | Agenda diária e mensal de consultas |
| `:feature:documents` | Modelos de documentos, PDF e acesso aos portais de assinatura |

Consulte a [arquitetura](docs/ARCHITECTURE.md), as [decisões de segurança](docs/SECURITY_DECISIONS.md), o [resultado da auditoria](docs/AUDITORIA_FINAL_ETAPA_15A.md), o [estado de prontidão](docs/ETAPA_15B_RELEASE_READINESS.md), a [preparação F-Droid](docs/FDROID_READINESS.md) e o [histórico de versões](CHANGELOG.md).

## Próximas etapas

1. Validar em aparelhos reais a migração, o bloqueio, a restauração e o comportamento em diferentes tamanhos de tela e escalas de fonte.
2. Validar o volume e a recuperação de anexos em aparelhos reais e migrar o índice e os registros para um banco criptografado antes do uso clínico.
3. Validar em aparelho os lembretes, o painel e o fluxo de documentos e assinatura externa.
4. Avaliar assinatura qualificada dentro do app com certificado ICP-Brasil, incluindo verificação da cadeia e requisitos por tipo de documento.
5. Cumprir os requisitos de elegibilidade e revisão do [F-Droid](docs/FDROID_READINESS.md) antes de solicitar inclusão.

## Licença

O código do OdontoLivreVal é licenciado sob a [GNU General Public License v3.0 ou posterior](LICENSE) (`GPL-3.0-or-later`). As dependências mantêm suas próprias licenças.

O emoji 🦷 do ícone foi adaptado de [Twemoji](https://github.com/jdecked/twemoji/blob/main/assets/svg/1f9b7.svg), de Twitter e colaboradores, sob a licença [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). A arte foi convertida em um vetor Android e recebeu um fundo roxo.
