# Changelog

`0.13.0` (versionCode 13) é uma versão de desenvolvimento/candidata. Não foi publicada nem submetida ao F-Droid. Recursos nas etapas 14A–15B têm limitações registradas e não equivalem a prontidão para uso clínico.

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

### Planos de tratamento e orçamentos odontológicos (Etapa 13B)

- Planos de tratamento por paciente com título, descrição, objetivos clínicos, sessões estimadas
- Procedimentos vinculados a dentes e faces (numeração FDI), com valores e etapas
- Status de procedimento: Planejado, Aprovado, Em andamento, Concluído, Cancelado
- Orçamento a partir do plano, com BigDecimal para precisão monetária
- Status do orçamento: Rascunho, Apresentado, Aprovado, Recusado, Expirado, Cancelado
- Versionamento de orçamentos (novas revisões para alterações)
- PDF A4 com tabela de procedimentos, resumo financeiro, condições de pagamento
- Integração com prontuário e odontograma
- Armazenamento criptografado no snapshot (versão 13)
- Compartilhamento via Android Sharesheet e FileProvider

## 0.12.0 — 2026-11-09

### Termos de Consentimento Livre e Esclarecido (Etapa 13C)

- Nove modelos de TCLE: tratamento geral, extração, endodontia, periodontia, restauração, prótese, implante, ortodontia, estética
- Ciclo de vida: rascunho, apresentado, aguardando manifestação, aceito, recusado, revogado, substituído
- Conteúdo completo: paciente, profissional/CRO/UF, procedimento, objetivos, explicação, benefícios, riscos, alternativas, cuidados, limitações, dúvidas, declaração de esclarecimento
- Suporte a responsável legal
- Histórico e versionamento com preservação de versões finalizadas
- PDF A4 com paginação, espaços para assinatura (sem assinatura digital fictícia)
- Compartilhamento via Android Sharesheet e FileProvider
- Armazenamento criptografado no snapshot (versão 14)
- Inclusão no backup criptografado

## 0.13.0 — 2026-11-09 (desenvolvimento; não lançado)

### Assinaturas eletrônicas, digitais e integridade documental (Etapa 14A)

- Registro local de confirmação eletrônica digitada e registro de modalidade em papel; sem captura de traçado manuscrito.
- A opção de assinatura criptográfica está indisponível; não há provedor, certificado ou validação PAdES implementados.
- Hash SHA-256 é exibido para o PDF selecionado como referência; não comprova autoria nem valida assinatura.
- PDF selecionado é copiado sem alterações para o arquivo criptografado do paciente e incluído no backup; ainda não está vinculado a uma versão específica de documento.
- Não declarar assinatura certificada ou validade jurídica automática.
