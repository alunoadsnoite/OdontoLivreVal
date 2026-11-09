# Etapa 13A — Atestados e declarações odontológicas

## Resumo

Implementado módulo completo para criação, armazenamento, visualização, exportação e compartilhamento de atestados e declarações odontológicas, vinculados ao prontuário do paciente.

## Funcionalidades implementadas

### Tipos de documento (3 tipos)

1. **Atestado odontológico** (`CertificateType.MEDICAL_CERTIFICATE`)
   - Identificação do paciente e do cirurgião-dentista com CRO/UF
   - Data do atendimento/avaliação
   - Período de afastamento/repouso em dias (opcional, apenas quando clinicamente indicado)
   - Texto descritivo editável pelo profissional
   - Local e data de emissão
   - Espaço para assinatura manuscrita no PDF

2. **Declaração de comparecimento** (`CertificateType.ATTENDANCE_DECLARATION`)
   - Identificação do paciente
   - Data de comparecimento
   - Horário de entrada e saída (opcional)
   - Identificação profissional
   - Texto padronizado, mas editável

3. **Declaração de acompanhamento** (`CertificateType.COMPANION_DECLARATION`)
   - Identificação do acompanhante (quando necessária)
   - Identificação do paciente (quando apropriado e autorizado)
   - Data e período de acompanhamento
   - Identificação do profissional

### Ciclo de vida do documento

- **Rascunho (DRAFT)**: pode ser criado, editado e salvo múltiplas vezes
- **Finalizado (FINALIZED)**: permanece íntegro, não pode ser sobrescrito silenciosamente
- **Cancelado (CANCELLED)**: requer motivo registrado, fica visível no histórico

### Operações suportadas

- Criar novo documento a partir de modelos editáveis em português
- Salvar rascunho
- Editar rascunho existente
- Pré-visualizar antes de finalizar
- Finalizar documento (gera versão imutável)
- Consultar histórico de documentos por paciente
- Cancelar documento com registro do motivo
- Criar nova versão quando houver correção (deriva do documento finalizado)

### Requisitos de privacidade e CFO

- ✅ Não inclui diagnóstico nem CID automaticamente
- ✅ CID só quando juridicamente apropriado, necessário e autorizado pelo paciente (campo `cidAuthorized`)
- ✅ Declaração de comparecimento não permite CID
- ✅ Diferencia claramente atestado de afastamento de declaração de comparecimento
- ✅ Não afirma validade de assinatura eletrônica/digital inexistente
- ✅ Informa que documentos exportados ficam fora da proteção criptográfica do app

### Integração com prontuário

- Nova seção "Atestados e declarações" no prontuário do paciente
- Botão de acesso na tela da ficha do paciente (ao lado de "Documentos")
- Navegação: Prontuário → Atestados e declarações → Lista / Formulário / Pré-visualização

### PDF A4

- Cabeçalho profissional com nome, CRO/UF, especialidade, clínica, endereço, telefone
- Identificação do paciente (nome, nascimento, telefone)
- Título do documento
- Corpo do texto editável
- Data e local de emissão
- Espaço para assinatura manuscrita
- Rodapé profissional
- Suporte a caracteres acentuados, quebra de linhas, margens e paginação
- Nenhuma assinatura digital fictícia

### Segurança e persistência

- Armazenamento criptografado no snapshot da clínica (versão 11 → 12)
- Associação correta ao paciente
- Identificadores únicos auto-incrementais
- Documentos finalizados preservados (não sobrescritos)
- Compatível com backup e restauração criptografados
- Arquivos temporários tratados com segurança

### Compartilhamento

- Exportação pelo seletor de documentos Android (FileProvider)
- Compartilhamento via Android Sharesheet (WhatsApp, e-mail, etc.)
- Apenas após ação explícita do usuário
- Permissões temporárias de acesso aos arquivos (FLAG_GRANT_READ_URI_PERMISSION)

## Arquivos modificados/criados

### Core (:core:demo)
- `MedicalCertificate.kt` — NOVO: Modelos `CertificateType`, `CertificateStatus`, `MedicalCertificate`, `MedicalCertificateFile`, `StoredMedicalCertificate`
- `ClinicStore.kt` — 6 novos métodos de persistência de certificados
- `ClinicSnapshot.kt` — Suporte a certificados no snapshot (versão 12)
- `DemoClinicStore.kt` — Implementação dos métodos de certificados

### Feature documents (:feature:documents)
- `CertificateViewModel.kt` — NOVO: ViewModel para formulário e lista de certificados
- `CertificateListScreen.kt` — NOVO: Tela de lista, cancelamento, nova versão
- `CertificateFormScreen.kt` — NOVO: Tela de formulário de criação/edição
- `CertificatePreviewScreen.kt` — NOVO: Tela de pré-visualização e compartilhamento PDF
- `strings.xml` — Strings em português para certificados

### Feature patients (:feature:patients)
- `PatientRecordScreen.kt` — Botão "Atestados e declarações" no prontuário
- `strings.xml` — String `demo_patient_certificates`

### Feature schedule (:feature:schedule)
- `FakeScheduleStore.kt` — Implementação dos métodos de certificados para testes

### App
- `OdontoApp.kt` — Rotas de navegação para certificados (`certificates_list`, `certificates_form`, `certificates_preview`)

### Testes (fake stores)
- `FakeClinicStore.kt` (patients) — Implementação dos métodos de certificados
- `FakeScheduleStore.kt` (schedule) — Implementação dos métodos de certificados

## Testes executados

- `./gradlew test` — ✅ Todos os testes unitários passam
- `./gradlew lint` — ✅ Sem problemas de lint
- `./gradlew :app:assembleDebugAndroidTest` — ✅ Compilação de testes instrumentados OK
- `./gradlew :app:assembleDebug` — ✅ Build de release de debug OK

## APK

- Caminho: `/home/valdenor/Downloads/OdontoLivreVal-0.10.0-debug.apk`
- Versão: 0.10.0 (versionCode 10)
- Não publicado no F-Droid

## Pendências regulatórias

- Requisitos do Conselho Federal de Odontologia (CFO) sobre atestados e declarações devem ser confirmados por profissional legalmente habilitado
- Validade jurídica de documentos gerados depende do contexto de uso e assinatura em sistemas próprios (portal ITI/CFO)
- Não implementada assinatura digital com valor legal (conforme restrição do projeto)
