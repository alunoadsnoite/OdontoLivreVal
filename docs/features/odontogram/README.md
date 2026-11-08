# Odontograma

O odontograma está implementado dentro de `:feature:patients`, junto ao prontuário, porque pertence a uma evolução do paciente. O esquema atual usa a numeração FDI de dentição permanente: 11–18, 21–28, 31–38 e 41–48.

Cada dente pode receber um código persistido no mapa da consulta:

| Código | Significado |
| --- | --- |
| (vazio) | Sem marcação |
| `C` | Cárie |
| `R` | Restauração |
| `X` | Ausente/extraído |
| `I` | Implante |

A tela mostra as arcadas superior e inferior com rolagem horizontal em telas estreitas, legenda e descrição acessível por dente. O estado vem do ViewModel; o componente não mantém cópia local do mapa.

Próximas etapas: dentição decídua, marcação por superfície (oclusal, mesial, distal, vestibular e lingual/palatina), seleção múltipla de dentes e relatórios. Quando houver necessidade de reutilização fora do prontuário, criar `:feature:odontogram` com o componente e o modelo próprios. Persistência clínica depende das decisões em [SECURITY_DECISIONS.md](../../SECURITY_DECISIONS.md).
