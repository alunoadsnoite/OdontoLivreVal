# OdontoLivreVal — Etapa 01: Fundação Android

## Instrução para o Codex

Você é um engenheiro Android sênior trabalhando no projeto **OdontoLivreVal**, aplicativo Android livre, gratuito, offline-first, inicialmente para **um único cirurgião-dentista por dispositivo**. Implemente somente a fundação técnica descrita aqui, com código real, compilável e testado. Não implemente pacientes, consultas, prontuários, odontogramas ou dados clínicos nesta etapa.

### 1. Inspeção obrigatória
- Examine o diretório atual e preserve arquivos existentes; se houver um projeto Android, avalie antes de alterar.
- Verifique `java -version`, Gradle wrapper, SDK Android, ferramentas de build e versões disponíveis. Não invente versões; selecione versões estáveis compatíveis entre Kotlin, AGP, Compose, Hilt, Room e DataStore. Explique eventuais limitações do ambiente.
- Não execute comandos destrutivos e não publique nem faça push sem autorização.

### 2. Projeto e arquitetura
- Nome exibido: **OdontoLivreVal**.
- Package/namespace inicial: `br.com.odontolivreval` (registrar como provisório e manter consistente).
- Kotlin, Jetpack Compose, Material 3, Navigation Compose, Gradle Kotlin DSL e Version Catalog (`gradle/libs.versions.toml`).
- Min SDK 26, target/compile SDK conforme versão estável instalada e compatível; justificar escolha.
- Projeto multi-módulo **enxuto**: `:app`, `:core:designsystem`, `:core:model`, `:feature:settings`. Não criar módulos vazios só para cumprir arquitetura; criar novas features quando houver implementação.
- MVVM, fluxo unidirecional de estado, interfaces claras, sem overengineering. Hilt somente se sua configuração puder ser validada; caso contrário, preferir injeção manual documentada e evoluir depois.
- Preparar diretórios e convenções para futuras features `patients`, `schedule`, `odontogram`, mas não implementar persistência clínica.

### 3. Telas e navegação
- `MainActivity` e aplicação Compose com navegação entre: **Início, Pacientes, Agenda, Odontograma, Configurações**.
- Início com apresentação do aplicativo e atalhos; telas das três features clínicas exibem claramente **“Em desenvolvimento”**, sem criar cadastros fictícios funcionais.
- Configurações: preferência de aparência **Sistema / Claro / Escuro** persistida com DataStore; tela **Sobre** com nome do aplicativo, licença e versão obtida de configuração de build. Não inventar contatos pessoais; deixar contato configurável em recurso separado.
- Material 3, layout responsivo, fontes escaláveis, descrições acessíveis e suporte a rotação/recriação da Activity.

### 4. Persistência e segurança
- DataStore apenas para preferências não sensíveis, como tema.
- Não armazenar nome, CRO, prontuário, anamnese, fotos, radiografias ou qualquer dado real nesta etapa.
- **Room não criptografa por padrão.** Não criar banco clínico sem estratégia de criptografia de dados em repouso e migrações aprovada. Pode adicionar dependências apenas se forem efetivamente usadas; documentar a decisão de adiar Room para a etapa Pacientes.
- Não solicitar permissão de internet, localização, câmera ou armazenamento sem necessidade; evitar analytics, anúncios, SDKs proprietários, logs sensíveis e serviços de nuvem.
- Não implementar autenticação ou backup ainda; criar `docs/SECURITY_DECISIONS.md` com ameaças, decisões pendentes e requisitos antes de dados clínicos reais.

### 5. Qualidade e distribuição
- Criar `README.md` com propósito, requisitos, build, execução e roadmap; `docs/ARCHITECTURE.md` com limites entre módulos; `.gitignore` Android adequado; `CHANGELOG.md`; `LICENSE` GPL-3.0-or-later (texto integral correto, sem gerar licença abreviada).
- Configurar testes unitários para preferências/estado quando aplicável e teste Compose de navegação ou interface. Não criar testes que aparentem passar sem verificar comportamento.
- Preparar configuração compatível com repositório público e futura submissão ao F-Droid; sem dependência de Play Services.
- Usar strings em `strings.xml` (pt-BR como idioma principal), evitar texto hardcoded nos componentes.

### 6. Verificações obrigatórias
- Executar, quando o SDK estiver disponível: `./gradlew :app:assembleDebug`, `./gradlew test`, `./gradlew lint`.
- Corrigir erros encontrados. Se testes instrumentados precisarem de dispositivo/emulador indisponível, informar explicitamente **não executados**.
- Mostrar árvore de pastas efetivamente criada, versões escolhidas, arquivos relevantes, resultados de comandos, pendências e próximos passos.
- Não declarar compilação, testes ou lint como aprovados se não foram executados.

### Critérios de aceite
1. APK Debug gerado com sucesso (quando houver SDK).
2. Navegação entre cinco telas sem crash.
3. Tema Sistema/Claro/Escuro persistente após fechar e reabrir.
4. Nenhum dado clínico real armazenado.
5. Build, testes e lint com resultados verificáveis; bloqueios descritos.
6. Código aberto organizado e documentação consistente.

### Ordem de execução
1. Inspecionar ambiente e repositório.
2. Relatar decisões de versões e módulos.
3. Implementar projeto, telas, tema e preferências.
4. Adicionar testes e documentação.
5. Executar verificações e corrigir falhas.
6. Entregar resumo técnico com próximos passos. Não fazer push nem publicar sem autorização.
