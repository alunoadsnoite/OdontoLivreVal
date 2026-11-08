# AUDITORIA TÉCNICA — Etapa 11A

**Projeto:** OdontoLivreVal  
**Versão analisada:** 0.11.0 (conforme `app/build.gradle.kts`: versionCode=11, versionName="0.11.0")  
**Commit analisado:** `da2e914` — `feat: assinatura digital em documentos`  
**Data da auditoria:** 08/11/2026  
**Auditor:** Engenheiro Android Sênior (Kotlin, Jetpack Compose, Segurança, Persistência)  
**Status:** Diagnóstico preliminar — nenhuma modificação foi realizada no código.

---

## 1. Versão e commit analisados

- Repositório local: `/home/valdenor/Projetos/OdontoLivreVal`
- Commit HEAD: `da2e914 feat: assinatura digital em documentos`
- Branches: `main` (única)
- Versão no `app/build.gradle.kts`: `versionCode = 11`, `versionName = "0.11.0"`
- Versão no `CHANGELOG.md`: `0.11.0 — 2026-11-08`
- Compile SDK: 37, Min SDK: 26, Target SDK: 37
- AGP: 9.5.1, Gradle: 9.9.0, Kotlin: 2.4.21, Compose BOM: 2026.09.00

---

## 2. Funcionalidades confirmadas

As seguintes funcionalidades estão implementadas e compilando:

| Módulo | Funcionalidade | Status |
|--------|---------------|--------|
| `:app` | Início com painel de próximas consultas (máx. 30 dias, até 6 itens) | Implementado |
| `:app` | Navegação entre Início, Pacientes, Agenda, Configurações, Sobre | Implementado |
| `:app` | Lembretes locais (~1h antes) via AlarmManager + BootReceiver | Implementado |
| `:app` | Bloqueio opcional do app com credencial Android / biometria | Implementado |
| `:app` | Exportação e restauração de backup criptografado | Implementado |
| `:feature:patients` | Cadastro de pacientes (busca, edição, exclusão confirmada) | Implementado |
| `:feature:patients` | Prontuário: ficha, anamnese, diagnóstico, conduta, observações | Implementado |
| `:feature:patients` | Histórico de consultas/evoluções | Implementado |
| `:feature:patients` | Odontograma: dentição permanente e decídua, marcação por dente inteiro e por face | Implementado |
| `:feature:patients` | Exportação do odontograma em PDF (com nome e CRO do profissional) | Implementado |
| `:feature:patients` | Radiografias: anexar (arquivo/câmera), visualizar, remover | Implementado |
| `:feature:patients` | Receitas odontológicas: anexar PDF/DOCX, salvar cópia | Implementado |
| `:feature:patients` | Fotos de documentos: anexar/tirar foto, visualizar, remover | Implementado |
| `:feature:schedule` | Agenda diária e mensal, duração 1–240 min, prevenção de sobreposição | Implementado |
| `:feature:schedule` | Retornos vinculados à consulta original e mesmo paciente | Implementado |
| `:feature:settings` | Tema claro/escuro/sistema (DataStore) | Implementado |
| `:feature:settings` | Perfil profissional: nome, CRO, especialidade, clínica, endereço, telefone | Implementado |
| `:feature:documents` | PDF: receita, atestado, orçamento, consentimento (conteúdo livre + template) | Implementado |
| `:feature:documents` | Assinatura digital (renderização de texto como bitmap no PDF) | Implementado |
| `:feature:documents` | Compartilhamento de PDF via FileProvider | Implementado |
| `:core:demo` | Armazenamento criptografado (AES-256-GCM, Android Keystore) | Implementado |
| `:core:demo` | Backup criptografado (PBKDF2 600k iterações, AES-256-GCM) | Implementado |
| `:core:demo` | Migração de versões 1–10 do formato de dados | Implementado |

---

## 3. Resultados dos testes

Os seguintes comandos foram executados com sucesso:

| Comando | Resultado | Observações |
|---------|-----------|-------------|
| `./gradlew :app:assembleDebug` | ✅ SUCESSO | APK gerado em `app/build/outputs/apk/debug/app-debug.apk` (~14 MB) |
| `./gradlew test` | ✅ SUCESSO | 20 testes unitários aprovados (core:demo, feature:patients, feature:schedule, feature:settings) |
| `./gradlew lint` | ✅ SUCESSO | 0 erros, 3 avisos (dependências desatualizadas, warnings de SDK) |
| `./gradlew :app:assembleDebugAndroidTest` | ✅ SUCESSO | APK de testes instrumentados gerado |
| `./gradlew connectedDebugAndroidTest` | ⏸️ NÃO EXECUTADO | Nenhum dispositivo ou emulador disponível (`adb devices` vazio) |

