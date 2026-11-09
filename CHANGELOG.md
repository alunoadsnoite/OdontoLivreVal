# Changelog

# Changelog

## 0.10.0 — 2026-11-08

### Perfil profissional
- Perfil profissional com nome, CRO, UF do CRO, CPF, especialidade, clínica, endereço, telefone e e-mail
- Perfil gravado no armazenamento criptografado e incluído no backup
- Dados gravados junto com o restante dos dados de demonstração

### Prontuário
- Ficha completa com nome, nascimento, telefone, queixa, anamnese, observações, diagnóstico e conduta
- Histórico de consultas com evoluções
- Marcação de consulta ativa

### Agenda
- Agenda diária com duração de 1 a 240 minutos e exibição do intervalo
- Prevenção de consultas sobrepostas, com horários consecutivos permitidos
- Retornos vinculados à consulta original e ao mesmo paciente
- Agendamentos antigos preservam o início e recebem até 30 minutos sem criar sobreposição

### Documentos em PDF
- Receita, atestado, orçamento e consentimento com cabeçalho do profissional
- Identificação do paciente e bloco de assinatura
- Compartilhamento via Android (FileProvider)

### Lembretes de consulta
- Lembretes locais (~1 hora antes) para consultas ativas dos próximos 60 dias
- Permissão de notificação no Android 13+
- Reagendamento ao abrir o app, ao voltar do segundo plano e após reiniciar

### Painel no Início
- Próximas consultas dos próximos 30 dias (máx. 6), com data, horário, duração e indicação de retorno

### Backend e persistência
- Formato de dados versionado para 10 (snapshot criptografado)
- Backup criptografado com PBKDF2 (600k iterações)
- Alerta antes de backup
- Arquivo de emergência antes de restaurar backup
- Controle de sincronização de armazenamento
- Armazenamento criptografado AES-256-GCM
- Limite de 10 MiB por anexo e 512 MiB somados

### Documentação
- Documentação, versão e roadmap atualizados

### Em desenvolvimento (próxima versão)

- Backup automático antes de exclusão de paciente e restauração de backup
- Compactação automática do snapshot criptografado quando excede 4 MiB
- Limpeza de dados sensíveis dos ViewModels quando o app vai para segundo plano
- Validação de complexidade de senha de backup (12+ caracteres, maiúscula, número, símbolo)
- Opção de criptografar PDF antes de compartilhar (AES-128-CBC com PBKDF2)
- Perfil profissional expandido: nome, CRO, UF do CRO, CPF, especialidade, clínica, endereço, telefone e e-mail
- Validação de campos obrigatórios (nome e CRO) e formatos nos documentos profissionais
- Receituário odontológico com geração de PDF em A4 e compartilhamento
- Controle de lista de medicamentos (nome, apresentação, concentração, posologia, via de administração, duração, quantidade, observações)

## 0.11.0 — 2026-11-09

### Atestados e declarações odontológicas (Etapa 13A)

- Três tipos de documento: atestado odontológico, declaração de comparecimento, declaração de acompanhamento
- Ciclo de vida: rascunho → finalizado → cancelado (com motivo)
- Nova versão para correções (deriva do documento finalizado)
- Modelos editáveis em português brasileiro
- CID apenas quando autorizado explicitamente pelo paciente
- Não gera diagnóstico ou afastamento automaticamente
- Integração com prontuário: seção "Atestados e declarações"
- PDF A4 com cabeçalho profissional, CRO/UF, assinatura manuscrita, sem assinatura digital fictícia
- Compartilhamento via Android Sharesheet e FileProvider
- Armazenamento criptografado no snapshot (versão 12)
- Inclusão no backup criptografado
