# Changelog

## 0.5.0 — 2026-11-07

- Prontuário do paciente com ficha clínica, anamnese, diagnóstico, conduta, observações e histórico de consultas.
- Odontograma editável por dente, com cárie, restauração, ausente/extraído e implante, persistido junto à consulta ativa.
- Nova consulta mantém as evoluções anteriores e permite registrar mais de um atendimento por paciente.
- Identificadores de ficha e consulta são gerados pelo armazenamento, evitando colisões e sobrescrita entre pacientes.
- Ficha e consulta são gravadas em uma única transação, com estado de salvamento e confirmação antes de descartar alterações.
- Data de nascimento e telefone com máscara e validação; formatação de datas no padrão brasileiro.
- Odontograma responsivo com rolagem horizontal, alvos de toque maiores, descrições acessíveis e legenda.
- Agenda: corrigido o atalho “Abrir Pacientes”, ordenação sem acentos e proteção contra paciente ausente.
- Pacientes: exclusão só após confirmação, estado de exclusão e mensagens de erro dentro da lista.
- Backup em formato 2, com versão e iterações do PBKDF2 gravadas no arquivo; leitura de backups 1 preservada.
- Validação mais rígida do formato dos dados e recusa de versões desconhecidas.
- Correção dos erros de compilação do prontuário e do lint.
- Testes unitários dos ViewModels de pacientes, prontuário e agenda; testes instrumentados atualizados.
- Contato do desenvolvedor (Valdenor Tavares — valdenor.sa@proton.me) exibido em Configurações e Sobre.
- Documentação, versão, regras de backup e fluxo de CI atualizados.

## 0.4.0 — 2026-11-07

- Armazenamento criptografado com migração do cadastro e da agenda de teste.
- Bloqueio opcional com a credencial do Android e biometria forte quando disponível.
- Exportação e restauração de backup criptografado com senha própria.

## 0.3.0 — 2026-11-07

- Agenda diária de teste vinculada aos pacientes fictícios.
- Marcação, remarcação e cancelamento de consultas, com proteção contra horários duplicados.
- Atualização do banco de teste preserva os pacientes da 0.2.0.

## 0.2.0 — 2026-11-07

- Cadastro de teste com nome fictício, busca sem distinção de acentos e edição.
- Persistência em um banco SQLite de demonstração no aparelho.
- Ícone do aplicativo com o emoji de dente 🦷.
- Tema claro com fundo branco e detalhes roxos, e tema escuro com detalhes lilás.

## 0.1.0 — 2026-11-07

- Fundação Android com quatro módulos, Kotlin, Compose e Material 3.
- Navegação entre cinco destinos e Sobre; placeholders clínicos explícitos.
- Preferência de aparência persistida em DataStore com estado MVVM.
- Testes locais e teste Compose de navegação/recriação.
- Licença GPL-3.0-or-later, documentação de arquitetura e decisões de segurança.
- Sem dados clínicos, permissões, serviços proprietários, autenticação ou backup.
