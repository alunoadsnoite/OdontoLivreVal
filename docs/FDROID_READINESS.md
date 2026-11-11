# F-Droid: preparação e elegibilidade

**Candidata analisada:** 0.13.0, versionCode 13  
**Estado em 11/11/2026:** metadados descritivos em português e ícone preparados. A exceção de elegibilidade por manter outros apps no F-Droid está comprovada pelo perfil público do mantenedor; a solicitação de inclusão ainda não foi aberta.

## Dados de identificação propostos

- Pacote: `br.com.odontolivreval`
- Repositório: https://github.com/alunoadsnoite/OdontoLivreVal
- Licença declarada: GPL-3.0-or-later; conferir se todos os recursos e dependências distribuídos têm licença compatível.
- Plataforma: Android, minSdk 26; compile/target SDK 37.
- Origem: código-fonte e Gradle Wrapper do repositório. Não incluir APKs pré-compilados como fonte do build.

## Pendências técnicas

- Os metadados descritivos e o ícone estão em `fastlane/metadata/android/pt-BR`. Ainda faltam capturas de tela reais do app.
- A submissão ao repositório principal exige um registro em `fdroiddata` (não neste repositório) e uma revisão dos mantenedores. Para `versionCode 13`, a revisão de origem deve apontar para o hash completo do commit que contém esta versão; não usar nome de branch ou tag como `commit`.
- Confirmar que a compilação do fdroidserver usa apenas dependências disponíveis e não depende de artefatos pré-compilados ou rede durante a fase de build. O build release agora fica sem assinatura de desenvolvedor quando as variáveis `ODONTO_RELEASE_*` não estão configuradas; o F-Droid assina o APK publicado.
- Validar assinatura/reprodutibilidade e executar a análise de metadados e varreduras do F-Droid.
- Confirmar as licenças de bibliotecas, imagens, ícones e outros recursos.
- Concluir testes funcionais reais e a revisão de segurança/privacidade descrita em `ETAPA_15B_RELEASE_READINESS.md`.
- A política exige três meses de desenvolvimento ativo, salvo exceções. A exceção de mantenedor se aplica: o mesmo mantenedor publica o FocusFeed (`io.github.alunoadsnoite.focusfeed`) no F-Droid ([listagem oficial](https://f-droid.org/pt/packages/io.github.alunoadsnoite.focusfeed/)). A aceitação continua sujeita à análise dos mantenedores do F-Droid.
- Criar uma tag versionada para o commit de cada release oficial; a candidata 0.13.0 ainda não tem tag.
- Corrigir a descrição para refletir o estado do app no momento da submissão; ele continua em validação e não deve ser apresentado como pronto para uso clínico.

O guia oficial explica o fluxo de submissão e revisão [Submetendo ao F-Droid](https://fdroid.gitlab.io/jekyll-fdroid/docs/Submitting_to_F-Droid_Quick_Start_Guide/). A política de inclusão pede manutenção ativa por pelo menos três meses, salvo exceções explícitas, além de app funcional, útil e não demonstrativo ([Política de inclusão](https://fdroid.gitlab.io/jekyll-fdroid/en/docs/Inclusion_Policy/)). Os metadados de build ficam no repositório `fdroiddata`, com `commit` apontando para hash completo ([referência de metadados](https://fdroid.gitlab.io/jekyll-fdroid/docs/Build_Metadata_Reference/)).

Ainda não foi aberta uma solicitação de inclusão. A sessão local do GitLab não está autenticada; será necessário entrar no GitLab para criar a merge request. A configuração anterior continha uma senha padrão de assinatura no código; essa credencial foi removida e a chave usada com ela não deve ser reutilizada para distribuição própria.
