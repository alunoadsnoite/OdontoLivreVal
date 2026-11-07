# Arquitetura

## Limites

- `:app`: Application, ponto de composição da injeção manual, Activity, grafo de navegação, Início, placeholders e Sobre. Depende dos outros três módulos.
- `:core:model`: enum ThemeMode e mapeamento das chaves estáveis. Sem Compose ou persistência.
- `:core:designsystem`: tema Material 3; depende apenas de core:model e Compose.
- `:feature:settings`: contrato ThemeRepository, implementação DataStore, SettingsViewModel, estado e tela. Depende de core:model; não conhece app ou features clínicas.

Fluxo unidirecional: toque → selectTheme → repositório → DataStore Flow → SettingsState → Compose. O estado inicial sem tema impede aplicar um tema incorreto antes da primeira leitura. IOException de leitura usa Sistema; falha ao salvar conserva a seleção persistida e exibe mensagem. Corrupção não é apagada silenciosamente. Um DataStore por arquivo e processo é criado pelo delegate do contexto de aplicação. ViewModel sobrevive à recriação; o NavController salva o back stack.

Injeção manual é suficiente: Application mantém o repositório, uma Factory cria o ViewModel. Hilt foi adiado para evitar plugins e geração de código sem benefício nesta etapa. Room foi adiado: nenhuma entidade clínica ou banco foi criado. Não existem módulos vazios.

## Convenções futuras

Os diretórios `docs/features/patients`, `schedule` e `odontogram` registram os limites das próximas etapas. Quando houver implementação, criar `:feature:patients`, `:feature:schedule` e `:feature:odontogram`, com packages `br.com.odontolivreval.feature.<nome>`, tela e ViewModel na feature. Dados compartilhados só recebem módulo próprio quando necessários. Não mover dados clínicos para DataStore.

Strings em recursos, UI sem textos hardcoded, preferências com chaves estáveis. Version Catalog centraliza versões; AGP controla sua versão integrada de Kotlin. Namespace provisório precisa ser confirmado antes de distribuir. Não há configuração de backup ou autenticação; backup está desabilitado no manifesto.
