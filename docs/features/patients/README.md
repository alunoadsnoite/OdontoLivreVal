# Pacientes

O módulo `:feature:patients` permite cadastrar, buscar pelo nome e editar pacientes fictícios. A busca ignora maiúsculas, minúsculas e acentos. O cadastro contém apenas um ID gerado pelo aplicativo e um nome, armazenados em um arquivo criptografado no diretório privado excluído de backup do Android.

O cadastro é de demonstração e segue em validação. Consulte as [decisões de segurança](../../SECURITY_DECISIONS.md) antes de implementar persistência clínica.
