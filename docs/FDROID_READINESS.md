# Preparação para F-Droid

**Candidata analisada:** 0.13.0, versionCode 13  
**Estado:** documentação inicial; não submetido. A publicação depende de autorização explícita do mantenedor.

## Dados de identificação propostos

- Pacote: `br.com.odontolivreval`
- Repositório: https://github.com/alunoadsnoite/OdontoLivreVal
- Licença declarada: GPL-3.0-or-later; conferir se todos os recursos e dependências distribuídos têm licença compatível.
- Plataforma: Android, minSdk 26; compile/target SDK 37.
- Origem: código-fonte e Gradle Wrapper do repositório. Não incluir APKs pré-compilados como fonte do build.

## Pendências técnicas

- Definir tag/revisão de origem e metadados F-Droid com `Build` reproduzível para versionCode 13.
- Confirmar que a compilação do fdroidserver usa apenas dependências disponíveis e não depende de artefatos pré-compilados ou rede durante a fase de build.
- Validar assinatura/reprodutibilidade e executar a análise de metadados e varreduras do F-Droid.
- Confirmar as licenças de bibliotecas, imagens, ícones e outros recursos.
- Concluir testes funcionais reais e a revisão de segurança/privacidade descrita em `ETAPA_15B_RELEASE_READINESS.md`.
- Verificar políticas e documentação atuais do F-Droid no momento de uma eventual submissão.

O guia oficial de submissão descreve o fluxo de inclusão e revisão [Submetendo ao F-Droid](https://fdroid.gitlab.io/jekyll-fdroid/docs/Submitting_to_F-Droid_Quick_Start_Guide/). Os metadados e comandos de build são descritos na [referência de metadados](https://fdroid.gitlab.io/jekyll-fdroid/docs/Build_Metadata_Reference/); a validação de builds reproduzíveis é detalhada em [Reproducible Builds](https://fdroid.gitlab.io/jekyll-fdroid/docs/Reproducible_Builds/).

Este arquivo registra preparação; não é uma submissão nem aprovação para publicar.
