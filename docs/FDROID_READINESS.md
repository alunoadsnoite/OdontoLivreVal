# F-Droid: preparação e elegibilidade

**Candidata analisada:** 0.13.0, versionCode 13  
**Estado em 09/11/2026:** metadados localizados preparados; ainda não submetido e não elegível, pelo momento, à inclusão no repositório principal.

## Dados de identificação propostos

- Pacote: `br.com.odontolivreval`
- Repositório: https://github.com/alunoadsnoite/OdontoLivreVal
- Licença declarada: GPL-3.0-or-later; conferir se todos os recursos e dependências distribuídos têm licença compatível.
- Plataforma: Android, minSdk 26; compile/target SDK 37.
- Origem: código-fonte e Gradle Wrapper do repositório. Não incluir APKs pré-compilados como fonte do build.

## Pendências técnicas

- Publicar os metadados de loja em `fastlane/metadata/android/pt-BR` junto com o código-fonte.
- A submissão ao repositório principal exige um registro em `fdroiddata` (não neste repositório) e uma revisão dos mantenedores. Para `versionCode 13`, a revisão de origem deve apontar para o hash completo do commit que contém esta versão; não usar nome de branch ou tag como `commit`.
- Confirmar que a compilação do fdroidserver usa apenas dependências disponíveis e não depende de artefatos pré-compilados ou rede durante a fase de build.
- Validar assinatura/reprodutibilidade e executar a análise de metadados e varreduras do F-Droid.
- Confirmar as licenças de bibliotecas, imagens, ícones e outros recursos.
- Concluir testes funcionais reais e a revisão de segurança/privacidade descrita em `ETAPA_15B_RELEASE_READINESS.md`.
- Manter o projeto por pelo menos três meses antes de pedir inclusão, ou demonstrar uma exceção prevista pela política. O histórico público atual começa em 07/11/2026 e ainda não demonstra três meses de manutenção nem usuários comprovados ou outra exceção.
- Corrigir a descrição para refletir o estado do app no momento da submissão; ele continua em validação e não deve ser apresentado como pronto para uso clínico.

O guia oficial explica o fluxo de submissão e revisão [Submetendo ao F-Droid](https://fdroid.gitlab.io/jekyll-fdroid/docs/Submitting_to_F-Droid_Quick_Start_Guide/). A política de inclusão pede manutenção ativa por pelo menos três meses, salvo exceções explícitas, além de app funcional, útil e não demonstrativo ([Política de inclusão](https://fdroid.gitlab.io/jekyll-fdroid/en/docs/Inclusion_Policy/)). Os metadados de build ficam no repositório `fdroiddata`, com `commit` apontando para hash completo ([referência de metadados](https://fdroid.gitlab.io/jekyll-fdroid/docs/Build_Metadata_Reference/)).

Não foi aberta uma submissão: a política de três meses ainda não foi atendida e a sessão local do GitLab não está autenticada. A sessão do GitHub também está com token inválido; a tentativa de envio pode ser feita pelo Git remoto, mas só se as credenciais locais permitirem.
