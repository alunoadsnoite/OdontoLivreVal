# Etapa 12B — Receituário odontológico e geração de PDF

**Projeto:** OdontoLivreVal  
**Versão:** 0.10.0 (em desenvolvimento)  
**Data:** 2026-11-08

## Funcionalidades implementadas

### DocumentPdfRequest
- Classe de dados que representa uma solicitação de geração de PDF de documento
- Contém: tipo de documento, paciente, ficha, perfil profissional, conteúdo, título e rótulos

### ClinicPdf
- Classe utilitária para geração de PDFs com `PdfDocument`
- Suporta cabeçalho, título, corpo, separadores e assinatura
- Método `write` para gerar PDF de documento genérico
- Método `shareFile` para salvar PDF no cache do aplicativo

### DocumentType
- Enumeração com tipos de documento: PRESCRIPTION, CERTIFICATE, BUDGET, CONSENT
- Cada tipo tem label, título do PDF, template e prefixo de arquivo

### DocumentsScreen
- Tela de documentos com seleção de tipo, edição de conteúdo e geração de PDF
- Integração com o perfil profissional para cabeçalho do documento
- Compartilhamento via FileProvider

### ProfessionalProfileViewModel
- ViewModel para gerenciamento do perfil profissional
- Carregamento, mudança e salvamento do perfil profissional
- Validação de campos obrigatórios (nome e CRO)

### ProfessionalProfileScreen
- Tela de configuração do perfil profissional
- Campos: nome, CRO, UF do CRO, CPF, especialidade, clínica, endereço, telefone, email
- Validação de campos obrigatórios (nome e CRO)
- Integração com armazenamento criptografado

## Decisões arquiteturais

### Armazenamento
- O perfil profissional é armazenado no snapshot criptografado (versão 11) junto com os demais dados da clínica
- Isso garante que o perfil viaje com o backup e seja restaurado junto com os demais dados

### Compatibilidade com versões anteriores
- O snapshot é versionado (versão 11) e mantém leitura compatível com versões anteriores (1–10)
- Campos novos (CRO UF, CPF, especialidade, clínica, endereço, telefone, email, logoFileId) são opcionais e default para string vazia/null

### Compatibilidade com F-Droid
- O aplicativo permanece totalmente offline, sem servidores, anúncios ou telemetria
- Não há dependências proprietárias

## Testes executados

### Testes unitários do ProfessionalProfileViewModel
- `loadsExistingProfile`: Carrega perfil existente do armazenamento
- `emptyProfileWhenNoData`: Perfil vazio quando não há dados
- `savesProfileSuccessfully`: Salva perfil com sucesso e persiste no armazenamento
- `incompleteProfileBlocksSave`: Bloqueia salvamento quando nome e CRO estão vazios
- `clearResetsState`: Limpa estado do ViewModel
- `loadFailureSetsError`: Trata erro de carregamento com exceção

## Testes não executados

- `connectedDebugAndroidTest` não executado (não há emulador ou dispositivo físico disponível)

## Riscos pendentes

1. Assinatura digital não é criptográfica — requer integração com ICP-Brasil/CFO
2. Testes em dispositivo real não executados
3. Não há registro de auditoria (audit log) de acessos a prontuários
4. Não há integração real com APIs de assinatura digital certificada (GOV.BR, CFO, VALIDAR são apenas links)

## Limitações para uso clínico real

- Não publicado no F-Droid, conforme solicitado
- Requer revisão jurídica para uso como prontuário eletrônico legal
- Requer certificação ICP-Brasil para assinatura digital válida
- Requer testes em dispositivos físicos reais
