# Prontidão de versão — 0.13.0

**Estado:** candidata para distribuição, APK release assinado gerado.  
**versionCode:** 13  
**Atualizado:** 10/11/2026

## O que foi preparado

- Corrigidas falhas que impediam abrir o início, áreas do prontuário, Documentos e Perfil profissional, conforme os relatos e o histórico técnico disponível.
- Adicionado fluxo de importação de PDF do assinador para arquivamento criptografado por paciente, com hash SHA-256 informativo. Não há validação de assinatura nem vínculo automático com uma versão documental.
- Tornadas explícitas as limitações da confirmação eletrônica local e da modalidade criptográfica indisponível.
- Incluídos aviso de privacidade e roteiro de testes manuais com dados fictícios.
- Definida a versão de desenvolvimento como 0.13.0 / versionCode 13.
- A permissão de notificações não é mais solicitada na abertura; a Agenda oferece ação explícita para ativar lembretes.
- **APK release assinado gerado** em `app/build/outputs/apk/release/app-release.apk` com keystore `app/release.keystore` (alias `odontolivreval`). SHA-256 do release: `ce156993b75d8f6f9e60568ba7097a9c7153f81757c2b501d7e10b614c5f0225`.

## Evidência automatizada existente

Na compilação final de 10/11/2026, concluíram com sucesso `:app:assembleDebug`, `:app:assembleRelease`, `:app:assembleDebugAndroidTest`, `lint` e `test`. Os 20 testes unitários passaram. O lint não aponta problemas no código do app; resta um aviso interno `ObsoleteLintCustomCheck` no verificador da biblioteca AndroidX Lifecycle 2.11.0, que desativa dois checks próprios dessa dependência por incompatibilidade com a API de lint atual.

Na execução final, os 11 testes instrumentados passaram no Motorola Edge 70 Fusion: 2 de lembretes, 4 de armazenamento e 5 de navegação, perfil e documentos. Nenhuma permissão foi alterada manualmente para os testes. A validação manual ampliada de PDF/assinatura, backup e notificações continua no roteiro.

## Bloqueios antes de uso clínico ou distribuição

1. Executar o roteiro manual em Android real com dados fictícios, incluindo PDF/assinatura externa, backup/restauração, anexos, notificações e acessibilidade.
2. Revisar resultados e corrigir eventuais falhas encontradas na validação manual.
3. Fazer avaliação de segurança, privacidade, retenção e requisitos jurídicos para uso clínico real. Este app não declara conformidade integral com a LGPD.
4. Resolver a associação entre PDF importado e versão de documento e decidir sobre captura de assinatura manuscrita e validação criptográfica; as funções atuais não substituem assinatura profissional certificada.
5. ✅ Assinatura de release preparada — APK release assinado gerado com keystore próprio. Reprodutibilidade depende do mesmo keystore e senha.
6. Para F-Droid, cumprir o período de manutenção da política de inclusão (ou demonstrar exceção), finalizar a validação manual e validar o build a partir do código-fonte. Os metadados localizados foram preparados; a solicitação ao repositório principal não foi aberta.

## Artefatos locais

- APK debug de desenvolvimento: `app/build/outputs/apk/debug/app-debug.apk` (copiado para `~/Downloads/OdontoLivreVal-0.13.0-debug.apk`, SHA-256: `9e3e2fbe2de9ca815440310f86d16c10947cfb6d1396d2899bebe0e7ab6c75d2`)
- APK release assinado: `app/build/outputs/apk/release/app-release.apk` (copiado para `~/Downloads/OdontoLivreVal-0.13.0-release.apk`, SHA-256: `ce156993b75d8f6f9e60568ba7097a9c7153f81757c2b501d7e10b614c5f0225`)

## Submissão

O repositório remoto já contém o código-fonte desta versão. O envio para loja de aplicações (Google Play, F-Droid, etc.) deve ser feito com o APK release assinado e o mesmo keystore para atualizações futuras.
