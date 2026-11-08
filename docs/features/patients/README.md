# Pacientes e prontuário

O módulo `:feature:patients` permite cadastrar, buscar pelo nome, editar e excluir pacientes fictícios. A busca e a ordenação ignoram maiúsculas, minúsculas, espaços nas pontas e acentos. O cadastro contém um ID gerado pelo aplicativo e um nome, armazenados em um arquivo criptografado no diretório privado excluído de backup do Android.

Ao tocar no nome, o prontuário abre a ficha do paciente:

- Data de nascimento (máscara `dd/mm/aaaa` e validação) e telefone com DDD.
- Queixa principal, histórico/anamnese e observações.
- Diagnóstico e conduta da consulta ativa.
- Odontograma FDI permanente editável por dente: cárie, restauração, ausente/extraído e implante.
- Histórico de evoluções, com a consulta ativa destacada.

**Salvar ficha** grava ficha e consulta juntas. **Nova consulta** inicia uma evolução em branco sem apagar as anteriores. Se houver alterações não salvas, sair ou iniciar outra consulta pede confirmação.

O cadastro é de demonstração e segue em validação. Consulte as [decisões de segurança](../../SECURITY_DECISIONS.md) antes de implementar persistência clínica.
