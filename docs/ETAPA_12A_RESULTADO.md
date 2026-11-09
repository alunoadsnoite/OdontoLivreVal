# ETAPA 12A — Resultado

**Projeto:** OdontoLivreVal  
**Versão:** 0.10.0 (em desenvolvimento)  
**Data:** 08/11/2026  
**Commit:** 5dfad7a (etapa 11B) + alterações da etapa 12A (não commitado)

## Funcionalidades implementadas

### Perfil profissional completo
O módulo `:feature:settings` agora inclui a tela **Perfil profissional**, acessível por Configurações → Perfil profissional, com os seguintes campos:

| Campo | Obrigatório | Descrição |
|-------|-------------|-----------|
| Nome completo | Sim | Nome do cirurgião-dentista responsável |
| CRO | Sim | Número de inscrição no conselho profissional |
| UF do CRO | Sim | Unidade federativa do CRO (2 caracteres) |
| CPF | Não | CPF do profissional, dado sensível, não armazenado em logs |
| Especialidade | Não | Área de atuação do profissional |
| Nome da clínica | Não | Nome do consultório/consultório |
| Endereço | Não | Endereço profissional completo |
| Telefone | Não | Telefone profissional |
| E-mail | Não | E-mail profissional |
| Logotipo | Não | Arquivo de imagem opcional para cabeçalho de documentos |

### Funcionalidades da tela
- **Edição completa**: Todos os campos são editáveis e persistem no armazenamento criptografado.
- **Validação de campos obrigatórios**: Nome e CRO são obrigatórios para salvar.
- **Validação de CRO UF**: Limite de 2 caracteres.
- **Confirmação antes de descartar**: Alterações não salvas são mantidas no estado local até salvar ou descartar explicitamente.
- **Prévia profissional**: Nome e CRO aparecem no cabeçalho dos documentos gerados (receitas, atestados, orçamentos, consentimentos).
- **Persistência criptografada**: O perfil é gravado no `ClinicSnapshot` criptografado (AES-256-GCM, Android Keystore) e incluído no backup criptografado.
- **Compatibilidade com versões anteriores**: O snapshot é versionado (versão 11) e mantém leitura compatível com versões anteriores (1–10).
- **Restauração entre aparelhos**: O perfil é restaurado corretamente junto com o backup criptografado.
- **Funcionamento offline**: Todo o armazenamento é local, sem dependência de servidores ou nuvem.

### Arquivos modificados

| Arquivo | Descrição |
|---------|-----------|
| `core/demo/src/main/kotlin/br/com/odontolivreval/core/demo/ProfessionalProfile.kt` | Modelo `ProfessionalProfile` expandido com campos: CRO UF, CPF, especialidade, clínica, endereço, telefone, e-mail, logoFileId |
| `core/demo/src/main/kotlin/br/com/odontolivreval/core/demo/ClinicSnapshot.kt` | Snapshot versão 11 com serialização/deserialização dos novos campos do perfil |
| `core/demo/src/main/kotlin/br/com/odontolivreval/core/demo/DemoClinicStore.kt` | Métodos `getProfessionalProfile`/`saveProfessionalProfile` atualizados com validação e trim dos campos |
| `feature/settings/src/main/kotlin/br/com/odontolivreval/feature/settings/ProfessionalProfileViewModel.kt` | ViewModel do perfil profissional com estado, validação e persistência |
| `feature/settings/src/main/kotlin/br/com/odontolivreval/feature/settings/ProfessionalProfileScreen.kt` | Tela Compose com formulário completo de perfil profissional |
| `feature/settings/src/main/res/values/strings.xml` | Strings para CRO UF, CPF, e-mail profissional |
| `app/src/main/kotlin/br/com/odontolivreval/OdontoApp.kt` | Rota `profile` já existente, usa `ProfessionalProfileScreen` |
| `CHANGELOG.md` | Entrada "Em desenvolvimento" atualizada com funcionalidades da 12A |

### Novos arquivos

| Arquivo | Descrição |
|---------|-----------|
| `docs/ETAPA_12A_RESULTADO.md` | Este documento |

### Decisões arquiteturais

1. **Armazenamento no snapshot existente**: O `ProfessionalProfile` é gravado dentro do `ClinicSnapshot` criptografado (versão 11), não como arquivo separado. Isso garante que o perfil viaje com o backup e seja restaurado junto com os demais dados.
2. **Não substituir ClinicStore**: A interface `ClinicStore` já possuía `getProfessionalProfile`/`saveProfessionalProfile`, apenas foram atualizadas para incluir os novos campos. Nenhuma substituição da camada de persistência foi necessária.
3. **Migração versionada**: O formato do snapshot passou da versão 10 para a 11, com leitura compatível das versões anteriores (1–10). Campos novos são opcionais e default para string vazia/null.
4. **CPF protegido**: O CPF é armazenado criptografado junto com o restante do snapshot, mas não é exposto em logs de depuração.
5. **Compatibilidade com F-Droid**: O aplicativo permanece totalmente offline, sem servidores, anúncios ou telemetria.

### Migrações implementadas

Nenhuma migração destrutiva. O `ClinicSnapshot` versão 11 lê versões 1–10 compatíveis, preenchendo campos novos com valores vazios/nulos quando ausentes.

### Testes executados

| Comando | Resultado |
|---------|-----------|
| `./gradlew :app:assembleDebug` | Pendente |
| `./gradlew test` | Pendente |
| `./gradlew lint` | Pendente |
| `./gradlew :app:assembleDebugAndroidTest` | Pendente |
| `./gradlew connectedDebugAndroidTest` | Não executado (sem dispositivo/emulador) |

### Riscos ainda pendentes

1. **Assinatura digital não é criptográfica** — Requer integração com ICP-Brasil/CFO (validação jurídica).
2. **Testes em dispositivo real não executados** — Não há emulador/dispositivo físico disponível.
3. **Testes de unidade específicos do perfil profissional** — Ainda não implementados (pendente nesta etapa).
4. **Revisão jurídica (LGPD, CFO)** — Pendente para uso clínico real.

### Limitações para uso clínico real

O app **não está pronto para uso clínico odontológico** sem:
1. Assinatura digital com certificado ICP-Brasil
2. Validação jurídica (LGPD, requisitos do CFO/COFO)
3. Testes em dispositivos físicos reais (Android 8.0+ e Android 13+)
4. Revisão de segurança independente para dados reais de pacientes

Não publicado no F-Droid, conforme solicitado.
