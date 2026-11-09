# Prontidão de versão — 0.13.0

**Estado:** candidata para validação, não pronta para distribuição clínica.  
**versionCode:** 13  
**Atualizado:** 09/11/2026

## O que foi preparado

- Corrigidas falhas que impediam abrir o início, áreas do prontuário, Documentos e Perfil profissional, conforme os relatos e o histórico técnico disponível.
- Adicionado fluxo de importação de PDF do assinador para arquivamento criptografado por paciente, com hash SHA-256 informativo. Não há validação de assinatura nem vínculo automático com uma versão documental.
- Tornadas explícitas as limitações da confirmação eletrônica local e da modalidade criptográfica indisponível.
- Incluídos aviso de privacidade e roteiro de testes manuais com dados fictícios.
- Definida a versão de desenvolvimento como 0.13.0 / versionCode 13.

## Evidência automatizada existente

Na compilação final de 09/11/2026, concluíram com sucesso `:app:assembleDebug`, `:app:assembleRelease`, `:app:assembleDebugAndroidTest` e `lint`. Nenhum teste funcional foi executado nesta rodada. O lint do app registra 231 avisos `UnusedResources` e nenhum erro; os módulos também exibem avisos Kotlin já existentes de nullability redundante.

Os testes instrumentados não foram executados em dispositivo. Essa validação foi combinada para a sessão da noite.

## Bloqueios antes de uso clínico ou distribuição

1. Executar testes instrumentados e o roteiro manual em Android real, incluindo Motorola, navegação Documentos/Perfil, persistência, atualização, backup/restauração, anexos e notificações.
2. Revisar resultados e corrigir falhas encontradas nos testes.
3. Fazer avaliação de segurança, privacidade, retenção e requisitos jurídicos para uso clínico real. Este app não declara conformidade integral com a LGPD.
4. Resolver a associação entre PDF importado e versão de documento e decidir sobre captura de assinatura manuscrita e validação criptográfica; as funções atuais não substituem assinatura profissional certificada.
5. Preparar assinatura de release e comprovar atualização/reprodutibilidade antes de distribuir APK.
6. Para F-Droid, preparar metadados e validar o build a partir do código-fonte. Nenhuma submissão está autorizada neste momento.

## Artefatos locais

O APK debug de desenvolvimento está em `app/build/outputs/apk/debug/app-debug.apk` e foi copiado para `~/Downloads/OdontoLivreVal-0.13.0-debug.apk` para transferência local. SHA-256: `eb83af47747350d26b58112b427e7c1b9a2d6ceaf203daa99089ce67089265f7`. O APK release gerado pelo Gradle é unsigned e não é um artefato instalável para distribuição.
