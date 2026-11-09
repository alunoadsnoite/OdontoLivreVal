# Etapa 15A — Correção da inicialização no Motorola

**Data:** 09/11/2026  
**Dispositivo:** Motorola Edge 70 Fusion, Android 17 (API 37)  
**Versão do aplicativo:** 0.10.0

## Problema reproduzido

Ao abrir o aplicativo, o processo encerrava durante a composição da tela inicial. O logcat do aparelho mostrou:

```text
FATAL EXCEPTION: main
java.lang.NullPointerException
  at br.com.odontolivreval.HomeViewModel.load(HomeViewModel.kt:59)
  at br.com.odontolivreval.HomeViewModel.<init>(HomeViewModel.kt:24)
```

`HomeViewModel` chamava `load()` em seu bloco `init` antes de inicializar o `MutableStateFlow` consultado por `load()`. A ordem de inicialização foi corrigida: o estado agora é criado antes da primeira carga.

O relatório da Etapa 11B já mencionava falhas de inicialização em testes do `PatientRecordViewModel`, mas essas falhas foram classificadas como preexistentes e não levaram à revisão da ordem de inicialização dos ViewModels. Este crash confirma a necessidade de corrigir e cobrir esses casos.

## Validação

- `./gradlew :app:assembleDebug` — build concluído com sucesso.
- APK atualizado instalado no Motorola com `adb install -r`.
- `MainActivity` aberta e permaneceu como atividade em primeiro plano; processo permaneceu ativo.
- Não houve nova exceção fatal após a instalação da correção.
- Os testes automatizados e o fluxo de navegação por toque não foram executados nesta etapa.

APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.
