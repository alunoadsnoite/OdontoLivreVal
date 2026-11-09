# Etapa 13B — Planos de Tratamento e Orçamentos Odontológicos

## Resumo

Implementado módulo completo de Planos de Tratamento e Orçamentos Odontológicos, integrado ao cadastro de pacientes, prontuário, odontograma e perfil profissional. O aplicativo continua gratuito, open source, offline-first e destinado a um único cirurgião-dentista por dispositivo.

## Funcionalidades implementadas

### Planos de Tratamento

- **Criar um ou mais planos por paciente**: cada paciente pode ter múltiplos planos de tratamento
- **Título e descrição do plano**: campos obrigatórios para identificação
- **Objetivos clínicos**: campo de texto para registrar objetivos do tratamento
- **Procedimentos individuais**: adição, edição e exclusão de procedimentos
- **Associação a dentes e faces**: cada procedimento pode estar vinculado a um dente (numeração FDI) e superfície dentária
- **Sequência dos procedimentos**: campo de etapa/sequência para ordenar o tratamento
- **Observações clínicas**: campo de texto livre para observações
- **Número estimado de sessões**: campo para planejamento de sessões
- **Datas previstas e realizadas**: controle de cronograma
- **Acompanhamento da evolução**: status de execução por procedimento

### Procedimentos Odontológicos

Cada procedimento possui:
- Identificador único auto-incremental
- Descrição (informada pelo profissional)
- Dente e face (quando aplicável, numeração FDI)
- Quantidade
- Valor unitário (BigDecimal, precisão monetária)
- Valor total (calculado automaticamente)
- Etapa do tratamento (sequência)
- Status de execução (Planejado, Aprovado, Em andamento, Concluído, Cancelado)
- Observações

**Regras de negócio:**
- Não sugerir diagnósticos ou tratamentos automaticamente
- Não considerar procedimento concluído apenas por pagamento
- Permitir reorganizar a sequência dos procedimentos

### Orçamento Odontológico

- Gerar orçamento a partir de um plano de tratamento
- Identificação do consultório e dentista (nome, CRO/UF, contatos)
- Nome e identificação do paciente
- Data de emissão e número do orçamento
- Relação de procedimentos com quantidades e valores
- Subtotais por item
- Descontos (quando aplicáveis)
- Valor total final
- Condições de pagamento
- Prazo de validade da proposta
- Observações e condições acordadas

**Regras de negócio:**
- Valores monetários usam BigDecimal (evita ponto flutuante)
- Formato brasileiro (reais)
- Após emissão (status diferente de Rascunho), o orçamento se torna imutável

### Status do Orçamento

- **Rascunho**: pode ser editado livremente
- **Apresentado**: imutável, apresentado ao paciente
- **Aprovado**: aprovado pelo paciente, com data de aprovação
- **Recusado**: recusado pelo paciente
- **Expirado**: passou do prazo de validade
- **Cancelado**: cancelado

**Regras de negócio:**
- Não considerar juridicamente assinado apenas por status aprovado
- Estrutura preparada para futura integração com módulo de consentimento e assinaturas
- Apenas orçamentos em rascunho podem ser editados

### Histórico e Versionamento

- Alterações permitidas enquanto em rascunho
- Após emissão, preservar versão imutável
- Nova revisão para alterações de valores ou procedimentos
- Registro de identificador, versão, data e situação
- Não sobrescrever silenciosamente propostas anteriores

### Integração com Odontograma

- Selecionar dentes e faces existentes no odontograma ao cadastrar procedimentos
- Preservar numeração FDI (dentição permanente e decídua)
- Não alterar automaticamente estado clínico do odontograma
- Atualização do estado clínico somente mediante registro explícito de atendimento

### PDF A4

- Cabeçalho: nome do consultório, profissional, CRO/UF, contatos
- Identificação: dados do paciente, número e data do orçamento
- Tabela de procedimentos: descrição, dente, quantidade, valor unitário, subtotal
- Resumo financeiro: total bruto, descontos, total final, condições de pagamento
- Rodapé: validade da proposta, observações, espaços para assinaturas
- Quebra de páginas, alinhamento de valores, margens adequadas, suporte a textos longos
- Nenhuma assinatura eletrônica/digital fictícia

