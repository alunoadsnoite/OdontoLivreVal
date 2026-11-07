# Decisões de segurança — Etapa 01

## Escopo atual

Um profissional por dispositivo. Somente a preferência não sensível de aparência é persistida em DataStore privado do aplicativo. Sem nome, CRO, pacientes, prontuários, anamnese, fotos, radiografias, cadastro fictício funcional ou outro dado clínico. Sem permissões de internet, localização, câmera ou armazenamento. Sem logs sensíveis, nuvem, anúncios ou analytics. Backup Android desabilitado; autenticação e backup do produto adiados.

## Ameaças e limites

- Dispositivo perdido, desbloqueado ou compartilhado: o aplicativo ainda não oferece controle de acesso; atualmente não contém dados clínicos.
- Dispositivo comprometido/root e extração de arquivos: sandbox Android não equivale a criptografia própria. Não prometer proteção em dispositivo comprometido.
- Cópias, screenshots, exportações e backups: nenhum fluxo clínico existe. Política de captura e ciclo de vida de exportações deve ser definida antes de implementá-los.
- Cadeia de dependências e distribuição: versões fixas e repositórios conhecidos; revisar dependências, assinatura e atualizações antes de release. Não incluir segredos ou chaves de assinatura no Git.

## Requisitos pendentes antes de dados reais

1. Aprovar arquitetura de criptografia em repouso para banco, anexos, arquivos temporários e exportações. **Room não criptografa por padrão**; adotá-lo exige estratégia adicional aprovada.
2. Definir geração, armazenamento (avaliar Android Keystore), desbloqueio, invalidação, rotação e recuperação de chaves. Decidir comportamento após perda de chave, troca de aparelho e reinstalação.
3. Aprovar autenticação local, tempo de bloqueio e comportamento de conteúdo em segundo plano; avaliar acessibilidade.
4. Planejar e testar migrações, rollback seguro, integridade, concorrência e recuperação sem perda silenciosa. Testar atualização de versões com dados sintéticos.
5. Definir política de retenção e exclusão, consentimento, exportação e requisitos aplicáveis de privacidade com responsáveis pelo produto.
6. Aprovar backup criptografado, restauração e modelo de recuperação antes de habilitar backup. Não confiar apenas no backup do sistema.
7. Validar permissões mínimas, ausência de logs/dados de teste reais e divulgação de segurança antes da primeira versão clínica.

Esses itens registram decisões ainda não aprovadas; esta fundação não implementa um prontuário seguro nem autoriza uso clínico.
