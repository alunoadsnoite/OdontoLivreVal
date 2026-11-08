# Decisões de segurança

## Proteção implementada na versão de teste

- Pacientes fictícios, consultas, fichas e evoluções ficam em um arquivo AES-256-GCM no diretório privado excluído do backup automático do Android. O Android Keystore mantém a chave da instalação. A preferência de tema permanece em DataStore.
- O primeiro acesso ao cadastro, à agenda ou à exportação migra os esquemas SQLite 1 e 2. A migração abre o banco antigo, grava e verifica o arquivo protegido antes de remover o banco. Se o arquivo protegido estiver ilegível, o app apresenta erro e preserva o arquivo. A remoção do banco legado não garante eliminação física de todas as cópias em armazenamento flash.
- O bloqueio é opcional nesta versão de teste. Ele usa o PIN, padrão ou senha do Android e pode oferecer biometria forte a partir do Android 11. Quando ativo, bloqueia ao sair da tela do app e protege a janela contra captura. O bloqueio controla a interface. A chave de armazenamento é restrita ao app pelo Keystore e não é vinculada a cada desbloqueio.
- O backup é um arquivo portátil com AES-256-GCM. A chave é derivada da senha por PBKDF2-HMAC-SHA256 com sal aleatório e 600 mil iterações. O formato 2 grava a versão do formato e o número de iterações autenticado no cabeçalho; o formato 1 anterior continua sendo lido, sempre com o custo original. A senha tem no mínimo 12 caracteres na exportação e não é salva pelo app. Sem ela, o arquivo não pode ser restaurado.
- O seletor de documentos do Android permite escolher um destino local ou um provedor configurado no aparelho. O app trabalha com o arquivo criptografado e não precisa de acesso amplo ao armazenamento. Se a exportação for interrompida depois da escolha do destino, o provedor pode deixar um arquivo vazio ou incompleto.
- A restauração autentica e valida o arquivo inteiro antes de substituir os cadastros e consultas. O formato de teste tem limite de 4 MiB de conteúdo antes da criptografia, recusa versões de dados desconhecidas e guarda apenas dados fictícios. Anexos exigirão outro desenho.
- A ficha e a consulta são gravadas em uma única transação no arquivo. IDs de fichas e consultas são gerados pelo armazenamento, nunca pela tela, evitando colisões ou sobrescrita entre pacientes.

Referências técnicas [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile) e [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files). A escolha de custo para PBKDF2 segue a [referência da OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

## Pendências antes de dados clínicos

1. Validar em dispositivos reais a migração, o bloqueio, a reinstalação, a perda de credencial, o backup e a restauração entre aparelhos. Medir desempenho em dispositivos mais antigos.
2. Definir política de bloqueio, retenção, exclusão e recuperação para uso clínico. Validar também as opções de acessibilidade e o comportamento em segundo plano.
3. Definir persistência e criptografia para prontuário, fotos, radiografias, documentos e arquivos temporários. O arquivo de demonstração tem escopo e capacidade limitados.
4. Revisar integridade, migrações, concorrência, falhas de gravação, cadeia de dependências e divulgação de segurança antes de distribuir uma versão clínica.
5. Definir auditoria, assinatura profissional e retenção legal do prontuário, que não fazem parte desta versão de teste.

A versão atual serve para testes com dados fictícios.