Avisos de lint (não críticos):
- `GradleDependency`: versões de biblioteca não são as mais recentes estáveis (ex: `androidx.core:core-ktx`).
- `ObsoleteSdkInt`: verificação de versão desnecessária em `AppointmentReminders.kt` (minSdk 26 já cobre a versão verificada).

---

## 4. Problemas identificados

### 4.1 Problemas críticos

| ID | Gravidade | Descrição | Local |
|----|-----------|-----------|-------|
| C1 | Crítico | **Assinatura digital não é criptográfica.** O componente `SignaturePad` renderiza o nome digitado como bitmap e o embaralha no PDF como uma "assinatura". Não há uso de certificado digital ICP-Brasil, não há validação de assinatura, não há rastreamento de integridade do documento após assinatura. Para uso clínico odontológico (receitas, atestados, consentimentos), isso não tem validade jurídica como assinatura eletrônica qualificada. | `feature/documents/SignaturePad.kt`, `feature/documents/ClinicPdf.kt` |
| C2 | Crítico | **PDFs gerados são salvos no cache do app sem criptografia adicional.** O arquivo PDF fica em `cacheDir/documents/` e é compartilhado via `FileProvider`. Qualquer app com permissão de leitura de armazenamento externo ou acesso ao cache pode ler o conteúdo do documento clínico gerado. | `feature/documents/ClinicPdf.shareFile()` |
| C3 | Alto | **Dados sensíveis descriptografados permanecem em memória durante o uso.** O snapshot é descriptografado para a RAM e permanece lá enquanto o app está aberto. Não há limpeza proativa de dados sensíveis da memória (exceto senhas de backup que são zeradas). | `core/demo/DemoClinicStore.load()` / `persist()` |

### 4.2 Problemas altos

| ID | Gravidade | Descrição | Local |
|----|-----------|-----------|-------|
| A1 | Alto | **Armazenamento em arquivo único (snapshot) carrega todo o índice em memória a cada operação.** Não há paginação, não há índices, não há consultas seletivas. À medida que o prontuário cresce (milhares de consultas, anexos), o tempo de leitura/escrita e o consumo de memória aumentarão linearmente. | `core/demo/DemoClinicStore.load()` / `persist()` |
| A2 | Alto | **Não há transações ACID reais.** O `AtomicFile` fornece atomicidade de escrita (grava em arquivo temporário e renomeia), mas não oferece isolamento, consistência ou durabilidade de banco de dados. Uma falha de escrita ou energia durante o `persist()` pode deixar o snapshot em estado inconsistente. | `core/demo/DemoClinicStore.persist()` |
| A3 | Alto | **Não há compactação ou "vacuum" do armazenamento.** Exclusões de registros não reduzem o tamanho do arquivo criptografado. O snapshot cresce monotonicamente. | `core/demo/DemoClinicStore.persist()` |
| A4 | Médio-Alto | **Arquivos temporários de captura de câmera ficam no cache antes da criptografia.** Imagens capturadas pela câmera são gravadas como arquivos JPG temporários em `cacheDir/radiograph-capture/` e `cacheDir/document-capture/` antes de serem criptografadas e anexadas. Se o app for interrompido nesse meio-tempo, a imagem fica exposta no cache. | `app/src/main/kotlin/.../MainActivity.kt` (captureFile / captureDocumentFile) |
| A5 | Médio | **Não há detecção de root ou verificação de integridade do ambiente.** O app não verifica se está rodando em dispositivo rootado, em emulador, ou se há apps de acessibilidade ativos que podem capturar tela. | `app/src/main/kotlin/.../DeviceLock.kt` |

### 4.3 Problemas médios

| ID | Gravidade | Descrição | Local |
|----|-----------|-----------|-------|
| M1 | Médio | **Backup não é reversível facilmente.** A restauração substitui todos os dados atuais. Não há modo de pré-visualizar o conteúdo do backup antes de restaurar, nem modo de mesclar seletivamente. | `core/demo/DemoClinicStore.restoreBackup()` |
| M2 | Médio | **Senha de backup tem comprimento mínimo 12 caracteres, mas não há validação de complexidade (maiúsculas, números, símbolos).** Uma senha de 12 caracteres alfanuméricos simples é mais fraca que o recomendado para dados de saúde. | `app/src/main/kotlin/.../BackupViewModel.kt`, `ClinicProtectionScreen.kt` |
| M3 | Médio | **Não há registro de auditoria (audit log) de quem acessou ou modificou qual prontuário.** Para uso clínico, é desejável registrar acessos, alterações e exportações. | Nenhuma implementação encontrada |
| M4 | Médio | **A integração com GOV.BR / CFO / VALIDAR é apenas abertura de links no navegador.** Não há integração real com APIs de assinatura digital certificada. | `feature/documents/src/main/res/values/strings.xml` (strings de assinatura) |

