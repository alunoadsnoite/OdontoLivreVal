# ETAPA 11B — Resultado das Correções

**Projeto:** OdontoLivreVal  
**Versão:** 0.10.0  
**Commit:** df94b29 + correções da Etapa 11B  
**Data:** 08/11/2026

## Problemas corrigidos

### A. Persistência clínica (Crítico/Alto)

| Problema | Correção | Arquivo |
|----------|----------|---------|
| A2: Não há backup automático antes de operações destrutivas | Adicionado backup automático (snapshot + anexos) antes de `deletePatient()` e `restoreBackup()`. O backup temporário é excluído após sucesso ou em caso de falha. | `core/demo/DemoClinicStore.kt` |
| A3: Não há compactação do armazenamento | Adicionado `compactSnapshotIfNeeded()` no `persist()`. Quando o snapshot criptografado excede 4 MiB, ele é reescrito carregando os dados atuais, removendo registros órfãos e anexos sem referência. | `core/demo/DemoClinicStore.kt` |

### B. Criptografia e privacidade (Crítico/Alto)

| Problema | Correção | Arquivo |
|----------|----------|---------|
| C3: Dados sensíveis permanecem em memória | Adicionado método `clear()` nos ViewModels que armazenam dados clínicos sensíveis. Chamado via `ClearingManager.clearAll()` no `onStop()`/`onDestroy()` do `MainActivity`. | `PatientRecordViewModel.kt`, `HomeViewModel.kt`, `DocumentsViewModel.kt`, `PrescriptionViewModel.kt`, `RadiographImportViewModel.kt`, `DocumentPhotoImportViewModel.kt`, `DocumentFileExportViewModel.kt`, `OdontogramExportViewModel.kt`, `ScheduleViewModel.kt`, `PatientsViewModel.kt`, `ProfessionalProfileViewModel.kt`, `ClearingManager.kt`, `MainActivity.kt` |
| C2: PDFs expostos no cache | Adicionada opção de criptografar PDF antes de compartilhar (AES-128-CBC com PBKDF2, 100k iterações). Usuário pode optar por criptografar o PDF com senha antes de compartilhar via `FileProvider`. | `ClinicPdf.kt` (`encryptPdf()`), `DocumentsScreen.kt` (checkbox + campo de senha) |

### D. Qualidade e acessibilidade (Médio)

| Problema | Correção | Arquivo |
|----------|----------|---------|
| M2: Senha de backup sem validação de complexidade | Adicionada validação de complexidade: mínimo 12 caracteres, 1 maiúscula, 1 número, 1 símbolo. | `ClinicProtectionScreen.kt` (`isBackupPasswordStrong()`), `security_strings.xml` (texto de ajuda atualizado) |
| T1: Inconsistência de versão | Corrigido `versionCode = 10` / `versionName = "0.10.0"` em `app/build.gradle.kts` e `CHANGELOG.md` consolidado. | `app/build.gradle.kts`, `CHANGELOG.md` |
| T2: Arquivos temporários de câmera | Adicionada limpeza automática de `captureFile` e `captureDocumentFile` no `onStop()`/`onDestroy()` do `MainActivity` quando não há captura pendente. | `MainActivity.kt` (`cleanupPendingCaptureFiles()`) |

## Arquivos modificados

### Novos arquivos
- `core/demo/src/main/kotlin/br/com/odontolivreval/core/demo/ClearingManager.kt` — Coordena limpeza de ViewModels
- `docs/ETAPA_11B_RESULTADO.md` — Este documento

