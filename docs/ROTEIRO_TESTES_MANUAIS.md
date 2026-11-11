# Roteiro de testes manuais

Use exclusivamente pacientes e conteúdo fictícios. Em cada cenário, anote modelo do aparelho, versão do Android, versão do app e resultado. Não marque um caso como aprovado sem executá-lo.

## Instalação e atualização

1. Instale o APK debug em um Android 8.0 ou superior e registre a versão exibida em **Sobre**.
2. Atualize de uma instalação 0.10.0 (versionCode 10) para 0.13.1 (versionCode 14) sem limpar os dados.
3. Confirme que os registros fictícios, anexos e preferências continuam disponíveis.
4. Reinicie o aparelho e abra o app; confirme início, bloqueio/desbloqueio e agenda.

## Perfil e navegação

1. Em **Configurações → Perfil profissional**, preencha dados fictícios, salve, feche e reabra a tela.
2. Volte a **Documentos**, confira os dados profissionais atualizados e percorra receita, atestado, orçamento e consentimento.
3. Em um paciente fictício, abra atestados, planos, orçamentos, TCLE e assinaturas; retorne a cada tela usando voltar.
4. Repita com rotação, fonte ampliada e tema escuro.

## Prontuário, documentos e assinatura

1. Cadastre um paciente com nome claramente fictício e preencha anamnese, evolução e odontograma.
2. Gere um PDF de documento, salve em destino local e confira conteúdo, paginação, nome/CRO e data.
3. Abra as opções de assinatura. Confirme que o registro eletrônico pede identificação e confirmação explícita; confirme também que a opção criptográfica aparece indisponível.
4. Selecione um PDF fictício para arquivar. Compare o arquivo original e o arquivado byte a byte; anote o SHA-256. Confirme que a interface informa que não houve validação criptográfica nem associação a versão documental.
5. Tente arquivo não PDF, vazio e maior que 10 MiB; o app deve apresentar erro sem mostrar sucesso falso.
6. Verifique que histórico, hashes e cópias continuam após fechar e reabrir o app.

## Anexos e backup

1. Importe uma imagem fictícia, confira orientação EXIF, visualização e remoção.
2. Exporte backup com senha fictícia forte; verifique que senha errada não troca os dados atuais.
3. Restaure o backup e confirme pacientes, anexos, documentos, assinaturas e lembretes.
4. Faça os cenários em atualização e instalação limpa separadamente, registrando limitações de cada aparelho.

## Agenda e notificações

1. Abra o app e confirme que ele não solicita permissão de notificação automaticamente.
2. Na Agenda, toque em **Ativar lembretes** e teste permissão concedida e negada.
3. Crie consultas fictícias com durações sobrepostas e consecutivas; confirme que apenas sobreposição é bloqueada.
4. Crie, remarque e cancele consultas; verifique vínculo de retorno e cancelamento de lembretes antigos.
5. Teste aparelho reiniciado, mudança de fuso horário e economia de bateria.
6. Confira a notificação na tela bloqueada; ela deve ser genérica e não expor dados clínicos.

## Offline, acessibilidade e encerramento

1. Desative a rede e repita cadastro, prontuário, agenda, documento e backup local.
2. Teste TalkBack, navegação por teclado quando disponível, escala de fonte ampliada e contraste nos temas claro/escuro.
3. Remova todos os registros de teste pela interface e confirme que nenhum ficou na base.

## Evidência

Registre para cada caso: aparelho/Android, passo, esperado, observado, resultado e log sem dados pessoais. Não copie nomes, CPF, CRO, anamnese ou documentos reais para o relatório.
