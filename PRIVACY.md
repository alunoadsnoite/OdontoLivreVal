# Privacidade — OdontoLivreVal

**Versão documentada:** 0.13.1 (desenvolvimento; versionCode 14)
**Atualizado em:** 15/11/2026

O OdontoLivreVal foi desenvolvido para uso local em um único aparelho. Esta explicação descreve o comportamento observado no código desta versão candidata; não é uma declaração de conformidade jurídica nem substitui a avaliação do profissional responsável.

## Dados tratados

O app pode armazenar nomes e contatos de pacientes, dados de anamnese e prontuário, consultas, odontogramas, imagens anexadas, documentos, registros de confirmação, perfil profissional e preferências. As notificações de consulta usam texto genérico e não mostram o nome nem dados clínicos do paciente.

## Onde ficam os dados e como são protegidos

Os registros e anexos ficam em arquivos privados do aplicativo e são protegidos por criptografia autenticada AES-256-GCM. A chave local é protegida pelo Android Keystore. Os arquivos clínicos são excluídos do backup automático do Android. A preferência de aparência fica no armazenamento local do Android.

A chave do Keystore é específica desta instalação. Desinstalar o app ou perder a chave pode tornar os dados locais irrecuperáveis. O app não oferece recuperação remota.

## Backup e restauração

O usuário pode exportar manualmente um backup criptografado com senha. O formato atual usa AES-256-GCM e derivação de chave PBKDF2-HMAC-SHA256. A senha não é guardada pelo aplicativo; sem ela, o backup não pode ser restaurado. Guarde a senha e o arquivo em local seguro. A restauração substitui o conjunto de dados local após confirmação.

## Guarda do prontuário

A exclusão do cadastro pelo app só é permitida quando ele não tem consultas ou outros registros vinculados. O app não calcula prazos de guarda nem executa descarte automático. A Lei 13.787/2018 prevê, em regra, prazo mínimo de 20 anos a partir do último registro para a eliminação de prontuários. A restauração de backup substitui os dados locais. Antes de restaurar, exporte e confira uma cópia atual dos dados que precisam ser preservados.

As evoluções salvas registram o nome e o CRO/UF do perfil profissional, junto com a data e hora do atendimento e o momento do registro. Correções geram uma nova evolução vinculada à anterior. Registros antigos preservam a autoria disponível na época da gravação. A identificação registrada pelo perfil não é assinatura eletrônica nem validação do registro profissional.

## PDFs, arquivos compartilhados e portais externos

Ao salvar um PDF por meio do seletor Android, o arquivo fica no destino escolhido pelo usuário e fora da criptografia do armazenamento interno do app. Ao compartilhar, o app cria uma cópia temporária privada e concede acesso ao destinatário escolhido pelo sistema. Cópias de PDF no cache podem permanecer por até 24 horas antes da limpeza seguinte.

Os atalhos de assinatura abrem sites externos no navegador somente quando acionados pelo usuário. O destino externo pode receber dados que o usuário digitar ou anexar nele; o OdontoLivreVal não controla a política de privacidade desse serviço. Arquivar um PDF no prontuário copia seus bytes para o armazenamento criptografado, mas não valida a assinatura nem a vincula automaticamente a uma versão documental específica.

O prontuário pode exportar um PDF individual de evolução para assinatura externa e uma cópia da ficha e das evoluções. Essa cópia não inclui a íntegra dos anexos. O recibo de entrega é preenchido com o destinatário e a lista de documentos, indicando cópias ou originais. O profissional confere a identidade e autorização do destinatário, registra a data da entrega e obtém as duas assinaturas. Guarde a via assinada no arquivo protegido do paciente.

Radiografias, fotos, receitas e PDFs importados permanecem no prontuário quando alguém tenta removê-los pelo app. Isso reduz a perda acidental durante a validação. A governança de descarte e as correções de anexos ainda precisam de um procedimento próprio.

## Rede e serviços

O manifesto não declara a permissão `INTERNET`. O app não usa conta, servidor, anúncios ou telemetria. A abertura de portais externos depende do navegador e da conexão do aparelho.

## Permissões declaradas

- `CAMERA`: fotografar anexos quando o profissional escolhe essa função.
- `POST_NOTIFICATIONS`: solicitado somente quando o profissional toca em **Ativar lembretes** na Agenda; abrir o app não inicia a solicitação.
- `RECEIVE_BOOT_COMPLETED`: reprogramar lembretes locais após reinicialização.
- `USE_BIOMETRIC`: permitir autenticação biométrica opcional do aparelho para o bloqueio do app.

O app deve continuar utilizável quando uma permissão opcional for recusada, respeitadas as funções que dependem dela.

## Responsabilidade e limites

O profissional é responsável por usar dados apropriados, proteger o aparelho e as senhas de backup, decidir a base legal e os prazos de retenção, atender solicitações aplicáveis dos titulares e avaliar fornecedores externos. A versão 0.13.1 permanece em validação e não declara conformidade integral com a LGPD nem prontidão para uso clínico.
