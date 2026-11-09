# Auditoria final — Etapa 15A

**Data:** 09/11/2026  
**Versão de trabalho:** 0.13.0 (versionCode 13)  
**Escopo:** revisão de inicialização, navegação, perfil profissional, documentos, assinatura, dados locais, privacidade e preparação para validação.

## Correções realizadas

- Ajustada a ordem de inicialização do estado em Home e Documentos, evitando carregar antes da criação dos estados usados pelo ViewModel.
- Preservada a seleção do paciente durante a navegação interna do prontuário, evitando telas que mostravam somente Voltar.
- Corrigida a factory de `ProfessionalProfileViewModel`, que falhava sem registrar o initializer; o acesso ao perfil ficou visível em Configurações.
- Corrigidos fluxos de inicialização e retorno do registro de confirmação; desabilitada a promessa de assinatura criptográfica não implementada.
- Criada importação de PDF para arquivamento local cifrado, com hash SHA-256 de referência e mensagens explícitas de que não há validação ou associação a versão documental.
- Adicionada implementação de lembretes e preparado o tratamento de duração/pluralização da agenda.
- Trocada a leitura EXIF de radiografias para AndroidX ExifInterface, compatível com o nível mínimo de Android declarado.
- Reforçados isolamento e limpeza de dados fictícios nos testes instrumentados para reduzir interferência entre execuções.
- Documentadas privacidade, roteiro manual, limites de distribuição e estado F-Droid.

## Estado funcional conhecido

| Área | Estado nesta candidata |
| --- | --- |
| Inicialização / navegação principal | Correções feitas no código; compilação final pendente |
| Perfil profissional | Factory corrigida; abertura e retorno de Configurações passaram no Motorola |
| Rotas do prontuário | Seleção preservada; telas de navegação cobertas pelos testes instrumentados com paciente fictício |
| Documentos | Inicialização corrigida; rota, perfil e controles principais passaram nos testes instrumentados; exportar PDF segue no roteiro manual |
| Confirmação eletrônica | Registro local digitado com confirmação explícita; não é assinatura certificada |
| Importar PDF assinado | Cópia criptografada por paciente e hash informativo; assinatura não verificada, versão não vinculada |
| Assinatura manuscrita/criptográfica | Traçado não capturado; modalidade criptográfica indisponível |
| Lembretes | Implementação em código; permissões, reinício e fuso precisam de teste em aparelho |
| Privacidade | Aplicativo offline sem permissão INTERNET; revisar comportamento real e requisitos legais antes de uso clínico |

## Verificações

Na rodada final, `test`, assemble debug/release, lint e compilação do APK instrumentado concluíram com sucesso; 20/20 testes unitários e 11/11 testes instrumentados no Motorola passaram. O lint não apontou problemas de código nos módulos do app; resta um aviso interno do lint check da dependência AndroidX Lifecycle 2.11.0. Exportação PDF, importação de PDF assinado, restauração e acessibilidade continuam no roteiro manual.

A inspeção no Motorola confirmou anteriormente o encerramento ao abrir Perfil profissional e sua correção de factory; a navegação completa de Documentos e a assinatura não foram validadas manualmente. Nenhum dado clínico real deve ser usado na próxima rodada.

## Decisão

A candidata 0.13.0 é adequada para continuar testes com dados fictícios. Ainda não deve ser tratada como versão clínica ou publicada. Ver pendências em `ETAPA_15B_RELEASE_READINESS.md` e requisitos preliminares em `FDROID_READINESS.md`.
