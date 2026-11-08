# Arquitetura

## Limites

- `:app`: Application, ponto de composição da injeção manual, Activity, grafo de navegação, Início, Sobre e proteção da janela. Depende dos módulos de núcleo e de funcionalidades.
- `:core:model`: enum ThemeMode e mapeamento das chaves estáveis. Sem Compose ou persistência.
- `:core:designsystem`: tema Material 3; depende apenas de core:model e Compose.
- `:feature:settings`: contrato ThemeRepository, implementação DataStore, SettingsViewModel, estado e tela. Depende de core:model.
- `:core:demo`: contrato `ClinicStore` e implementação `DemoClinicStore`; arquivo criptografado de pacientes, consultas, fichas e evoluções. Também migra os esquemas SQLite 1 e 2, serializa acessos, versiona o backup e grava o arquivo atomicamente fora da thread principal.
- `:feature:patients`: cadastro de pacientes, ficha clínica, evoluções e odontograma por dente. Depende de `:core:demo` e não conhece a agenda.
- `:feature:schedule`: agenda diária, estado e tela. Depende de `:core:demo`.

A interface de `ClinicStore` permite testar os ViewModels sem Android e mantém explícito o contrato entre as features e a persistência. As telas recebem `ClinicStore`, e o app injeta `DemoClinicStore` manualmente; Hilt continua adiado por não trazer benefício nesta etapa.

## Fluxo de estado

Fluxo unidirecional: toque → função do ViewModel → `ClinicStore` → `StateFlow` → Compose. O estado inicial sem tema impede aplicar um tema incorreto antes da primeira leitura. IOException de leitura usa Sistema; falha ao salvar conserva a seleção persistida e exibe mensagem. Corrupção não é apagada silenciosamente. Um DataStore por arquivo e processo é criado pelo delegate do contexto de aplicação. ViewModel sobrevive à recriação; o NavController salva o back stack e o paciente selecionado é salvo com `rememberSaveable`.

## Prontuário e odontograma

A ficha permanente (`PatientRecord`) guarda nascimento, telefone, queixa, anamnese e observações. Cada evolução (`VisitRecord`) guarda data, horário, queixa, anamnese, diagnóstico, conduta, observações e o mapa do odontograma (`número do dente → código`). A consulta mais recente é carregada como rascunho ativo; **Nova consulta** inicia uma evolução em branco. Ficha e consulta são gravadas por `saveRecordAndVisit` em uma única chamada ao armazenamento.

O odontograma usa o esquema FDI permanente (11–18, 21–28, 31–38, 41–48). A UI é declarativa: o mapa vem do estado do ViewModel e cada toque devolve um mapa novo. O componente não guarda cópia local do mapa, evitando divergência após carregar outra consulta. Dentição decídua e marcação por superfície ficam para uma etapa com módulo `:feature:odontogram` próprio.

## Persistência e segurança

A persistência de demonstração é um único arquivo JSON criptografado com AES-256-GCM. Não use o arquivo de demonstração para dados clínicos reais: o volume cresce com o prontuário e não há migração de banco. Room foi adiado até a definição do banco criptografado e das migrações. O backup usa PBKDF2-HMAC-SHA256, sal aleatório e 600 mil iterações; o formato 2 grava a versão e as iterações no cabeçalho, e a leitura do formato 1 continua disponível. Consulte [SECURITY_DECISIONS.md](SECURITY_DECISIONS.md).

## Convenções

Strings em recursos, UI sem textos hardcoded, preferências com chaves estáveis. Version Catalog centraliza versões. Namespace provisório precisa ser confirmado antes de distribuir. Backup automático e transferência do Android estão desabilitados no manifesto. A CI executa build, testes unitários e lint; testes instrumentados exigem dispositivo ou emulador.
