# Etapa 14A — Assinaturas Eletrônicas, Digitais e Integridade Documental

## Resumo

Implementado módulo completo de assinaturas eletrônicas, digitais e integridade documental para o OdontoLivreVal. O módulo permite criar, armazenar, visualizar, exportar e compartilhar assinaturas de documentos odontológicos, com foco em rastreabilidade, privacidade e conformidade com as diretrizes do Conselho Federal de Odontologia (CFO) e legislação brasileira aplicável.

## Funcionalidades implementadas

### Modalidades de assinatura (3 tipos)

1. **Assinatura manuscrita em documento impresso**: O PDF é gerado com campos em branco para assinatura manual posterior pelo paciente/profissional.
2. **Assinatura manuscrita capturada na tela**: Permite capturar a assinatura via toque/tela. A imagem capturada é uma representação visual apenas, **não** constitui assinatura digital ICP-Brasil nem prova automática de identidade.
3. **Assinatura digital criptográfica**: Interface preparada para integração futura com provedores ou certificados compatíveis com requisitos aplicáveis. Não cria certificados fictícios, não simula carimbos de certificação e não afirma validade ICP-Brasil sem verificação.

### Identificação do signatário

Cada assinatura registra:
- Identificador do documento
- Versão exata do documento no momento da assinatura
- Papel do signatário: dentista, paciente ou representante legal
- Nome informado
- Data e hora do registro
- Modalidade de assinatura
- Estado da assinatura (pendente, assinado, revogado)
- Método de identificação, quando aplicável
- Metadados técnicos estritamente necessários

### Fluxo de assinatura

1. Seleção do documento
2. Pré-visualização integral do documento
3. Identificação do signatário (nome, papel, documento)
4. Escolha da modalidade disponível
5. Confirmação explícita do signatário
6. Registro da operação
7. Visualização do resultado

### Integridade documental

Implementado cálculo de hash SHA-256 para documentos finalizados, com registro de:
- Identificador do documento
- Versão
- Hash
- Data do cálculo
- Estado de integridade (OK, ALTERADO, DESCONHECIDO)

Disponibilizada verificação local de integridade. Distingue claramente:
- Hash: permite detectar alterações em relação a um valor de referência confiável
- Assinatura desenhada: representação visual da assinatura
- Assinatura digital criptográfica: requer chave, certificado e validação apropriados

Um hash isolado **não** comprova autoria, identidade nem validade jurídica.

### Assinaturas no PDF

- Inserção de representação visual da assinatura manuscrita capturada no PDF, quando autorizada
- Preservação da distinção entre assinatura visual e assinatura digital criptográfica
- Não apresenta imagem como assinatura digital certificada

### Tratamento de documentos assinados

- Preservação da versão exata do documento após assinatura
- Impedimento de modificações silenciosas
- Permissão de verificação de integridade
- Registro de substituições por novas versões
- Preservação de histórico de operações
- Não apagar assinaturas anteriores sem justificativa

### Segurança

- Integração ao armazenamento criptografado existente (snapshot v15)
- Não armazenar chaves privadas de certificados em texto puro
- Não exportar chaves privadas sem necessidade e proteção adequada
- Não criar infraestrutura criptográfica própria quando houver bibliotecas consolidadas
- Não armazenar dados clínicos em logs

### Compartilhamento e exportação

- Exportação pelo seletor de documentos Android (FileProvider)
- Compartilhamento pelo Android Sharesheet (WhatsApp, e-mail, etc.)
- Compartilhamento apenas após ação explícita do usuário
- Permissões temporárias de acesso aos arquivos
- Informar que documentos exportados podem ficar fora da proteção criptográfica

### Importação de documentos assinados externamente

- Permitir importar um PDF assinado externamente e associá-lo ao documento e paciente corretos
- Preservar o arquivo original, sem regravar seu conteúdo
- Registrar sua origem como arquivo importado
- Não declarar que a assinatura foi validada sem efetuar uma verificação criptográfica real

### Interface e acessibilidade

- Tela de opções de assinatura
- Captura de assinatura manuscrita
- Pré-visualização do documento
- Histórico de assinaturas
- Verificação de integridade
- Importação de PDF assinado externamente
- Suporte a fontes ampliadas, leitores de tela e diferentes tamanhos de aparelho

## Arquivos modificados/criados

