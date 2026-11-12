# Prontidão de versão — 0.13.1

**Estado:** candidata para validação; build release sem assinatura de desenvolvedor validado para preparação F-Droid.
**versionCode:** 14  
**Atualizado:** 11/11/2026

## Atualização da candidata 0.13.1

- Build `:app:assembleDebug` concluído em 11/11/2026; APK versionCode 14 instalado no Motorola Edge 70 Fusion.
- Corrigido o endereço de distribuição do Gradle Wrapper para uso pelo buildserver do F-Droid.
- Incluídas três capturas reais em português feitas com instalação limpa e sem dados clínicos.
- A pipeline do GitLab ainda precisa validar os metadados e o buildserver; os testes funcionais manuais seguem pendentes no roteiro.

## O que foi preparado

- Corrigidas falhas que impediam abrir o início, áreas do prontuário, Documentos e Perfil profissional, conforme os relatos e o histórico técnico disponível.
- Adicionado fluxo de importação de PDF do assinador para arquivamento criptografado por paciente, com hash SHA-256 informativo. Não há validação de assinatura nem vínculo automático com uma versão documental.
- Tornadas explícitas as limitações da confirmação eletrônica local e da modalidade criptográfica indisponível.
- Incluídos aviso de privacidade e roteiro de testes manuais com dados fictícios.
- Atualizada a candidata para 0.13.1 / versionCode 14 após corrigir o Gradle Wrapper para o F-Droid.
- A permissão de notificações não é mais solicitada na abertura; a Agenda oferece ação explícita para ativar lembretes.
- Um APK release assinado foi gerado localmente em 10/11. A configuração usava uma senha padrão no código-fonte público; essa senha foi removida em 11/11 e a chave antiga não deve ser usada para distribuição própria. Sem variáveis `ODONTO_RELEASE_*`, o build release não assina o APK, como esperado para a compilação de origem do F-Droid.

## Evidência automatizada existente

Na compilação final de 10/11/2026, concluíram com sucesso `:app:assembleDebug`, `:app:assembleRelease`, `:app:assembleDebugAndroidTest`, `lint` e `test`. Os 20 testes unitários passaram. O lint não aponta problemas no código do app; resta um aviso interno `ObsoleteLintCustomCheck` no verificador da biblioteca AndroidX Lifecycle 2.11.0, que desativa dois checks próprios dessa dependência por incompatibilidade com a API de lint atual.

Na execução final, os 11 testes instrumentados passaram no Motorola Edge 70 Fusion: 2 de lembretes, 4 de armazenamento e 5 de navegação, perfil e documentos. Nenhuma permissão foi alterada manualmente para os testes. A validação manual ampliada de PDF/assinatura, backup e notificações continua no roteiro.

Em 11/11/2026, `:app:assembleRelease` concluiu sem as variáveis `ODONTO_RELEASE_*` e gerou `app-release-unsigned.apk`. A verificação de assinatura confirmou que está sem assinatura de desenvolvedor, e `fdroid scanner` não encontrou classes proprietárias no APK.

## Bloqueios antes de uso clínico ou distribuição

1. Executar o roteiro manual em Android real com dados fictícios, incluindo PDF/assinatura externa, backup/restauração, anexos, notificações e acessibilidade.
2. Revisar resultados e corrigir eventuais falhas encontradas na validação manual.
3. Fazer avaliação de segurança, privacidade, retenção e requisitos jurídicos para uso clínico real. Este app não declara conformidade integral com a LGPD.
4. Resolver a associação entre PDF importado e versão de documento e decidir sobre captura de assinatura manuscrita e validação criptográfica; as funções atuais não substituem assinatura profissional certificada.
5. Configurar uma chave nova e secreta para qualquer distribuição própria. A configuração de release do F-Droid deve permanecer sem chave de desenvolvedor.
6. Para F-Droid, finalizar a validação manual e validar o build no ambiente do F-Droid. Consultar a [preparação para o F-Droid](FDROID_READINESS.md) para acompanhar a elegibilidade e as pendências de build.

## Artefatos locais

- APK debug de desenvolvimento: `app/build/outputs/apk/debug/app-debug.apk` (copiado para `~/Downloads/OdontoLivreVal-0.13.0-debug.apk`, SHA-256: `9e3e2fbe2de9ca815440310f86d16c10947cfb6d1396d2899bebe0e7ab6c75d2`)
- APK release sem assinatura de desenvolvedor: `app/build/outputs/apk/release/app-release-unsigned.apk`. O APK release assinado anteriormente não deve ser publicado.

## Submissão

O repositório remoto contém o código-fonte desta versão. O F-Droid compila a partir do código e assina o APK no repositório; uma distribuição própria exigirá uma nova chave segura e a mesma chave para as atualizações dessa distribuição.
