# Changelog

## 0.10.0 — 2026-11-08

- Perfil profissional com CRO, painel de próximas consultas e lembretes locais.
- Documentos em PDF para receitas, atestados, orçamentos e consentimentos.
- Fluxo de assinatura eletrônica externa, com exportação do PDF sem assinatura, atalhos para GOV.BR, CFO e VALIDAR e arquivo criptografado do PDF retornado.
- Correção na abertura de Documentos e na atualização do perfil ao voltar ao prontuário.
- O PDF do odontograma inclui nome e CRO do perfil quando preenchidos.
- Os lembretes omitem o nome do paciente e conferem o horário vigente antes de notificar.
- Gradle Wrapper usa espelho com SHA-256 oficial e timeout ampliado.
- Duração de 1 a 240 minutos e exibição do intervalo na agenda.
- Prevenção de consultas sobrepostas, com horários consecutivos permitidos.
- Retornos vinculados à consulta original e ao mesmo paciente.
- Agendamentos antigos preservam o início e recebem até 30 minutos sem criar sobreposição.

## 0.9.0 — 2026-11-08

- Radiografias e receitas passam a ser guardadas em arquivos criptografados independentes, com migração automática dos anexos existentes.
- Limites de 10 MiB por anexo e 512 MiB somados, separados do limite do índice do prontuário.
- Backup em formato 3 processa um anexo por vez. A restauração lê os formatos anteriores e valida todos os dados antes da troca.

## 0.8.0 — 2026-11-08

- Odontograma com dentições permanente e decídua na ordem FDI.
- Marcação independente de cárie e restauração por face, além das marcações do dente inteiro.
- Exportação do odontograma com faces e uma página por dentição registrada.
- Preservação das marcações de versões anteriores ao abrir o prontuário.

## 0.7.0 — 2026-11-08

- Marcação Canal, disposição dos dentes na ordem FDI e exportação da evolução salva em PDF.
- Textos do cadastro, da agenda e do prontuário revisados.
- Radiografias anexadas por arquivo ou câmera, com visualização e remoção.
- Arquivo de receitas em PDF/DOCX por paciente, com exportação de cópia.
- Backup criptografado inclui anexos e continua lendo os registros de versões anteriores.

## 0.6.0 — 2026-11-08

- Visão mensal da agenda com contagem de consultas ativas e acesso à lista diária.
- A ação Voltar confirma antes de descartar alterações no prontuário.
- A evolução recém-salva é incorporada ao histórico sem uma segunda leitura do armazenamento.
- CI usa o pacote do SDK Android 37.0 e uma ação de instalação sem a dependência legada `tools`.

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
