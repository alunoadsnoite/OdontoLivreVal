# Agenda

A agenda permite marcar, remarcar e cancelar consultas de teste para pacientes fictícios já cadastrados. Ela mostra um dia por vez, ordena as consultas por horário e mantém as canceladas no histórico do dia. O armazenamento impede a sobreposição de consultas ativas. Uma consulta pode começar no minuto em que a anterior termina.

A agenda compartilha o armazenamento criptografado de demonstração com Pacientes e Prontuário. A migração preserva os nomes e consultas das versões anteriores. O botão **Ver ficha do paciente** abre o prontuário, e **Abrir Pacientes** leva ao cadastro quando ainda não há pacientes.

A visão mensal conta consultas ativas e abre a lista diária ao selecionar uma data. A duração é configurável de 1 a 240 minutos, e a consulta deve terminar até a meia-noite. Os agendamentos antigos preservam o início e recebem até 30 minutos, limitados pelo próximo horário ativo do dia ou pela meia-noite. **Marcar retorno** cria uma consulta para o mesmo paciente, vinculada à consulta original, com sugestão de data para sete dias depois ou hoje se essa data já tiver passado. Lembretes e painel inicial permanecem no plano.