### Persistência e Segurança

- Integrado ao armazenamento criptografado existente (snapshot v13)
- Vínculo correto com o paciente
- Identificadores únicos auto-incrementais
- Operações consistentes e recuperáveis
- Histórico de revisões
- Compatibilidade com dados anteriores
- Inclusão no backup criptografado
- Restauração íntegra entre aparelhos
- Proteção contra perda de dados em falhas de gravação
- Não realizar migrações destrutivas

### Interface

- Lista de planos do paciente
- Novo plano de tratamento
- Inclusão e edição de procedimentos
- Acompanhamento da execução
- Orçamento vinculado ao plano
- Pré-visualização do orçamento
- Histórico de revisões
- Design consistente com temas claro e escuro
- Suporte a fontes ampliadas e acessibilidade
- Confirmação antes de descartar alterações não salvas

### Exportação e Compartilhamento

- Exportar planos e orçamentos em PDF pelo seletor de documentos Android
- Compartilhamento pelo Android Sharesheet (WhatsApp, e-mail, etc.)
- Compartilhamento apenas após ação explícita do usuário
- Permissões temporárias de acesso aos arquivos (FileProvider)
- Aviso de que PDFs exportados ficam fora da proteção criptográfica

## Arquivos modificados/criados

### Core (:core:demo)
- `TreatmentPlan.kt` — NOVO: Modelos `ProcedureStatus`, `BudgetStatus`, `TreatmentPlan`, `TreatmentProcedure`, `TreatmentBudget`, `BudgetItem`, metadados e serialização JSON
- `ClinicStore.kt` — 14 novos métodos para planos, procedimentos e orçamentos
- `ClinicSnapshot.kt` — Suporte a planos de tratamento (versão 12 → 13)
- `DemoClinicStore.kt` — Implementação dos métodos de persistência

### Feature treatment (:feature:treatment) — NOVO MÓDULO
- `build.gradle.kts` — Configuração do módulo
- `strings.xml` — Recursos em português
- `TreatmentPlanViewModel.kt` — ViewModel para planos e procedimentos
- `TreatmentBudgetViewModel.kt` — ViewModel para orçamentos
- `TreatmentPlanListScreen.kt` — Tela de lista de planos
- `TreatmentPlanFormScreen.kt` — Tela de formulário de plano
- `TreatmentBudgetListScreen.kt` — Tela de lista de orçamentos
- `TreatmentBudgetFormScreen.kt` — Tela de formulário de orçamento
- `TreatmentBudgetPreviewScreen.kt` — Tela de pré-visualização e compartilhamento PDF

### Feature patients (:feature:patients)
- `PatientRecordScreen.kt` — Botão "Planos de tratamento" no prontuário
- `strings.xml` — String `demo_patient_treatment_plans`
- `FakeClinicStore.kt` (test) — Implementação dos métodos de tratamento para testes

### Feature schedule (:feature:schedule)
- `FakeScheduleStore.kt` (test) — Implementação dos métodos de tratamento para testes

### App
- `OdontoApp.kt` — Rotas de navegação (`treatment_plan_list`, `treatment_plan_form`, `treatment_budget_list`, `treatment_budget_form`, `treatment_budget_preview`)
- `build.gradle.kts` — Dependência do módulo `:feature:treatment`

### Configuração
- `settings.gradle.kts` — Inclusão do módulo `:feature:treatment`

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

- A interface de formulário de procedimentos ainda usa campos simplificados (edição detalhada de procedimento será em tela separada em próxima etapa)
- A integração completa com o módulo de consentimento e assinaturas digitais será implementada em etapa futura
- Não implementada assinatura digital com valor legal (conforme restrição do projeto)
- Testes instrumentados exigem dispositivo/emulador para execução completa

## Restrições cumpridas

- Não gerar diagnósticos ou tratamentos automaticamente ✅
- Não usar dados reais de pacientes nos testes ✅
- Não adicionar servidores, anúncios ou telemetria ✅
- Não remover funcionalidades existentes ✅
- Não executar migrações destrutivas ✅
- Não implementar assinatura digital fictícia ✅
- Manter compatibilidade com F-Droid ✅
- Não fazer push ou publicar releases sem autorização ✅