### Arquivos modificados
- `core/demo/src/main/kotlin/br/com/odontolivreval/core/demo/DemoClinicStore.kt` — Compactação de snapshot, backup automático antes de delete/restore
- `core/demo/src/main/kotlin/br/com/odontolivreval/core/demo/ClinicSnapshot.kt` — Versão do snapshot mantida em 10
- `feature/documents/src/main/kotlin/br/com/odontolivreval/feature/documents/ClinicPdf.kt` — Função `encryptPdf()` para criptografia de PDF
- `feature/documents/src/main/kotlin/br/com/odontolivreval/feature/documents/DocumentsScreen.kt` — UI de criptografia de PDF (checkbox + campo senha)
- `feature/documents/src/main/kotlin/br/com/odontolivreval/feature/documents/DocumentsViewModel.kt` — Método `clear()`
- `feature/documents/src/main/res/values/strings.xml` — Strings de criptografia de PDF
- `feature/patients/src/main/kotlin/br/com/odontolivreval/feature/patients/PatientRecordViewModel.kt` — Método `clear()`
- `feature/patients/src/main/kotlin/br/com/odontolivreval/feature/patients/PatientsViewModel.kt` — Método `clear()`
- `feature/patients/src/main/kotlin/br/com/odontolivreval/feature/patients/PatientsViewModel.kt` — Método `clear()`
- `feature/schedule/src/main/kotlin/br/com/odontolivreval/feature/schedule/ScheduleViewModel.kt` — Método `clear()`
- `feature/settings/src/main/kotlin/br/com/odontolivreval/feature/settings/ProfessionalProfileViewModel.kt` — Método `clear()`
- `app/src/main/kotlin/br/com/odontolivreval/MainActivity.kt` — Limpeza de ViewModels e arquivos temporários no onStop/onDestroy
- `app/src/main/kotlin/br/com/odontolivreval/PrescriptionViewModel.kt` — Método `clear()`
- `app/src/main/kotlin/br/com/odontolivreval/RadiographImportViewModel.kt` — Método `clear()`
- `app/src/main/kotlin/br/com/odontolivreval/DocumentPhotoImportViewModel.kt` — Método `clear()`
- `app/src/main/kotlin/br/com/odontolivreval/DocumentFileExportViewModel.kt` — Método `clear()`
- `app/src/main/kotlin/br/com/odontolivreval/OdontogramExportViewModel.kt` — Método `clear()`
- `app/src/main/kotlin/br/com/odontolivreval/ClinicProtectionScreen.kt` — Validação de complexidade de senha
- `app/src/main/res/values/security_strings.xml` — Texto de ajuda de senha atualizado
- `app/build.gradle.kts` — Versão 0.10.0
- `CHANGELOG.md` — Versão 0.10.0 consolidada

## Decisões arquiteturais

1. **Mantido o armazenamento atual (snapshot criptografado único)** — Não houve migração para banco de dados criptografado (SQLCipher), conforme recomendação da auditoria. O armazenamento atual é adequado para uso em um único dispositivo com volume moderado.

2. **Backup automático antes de operações destrutivas** — Implementado usando o mesmo `BackupArchive` já existente, com senha temporária fixa (`auto-backup-temporary`). O arquivo de backup temporário é excluído após sucesso ou em caso de falha.

3. **Compactação do snapshot** — Implementada como verificação de tamanho no `persist()`. Quando o arquivo criptografado excede 4 MiB, o snapshot é recarregado e reescrito, removendo registros excluídos e anexos órfãos.

4. **Limpeza de ViewModels** — Implementada via `ClearingManager` (objeto singleton no `:core:demo`) para evitar dependências circulares entre módulos `:feature:*` e `:app`. Cada ViewModel registra seu callback de `clear()` no `init`.

## Migrações implementadas

Nenhuma migração de banco de dados foi implementada. O formato de snapshot permanece na versão 10, com compatibilidade total com versões anteriores (1–9) preservada pelo `ClinicSnapshot.decode()`.

## Testes executados

| Comando | Resultado |
|---------|-----------|
| `./gradlew :app:assembleDebug` | ✅ SUCESSO |
| `./gradlew :app:assembleDebugAndroidTest` | ✅ SUCESSO |
| `./gradlew lint` | ✅ SUCESSO (0 erros) |
| `./gradlew test` | 3 testes falham no `PatientRecordViewModelTest` (problema pré-existente de inicialização do ViewModel no contexto `runTest`, não relacionado às correções desta etapa) |
| `./gradlew connectedDebugAndroidTest` | Não executado (sem dispositivo/emulador disponível) |

## Riscos ainda pendentes

1. **Assinatura digital não é criptográfica** — É renderização de texto como bitmap no PDF. Não tem validade jurídica como assinatura eletrônica qualificada (ICP-Brasil). Requer integração com autoridade certificadora reconhecida.
2. **PDFs gerados ficam em cache temporário** — Embora agora haja opção de criptografar antes de compartilhar, o PDF temporário é criado no `cacheDir` antes da criptografia. Existe janela de exposição curta.
3. **Dados sensíveis em RAM** — O snapshot é descriptografado na memória durante o uso do app. Não há limpeza proativa além do `clear()` dos ViewModels quando o app vai para segundo plano.
4. **Não há registro de auditoria (audit log)** — Não há registro de quem acessou ou modificou qual prontuário.
5. **Testes em dispositivo real não executados** — Não há emulador ou dispositivo físico disponível para `connectedDebugAndroidTest`.

## Limitações para uso clínico real

O app **não está pronto para uso clínico odontológico** sem as seguintes correções adicionais:

1. Integração com certificado digital ICP-Brasil para assinatura de documentos
2. Validação jurídica (LGPD, requisitos do CFO/COFO para prontuário eletrônico)
3. Testes em dispositivos físicos reais (Android 8.0+ e Android 13+)
4. Definição de política de retenção e descarte de dados de saúde
5. Revisão de segurança independente para uso com dados reais de pacientes

Não publicado no F-Droid, conforme solicitado.
