# Etapa 13C — Termos de Consentimento Livre e Esclarecido (TCLE)

## Resumo

Implementado módulo completo de Termos de Consentimento Livre e Esclarecido (TCLE) para elaboração, gerenciamento, armazenamento e exportação de TCLEs relacionados a tratamentos odontológicos. O aplicativo continua gratuito, open source, offline-first e destinado a um cirurgião-dentista por dispositivo.

## Funcionalidades implementadas

### Tipos de documento de consentimento (9 modelos)

1. Tratamento odontológico geral
2. Extração dentária
3. Tratamento endodôntico
4. Tratamento periodontal
5. Procedimentos restauradores
6. Tratamento protético
7. Implantes dentários
8. Tratamento ortodôntico
9. Procedimentos estéticos odontológicos

### Ciclo de vida do consentimento

- **Rascunho**: em edição pelo profissional, pode ser modificado livremente
- **Apresentado**: entregue ao paciente, torna-se imutável
- **Aguardando manifestação**: aguardando resposta do paciente
- **Aceito**: paciente concordou, com comprovação registrada
- **Recusado**: paciente recusou, com comprovação registrada
- **Revogado**: consentimento revogado, quando aplicável
- **Substituído**: substituído por nova versão

### Conteúdo do TCLE

Cada termo permite registrar:
- Identificação do paciente
- Identificação do cirurgião-dentista e CRO/UF
- Procedimento ou tratamento proposto
- Objetivos do tratamento
- Explicação do procedimento
- Benefícios esperados
- Riscos e possíveis complicações
- Alternativas terapêuticas
- Consequências da não realização
- Cuidados necessários
- Limitações e incertezas
- Espaço para dúvidas do paciente
- Declaração de esclarecimento
- Manifestação de concordância ou recusa
- Local e data
- Campos para assinaturas (espaços em branco, sem assinatura digital fictícia)

### Responsável legal

Suporte a responsável legal quando necessário:
- Nome do responsável
- Qualidade ou vínculo de representação
- Identificação
- Data e manifestação

### Histórico e rastreabilidade

- Versões finalizadas permanecem íntegras
- Não há sobrescrita silenciosa
- Criação de revisões para alterações
- Registro de data, versão e situação de cada termo
- Registro de revogações sem apagar o histórico

### PDF A4

- Cabeçalho com nome do consultório, profissional, CRO/UF, contatos
- Identificação do paciente
- Título do TCLE
- Descrição do tratamento, informações, riscos e alternativas
- Declarações de esclarecimento e decisão
- Local e data
- Espaços para assinatura do paciente/representante e do cirurgião-dentista
- Paginação, quebra de textos longos, suporte a caracteres acentuados, margens adequadas, numeração de páginas

### Compartilhamento

- Exportação pelo seletor de documentos Android (FileProvider)
- Compartilhamento pelo Android Sharesheet (WhatsApp, e-mail, etc.)
- Apenas após ação explícita do usuário
- Permissões temporárias de acesso aos arquivos
- Aviso de que documentos exportados ficam fora da proteção criptográfica

### Segurança e persistência

- Integrado ao armazenamento criptografado existente (snapshot v14)
- Associação correta ao paciente
- Identificadores únicos
- Preservação de versões finalizadas
- Compatibilidade com backup criptografado e restauração íntegra

### Regras importantes (conforme requisitos)

- Não considerar o simples toque em um botão como prova de consentimento
- Não implementar assinatura eletrônica ou digital nesta etapa
- Não apresentar termo sem assinatura como juridicamente assinado
- Modelos exigem validação profissional e jurídica antes do uso clínico
- Não gerar riscos, prognósticos ou alternativas automaticamente

## Arquivos modificados/criados

### Core (:core:demo)
- `InformedConsent.kt` — NOVO: Modelos `ConsentType`, `ConsentStatus`, `InformedConsent`, `InformedConsentFile`, `LegalRepresentative`, `ConsentProof`, `StoredInformedConsent`, serialização JSON
- `ClinicStore.kt` — 6 novos métodos para consentimentos
- `ClinicSnapshot.kt` — Suporte a consentimentos (versão 13 → 14)
- `DemoClinicStore.kt` — Implementação dos métodos de persistência

### Feature consent (:feature:consent) — NOVO MÓDULO
- `build.gradle.kts` — Configuração do módulo
- `strings.xml` — Recursos em português
- `ConsentViewModel.kt` — ViewModel para formulário e lista de consentimentos
- `ConsentListScreen.kt` — Tela de lista, revogação, nova versão
- `ConsentFormScreen.kt` — Tela de formulário de edição do TCLE
- `ConsentPreviewScreen.kt` — Tela de pré-visualização e compartilhamento PDF

### Feature patients (:feature:patients)
- `PatientRecordScreen.kt` — Botão "Termos de consentimento" no prontuário
- `strings.xml` — String `demo_patient_consents`
- `FakeClinicStore.kt` (test) — Implementação dos métodos de consentimento para testes

### Feature schedule (:feature:schedule)
- `FakeScheduleStore.kt` (test) — Implementação dos métodos de consentimento para testes

### App
- `OdontoApp.kt` — Rotas de navegação (`consent_list`, `consent_form`, `consent_preview`)
- `build.gradle.kts` — Dependência do módulo `:feature:consent`

### Configuração
- `settings.gradle.kts` — Inclusão do módulo `:feature:consent`

## Testes executados

- `./gradlew test` — ✅ Todos os testes unitários passam
- `./gradlew lint` — ✅ Sem problemas de lint em todos os módulos
- `./gradlew :app:assembleDebugAndroidTest` — ✅ Compilação de testes instrumentados OK
- `./gradlew :app:assembleDebug` — ✅ Build de debug OK

## APK

- Caminho: `/home/valdenor/Downloads/OdontoLivreVal-0.10.0-debug.apk`
- Versão: 0.10.0 (versionCode 10)
- Não publicado no F-Droid

## Limitações e pendências

- Não implementada assinatura eletrônica ou digital com valor legal (conforme restrição do projeto)
- Os espaços para assinatura são em branco no PDF
- Validação jurídica dos modelos de TCLE deve ser feita por profissional habilitado
- Testes instrumentados exigem dispositivo/emulador para execução completa

## Restrições cumpridas

- Não utilizar dados reais de pacientes nos testes ✅
- Não presumir consentimento ✅
- Não gerar informações clínicas automaticamente ✅
- Não afirmar validade jurídica sem fundamento ✅
- Não implementar assinatura digital fictícia ✅
- Não remover funcionalidades existentes ✅
- Não executar migrações destrutivas ✅
- Não adicionar servidores, anúncios ou telemetria ✅
- Manter compatibilidade com F-Droid ✅
- Não fazer push nem publicar releases sem autorização ✅
