# Changelog

## Não lançado

- Contato do desenvolvedor (Valdenor Tavares — valdenor.sa@proton.me) exibido na tela Configurações, clicável para abrir o e-mail do celular, e também na tela Sobre.
- Odontograma com esquema de dentes (arquadas superior e inferior) e simulação de raio-x, substituindo o placeholder vazio.
- Prontuário do paciente (ficha clínica) acessível ao tocar no nome de um paciente na aba Pacientes, com campos de anamnese e observações.

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
