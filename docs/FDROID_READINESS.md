# F-Droid: preparação e elegibilidade

**Candidata atual:** 0.14.0, versionCode 15

**Estado em 16/11/2026:** a solicitação de inclusão [!56870](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/56870) foi fechada em 12/11/2026 porque os mantenedores entenderam que o app ainda não cumpria o período mínimo de três meses de manutenção. Em 13/11, o autor pediu que considerassem uma exceção por manter outro app no F-Droid; não há resposta registrada. A conversa do MR informa que a branch de metadados foi atualizada para 0.14.0, mas a solicitação permanece fechada e o app não está no catálogo. O usuário confirmou em 16/11 que todos os cenários do roteiro manual foram aprovados; o registro está em [ROTEIRO_TESTES_MANUAIS.md](ROTEIRO_TESTES_MANUAIS.md), sem logs ou detalhes do aparelho anexados.

## Dados de identificação propostos

- Pacote: `br.com.odontolivreval`
- Repositório: https://github.com/alunoadsnoite/OdontoLivreVal
- Licença declarada: GPL-3.0-or-later; conferir se todos os recursos e dependências distribuídos têm licença compatível.
- Plataforma: Android, minSdk 26; compile/target SDK 37.
- Origem: código-fonte e Gradle Wrapper do repositório. Não incluir APKs pré-compilados como fonte do build.

## Pendências técnicas

- Os metadados descritivos, changelog, ícone e capturas de tela reais estão em `fastlane/metadata/android/{pt-BR,en-US}`. As capturas em português foram feitas no Motorola Edge 70 Fusion com uma instalação limpa e sem dados clínicos; incluem Início, Pacientes vazio e Agenda vazia.
- A submissão ao repositório principal exige um registro em `fdroiddata` (não neste repositório) e uma revisão dos mantenedores. Para uma futura solicitação, a revisão de origem deve apontar para o hash completo do commit que contém a versão submetida; não usar nome de branch ou tag como `commit`.
- Confirmar que a compilação do fdroidserver usa apenas dependências disponíveis e não depende de artefatos pré-compilados ou rede durante a fase de build. O build release agora fica sem assinatura de desenvolvedor quando as variáveis `ODONTO_RELEASE_*` não estão configuradas; o F-Droid assina o APK publicado.
- Validar assinatura/reprodutibilidade e executar a análise de metadados e varreduras do F-Droid.
- Confirmar as licenças de bibliotecas, imagens, ícones e outros recursos.
- Os testes manuais foram concluídos em 16/11/2026, com aprovação informada pelo usuário. A revisão independente de segurança, privacidade, retenção e requisitos jurídicos para uso clínico continua pendente.
- O MR foi fechado pela regra de três meses. O histórico local disponível começa em 07/11/2026; sem exceção aceita, a referência de três meses é 07/02/2027, a confirmar com os mantenedores. A exceção baseada no FocusFeed (`io.github.alunoadsnoite.focusfeed`) foi solicitada, mas ainda não foi aceita; não tratar a exceção como aprovada.
- A tag `v0.14.0` e a atualização da branch foram informadas no MR em 13/11/2026; confirmar a tag e o hash completo do commit no GitHub antes de uma nova submissão.
- Corrigir a descrição para refletir o estado do app no momento da submissão; ele continua em validação e não deve ser apresentado como pronto para uso clínico.

O guia oficial explica o fluxo de submissão e revisão [Submetendo ao F-Droid](https://fdroid.gitlab.io/jekyll-fdroid/docs/Submitting_to_F-Droid_Quick_Start_Guide/). A política de inclusão pede manutenção ativa por pelo menos três meses, salvo exceções explícitas, além de app funcional, útil e não demonstrativo ([Política de inclusão](https://fdroid.gitlab.io/jekyll-fdroid/en/docs/Inclusion_Policy/)). Os metadados de build ficam no repositório `fdroiddata`, com `commit` apontando para hash completo ([referência de metadados](https://fdroid.gitlab.io/jekyll-fdroid/docs/Build_Metadata_Reference/)).

A solicitação !56870 está fechada e o app ainda não está no catálogo. Não abrir uma solicitação duplicada enquanto a exceção não for respondida ou o período mínimo não for cumprido. A configuração anterior continha uma senha padrão de assinatura no código; essa credencial foi removida e a chave usada com ela não deve ser reutilizada para distribuição própria.