### 4.4 Problemas baixos

| ID | Gravidade | Descrição | Local |
|----|-----------|-----------|-------|
| B1 | Baixo | **Inconsistência de versão:** `app/build.gradle.kts` declara `versionCode = 11` / `0.11.0`, mas o usuário solicitou retorno para `0.10.0`. O CHANGELOG já está em 0.11.0. | `app/build.gradle.kts`, `CHANGELOG.md` |
| B2 | Baixo | **Strings duplicadas ou similares em múltiplos módulos** (ex: "Voltar" aparece em `app`, `feature:patients`, `feature:settings`). Não é um problema funcional, mas dificulta manutenção de traduções futuras. | Vários `strings.xml` |
| B3 | Baixo | **Não há configuração de cache de configuração do Gradle (`configuration cache`).** Builds lentos em CI. | `build.gradle.kts` raiz |

---

## 5. Riscos de perda ou exposição de dados

| Risco | Probabilidade | Impacto | Mitigação atual | Recomendação |
|-------|-------------|---------|-----------------|--------------|
| Leitura do PDF gerado por outros apps | Média | Alto | FileProvider com `grantUriPermissions` temporário | Criptografar o PDF antes de compartilhar, ou oferecer opção de "salvar assinado e criptografado" |
| Imagem de câmera exposta no cache se app for interrompido | Baixa | Médio | Arquivos temporários são excluídos após processamento | Criptografar imediatamente após captura, ou usar `FileProvider` com URI temporária |
| Corrupção do snapshot por falha de escrita | Baixa | Alto | `AtomicFile` + verificação de integridade após gravação | Adicionar backup automático antes de operações destrutivas |
| Perda de chave do Android Keystore (reset do dispositivo) | Baixa | Crítico | Chave gerada no keystore do dispositivo | Exportar backup com senha é a única recuperação; educar o usuário |
| Dados sensíveis em RAM após fechamento do app | Média | Médio | Nenhuma | Limpar ViewModels e dados sensíveis no `onDestroy` |

---

## 6. Recomendação fundamentada sobre persistência

O armazenamento atual (`ClinicStore` + `DemoClinicStore` com arquivo criptografado único) é adequado para um **único profissional em um único dispositivo** com volume moderado (centenas de pacientes, milhares de consultas, anexos < 512 MiB). No entanto, para uso clínico odontológico com crescimento previsível, recomendo:

### Estratégia recomendada (em ordem de prioridade):

1. **Manter o armazenamento atual criptografado** para a versão 0.11.x — não substituir agora, pois não há risco imediato e a migração é complexa.
2. **Adicionar compactação periódica**: ao salvar, se o tamanho do snapshot exceder um limite (ex: 4 MiB), reescrever o arquivo removendo registros excluídos.
3. **Adicionar backup automático local** antes de operações destrutivas (restauração, exclusão de paciente).
4. **Planejar migração para banco de dados criptografado (SQLCipher ou similar)** apenas se o volume de dados justificar (ex: > 10.000 consultas ou anexos > 200 MiB). A estratégia de migração seria:
   - Ler o snapshot atual (versão 10).
   - Inserir registros no novo banco criptografado.
   - Manter o arquivo antigo como fallback por algumas versões.
   - Não excluir dados antigos sem confirmação do usuário.

**Não substitua o armazenamento atual nesta etapa de auditoria.** A migração deve ser planejada como uma etapa separada (11B ou posterior), com testes de regressão completos.

---

## 7. Plano de correções em pequenas tarefas

As tarefas abaixo estão ordenadas por prioridade e podem ser executadas independentemente, sem reestruturação completa:

| Tarefa | Descrição | Estimativa | Bloqueador |
|--------|-----------|------------|------------|
| T1 | Corrigir inconsistência de versão (0.10.0 vs 0.11.0) em `build.gradle.kts` e `CHANGELOG.md` | 15 min | Nenhum |
| T2 | Adicionar limpeza de arquivos temporários de câmera no `onDestroy`/`onStop` do `MainActivity` | 30 min | Nenhum |
| T3 | Adicionar validação de complexidade mínima para senha de backup (pelo menos 1 maiúscula, 1 número, 1 símbolo) | 30 min | Nenhum |
| T4 | Adicionar opção de criptografar o PDF antes de compartilhar (senha opcional) | 2 h | Nenhum |
| T5 | Implementar compactação/vacuum do snapshot quando exceder limite de tamanho | 4 h | Nenhum |
| T6 | Adicionar backup automático antes de restauração e exclusão de paciente | 2 h | Nenhum |
| T7 | Adicionar logs de auditoria de acesso a prontuários (quem, quando, que ação) | 4 h | Requer definição do que logar |
| T8 | Avaliar integração real com certificado ICP-Brasil para assinatura digital (requerimento jurídico) | Indefinido | Requer contato com autoridades certificadoras |

---

## 8. Critérios de aceitação para uso clínico

Antes de considerar o app adequado para uso clínico odontológico, os seguintes critérios devem ser atendidos:

1. **Assinatura digital válida:** Integração com certificação digital ICP-Brasil (ou equivalente reconhecido pelo CFO/COFO) para receitas e atestados, com validação de integridade do documento assinado.
2. **PDFs protegidos:** Os documentos gerados não devem ficar expostos em cache legível por outros apps. Deverão ser compartilhados com criptografia opcional ou apagados do cache após o compartilhamento.
3. **Senha de backup forte:** Validação de complexidade mínima (comprimento + variedade de caracteres).
4. **Testes em dispositivo real:** Execução de `connectedDebugAndroidTest` em pelo menos um dispositivo físico com Android 8.0+ e um com Android 13+ (para testar permissão de notificações).
5. **Revisão de dados sensíveis em memória:** Análise se dados de pacientes permanecem descriptografados na RAM após o app ser colocado em segundo plano.
6. **Documentação de uso clínico:** Guia do usuário explicando limitações (offline, único dispositivo, sem sincronização em nuvem).
7. **Revisão jurídica:** Confirmação de que o app atende aos requisitos da LGPD para dados de saúde no Brasil.

---

## 9. Pendências que exigem validação jurídica ou profissional

Os seguintes itens **não podem** ser decididos apenas por análise técnica e requerem revisão de profissionais da área odontológica e/ou jurídica:

| Item | Por que exige validação externa |
|------|----------------------------------|
| Assinatura digital com certificado ICP-Brasil | A validade jurídica de receitas e atestados assinados digitalmente depende de certificação reconhecida por autoridades competentes (CFO, ITI, Receita Federal). |
| Retenção de dados de pacientes | A legislação brasileira (LGPD e normas do Conselho de Odontologia) pode exigir prazos específicos de retenção e descarte seguro de prontuários. |
| Compartilhamento de PDFs com dados de saúde | Envio de documentos contendo dados de saúde por apps de terceiros (WhatsApp, e-mail, etc.) pode exigir consentimento explícito do paciente e criptografia de ponta a ponta. |
| Uso do app como prontuário eletrônico | O Conselho Regional de Odontologia (CRO) pode ter requisitos específicos para prontuários eletrônicos (assinatura, carimbo, retenção, backup). |

---

## 10. Resumo executivo

O OdontoLivreVal v0.11.0 é um **app funcional e bem estruturado** para uso odontológico offline em um único dispositivo. A criptografia de dados (AES-256-GCM, Android Keystore, PBKDF2) está bem implementada para proteção em repouso. Todos os testes unitários e lint passaram.

**No entanto, para uso clínico real, há lacunas importantes:**
1. A assinatura digital é apenas visual, não criptográfica — não tem validade jurídica como assinatura eletrônica qualificada.
2. PDFs gerados ficam expostos no cache sem criptografia adicional.
3. O armazenamento em arquivo único cresce linearmente e não tem compactação.
4. Não há registro de auditoria de acessos nem validação de complexidade de senha de backup.

**Recomendação:** Corrigir primeiro os itens de segurança (T1–T6), especialmente a assinatura digital com certificado reconhecido e a proteção dos PDFs gerados. Planejar a migração de persistência apenas se o volume de dados justificar. Executar testes em dispositivos reais antes de qualquer uso clínico.

**Não foram feitas modificações no código.** Nenhum commit, push ou release foi realizado. Aguardando autorização para iniciar as correções.

---

**Próximos passos sugeridos:**
1. Revisar este relatório com o usuário.
2. Autorizar correções de baixa complexidade (T1–T4).
3. Definir requisitos de assinatura digital com o CFO/ITI (validação jurídica).
4. Executar testes em dispositivo físico antes de disponibilizar para uso clínico.