### Core (:core:demo)
- `DocumentSignature.kt` — NOVO: Modelos `SignatureModality`, `SignatureStatus`, `SignerIdentification`, `SignatureProof`, `DocumentSignature`, `DocumentSignatureFile`, `StoredDocumentSignature`, `DocumentHash`, `DocumentHashFile`, `StoredDocumentHash`, função `computeSha256()`, serialização JSON
- `ClinicStore.kt` — 8 novos métodos para assinaturas e hashes
- `ClinicSnapshot.kt` — Suporte a assinaturas e hashes (versão 14 → 15)
- `DemoClinicStore.kt` — Implementação dos métodos de persistência

### Feature signature (:feature:signature) — NOVO MÓDULO
- `build.gradle.kts` — Configuração do módulo
- `strings.xml` — Recursos em português
- `SignatureViewModel.kt` — ViewModel para assinaturas e integridade
- `SignatureOptionsScreen.kt` — Tela de opções de assinatura
- `SignatureCaptureScreen.kt` — Tela de captura de assinatura manuscrita
- `SignaturePreviewScreen.kt` — Tela de pré-visualização e compartilhamento PDF
- `SignatureListScreen.kt` — Tela de histórico de assinaturas
- `IntegrityVerificationScreen.kt` — Tela de verificação de integridade via hash
- `SignatureImportScreen.kt` — Tela de importação de PDF assinado externamente

### Feature patients (:feature:patients)
- `PatientRecordScreen.kt` — Botão "Assinaturas" no prontuário
- `strings.xml` — String `demo_patient_signatures`
- `FakeClinicStore.kt` (test) — Implementação dos métodos de assinatura para testes

### Feature schedule (:feature:schedule)
- `FakeScheduleStore.kt` (test) — Implementação dos métodos de assinatura para testes

### App
- `OdontoApp.kt` — Rotas de navegação (`signature_options`, `signature_capture`, `signature_preview`, `signature_import`)
- `build.gradle.kts` — Dependência do módulo `:feature:signature`

### Configuração
- `settings.gradle.kts` — Inclusão do módulo `:feature:signature`

## Testes executados

| Tarefa | Status |
|--------|--------|
| `./gradlew test` | ✅ Todos os testes unitários passam |
| `./gradlew lint` | ✅ Sem problemas de lint em todos os módulos |
| `./gradlew :app:assembleDebug` | ✅ Build de debug OK |
| `./gradlew :app:assembleDebugAndroidTest` | ✅ Compilação de testes instrumentados OK |

## APK

- Caminho: `/home/valdenor/Downloads/OdontoLivreVal-0.10.0-debug.apk`
- Versão: 0.10.0 (versionCode 10)
- Não publicado no F-Droid

## Limitações jurídicas e profissionais

- Não implementada assinatura digital com valor legal ICP-Brasil
- A assinatura manuscrita capturada na tela é apenas uma representação visual
- Um hash isolado não comprova autoria, identidade nem validade jurídica
- Modelos de TCLE exigem validação profissional e jurídica antes do uso clínico
- Não armazenar credenciais governamentais no aplicativo
- Integração com serviços oficiais (ICP-Brasil, CFO, ITI) não implementada nesta etapa

## Pendências para assinatura eletrônica/digital

- Integração com serviços oficiais de assinatura digital (ICP-Brasil, CFO, ITI)
- Implementação de assinatura digital criptográfica efetiva com chave e certificado
- Suporte a PAdES (PDF Advanced Electronic Signatures) quando exigido e tecnicamente suportado
- Validação de assinaturas digitais externas com cadeia de confiança

## Restrições cumpridas

- Não simular certificação ICP-Brasil ✅
- Não afirmar validade jurídica sem fundamentação ✅
- Não armazenar credenciais externas ✅
- Não usar dados reais de pacientes nos testes ✅
- Não executar migrações destrutivas ✅
- Não comprometer backups anteriores ✅
- Não adicionar anúncios ou telemetria ✅
- Manter compatibilidade com F-Drix ✅
- Não fazer push nem publicar releases sem autorização ✅

## Critérios de conclusão

✅ Os documentos podem ser preparados para assinatura manuscrita
✅ A captura de assinatura visual está corretamente identificada como representação visual
✅ Os documentos finalizados têm verificação local de integridade
✅ As assinaturas estão vinculadas às versões corretas
✅ PDFs assinados externamente podem ser importados sem alteração
✅ O histórico documental está preservado
✅ O aplicativo não apresenta modalidades de assinatura como juridicamente válidas sem comprovação
