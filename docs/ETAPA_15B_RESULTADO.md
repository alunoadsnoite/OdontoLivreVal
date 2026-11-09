# Etapa 15B — Correção das rotas do prontuário

**Data:** 09/11/2026  
**Versão instalada:** 0.10.0 (versionCode 10)  
**Dispositivo disponível:** Motorola Edge 70 Fusion, Android 17 (API 37)

## Problema

As rotas de atestados, planos de tratamento, orçamentos, consentimentos e assinaturas dependem do paciente selecionado no prontuário. `OdontoApp` limpava essa seleção sempre que a rota não era `patient_record`, `documents` ou `profile`. Nas demais rotas clínicas, as telas recebiam `patient == null` e mostravam apenas o botão **Voltar**.

Os relatórios das Etapas 13A, 13B e 14A descreviam a navegação desses módulos como implementada, mas não registravam esse caso de estado perdido. O histórico OpenCode `ses_e45706880ffeZYn8Chx99lLNSf` contém a Etapa 15A, que pede auditoria de navegação e gerenciamento de estado. Não encontrei nesse histórico uma análise ou correção para este caso.

## Correção

- Removida a limpeza automática da seleção baseada no nome da rota, que podia descartar o paciente durante a navegação interna.
- A seleção permanece entre as telas de documentos, atestados, tratamento, orçamento, consentimento e assinaturas.
- A seleção é limpa nas ações explícitas de saída do prontuário ou ao escolher uma área principal.

## Verificação

- `./gradlew :app:assembleDebug` — concluído com sucesso após a correção.
- Esta versão foi substituída pela correção complementar descrita em `ETAPA_15C_RESULTADO.md`.
- A tela inicial e a lista de pacientes foram abertas no dispositivo.
- A navegação para um prontuário existente não foi percorrida nesta verificação para evitar abrir dados pessoais/clínicos existentes. As rotas foram verificadas no código e o APK instalado; a conferência final das telas requer um registro fictício.
