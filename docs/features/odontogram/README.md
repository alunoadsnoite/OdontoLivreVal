# Odontograma

O odontograma está implementado dentro de `:feature:patients`, junto ao prontuário, porque pertence a uma evolução do paciente. O esquema usa a numeração FDI nas dentições permanente (11–18, 21–28, 31–38 e 41–48) e decídua (51–55, 61–65, 71–75 e 81–85).

Cada dente pode receber um código de marcação geral. Cárie e restauração também podem ser registradas de forma independente por face, incluindo incisal nos dentes anteriores e oclusal nos posteriores.

| Código | Significado |
| --- | --- |
| (vazio) | Sem marcação |
| `C` | Cárie |
| `E` | Canal |
| `R` | Restauração |
| `X` | Ausente/extraído |
| `I` | Implante |

A tela mostra as arcadas superior e inferior com rolagem horizontal em telas estreitas, legenda e descrição acessível por dente. O estado vem do ViewModel; o componente não mantém cópia local do mapa.

A evolução salva pode ser exportada em PDF com nome do paciente, data, numeração dos dentes, faces marcadas e legenda. O PDF usa uma página por dentição com marcações.

O armazenamento lê as marcações antigas de dente inteiro. Backups que usam o novo esquema de faces precisam da versão 0.8.0 ou posterior. Seleção múltipla de dentes continua pendente. Quando houver necessidade de reutilização fora do prontuário, criar `:feature:odontogram` com o componente e o modelo próprios. Persistência clínica depende das decisões em [SECURITY_DECISIONS.md](../../SECURITY_DECISIONS.md).
