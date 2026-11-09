# Etapa 15C — Documentos e fluxo de assinatura

**Data:** 09/11/2026
**Versão candidata:** 0.13.0 (versionCode 13; desenvolvimento)

## Documentos

O `DocumentsViewModel` iniciava `load()` no bloco `init` antes de inicializar o `MutableStateFlow` e o `Job` acessados pelo carregamento. A ordem causava falha ao criar o ViewModel. Os campos agora são inicializados antes da chamada `load()`.

## Assinaturas

- Os botões de assinatura manuscrita e em papel agora abrem a captura, e o ViewModel é inicializado somente quando os argumentos da tela estão prontos, sem reiniciar a cada recomposição.
- O fluxo do prontuário usa uma referência válida (`patient_record` e ID do paciente), em vez de ID `-1`, rejeitado pela validação.
- O salvamento agora observa o resultado assíncrono e retorna após persistir; a confirmação informada é registrada nas notas do comprovante.
- A lista de assinaturas inicia o carregamento. A modalidade criptográfica fica desativada enquanto não houver integração com certificado/provedor. O registro eletrônico local não é certificado ICP-Brasil.
- O acesso ao perfil profissional já está na tela **Configurações** e usa a mesma tela acessada por **Abrir perfil profissional** em Documentos.
- Corrigida a factory do ViewModel do perfil: faltava registrar o `initializer`, causando `IllegalArgumentException: No initializer set for ... ProfessionalProfileViewModel` e encerrando o app ao abrir a tela.

## Verificação

- A versão anterior à candidata final compilou com `:app:assembleDebug`.
- O atalho **Perfil profissional** foi colocado explicitamente no topo da tela Configurações, antes das opções de aparência. A confirmação visual no Motorola ficou pendente após bloqueio do aparelho.
- `git diff --check` concluído sem erros.
- Perfil aberto no Motorola sem repetir o crash e retorno a Configurações conferido. A mensagem anterior do AndroidRuntime confirmou a causa: ausência do initializer da factory. A visibilidade do atalho após o último reposicionamento não foi conferida porque o aparelho voltou à tela de bloqueio.
- A tela Documentos e a persistência de uma assinatura não foram percorridas manualmente nesta sessão.
- O fluxo de importação foi implementado para escolher PDF, calcular hash SHA-256 e arquivar os bytes no armazenamento criptografado do paciente. O hash não certifica assinatura nem associa o arquivo a uma versão do documento.
- A confirmação eletrônica exige nome e confirmação explícita; continua sendo apenas um registro local. Não captura traçado manuscrito nem realiza assinatura digital certificada.
- Build final da candidata 0.13.0: `:app:assembleDebug`, `:app:assembleRelease`, `:app:assembleDebugAndroidTest`, `lint` e `test` concluíram com sucesso em 09/11/2026. Os 20 testes unitários passaram. Avisos de recursos duplicados e dois avisos de lint no código foram removidos; resta um aviso `ObsoleteLintCustomCheck` interno do verificador AndroidX Lifecycle 2.11.0.
- Os 11 testes instrumentados passaram no Motorola Edge 70 Fusion: 2 de lembretes, 4 de armazenamento e 5 de navegação, perfil e Documentos. Os cenários de navegação foram alinhados aos rótulos reais em português usados pela interface.
- A permissão de notificação agora é solicitada apenas pelo botão **Ativar lembretes** na Agenda, em vez de aparecer na primeira abertura.
- APK debug atual em `~/Downloads/OdontoLivreVal-0.13.0-debug.apk`; SHA-256: `9e3e2fbe2de9ca815440310f86d16c10947cfb6d1396d2899bebe0e7ab6c75d2`. A compilação release é unsigned e não deve ser distribuída.
