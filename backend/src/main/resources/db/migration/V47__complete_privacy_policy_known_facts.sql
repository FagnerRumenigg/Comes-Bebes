UPDATE application.content_documents
SET body = replace(body, 'Controlador: [A CONFIRMAR — razão social, CNPJ e endereço].', 'Controlador: Fagner Rumenigg, pessoa física responsável pelo Comes&Bebes. Não é divulgado endereço residencial.')
WHERE slug = 'PRIVACY_POLICY';

UPDATE application.content_documents
SET body = replace(body, 'Canal de privacidade: [A CONFIRMAR — e-mail ou formulário].', 'Canal de privacidade: rumeniggxx@gmail.com.')
WHERE slug = 'PRIVACY_POLICY';

UPDATE application.content_documents
SET body = replace(body, 'Encarregado ou canal equivalente: [A CONFIRMAR].', 'Encarregado e canal de comunicação: Fagner Rumenigg, pelo e-mail rumeniggxx@gmail.com.')
WHERE slug = 'PRIVACY_POLICY';

UPDATE application.content_documents
SET body = replace(body, 'As bases legais de cada finalidade devem ser confirmadas pelo controlador antes da publicação desta política: [A CONFIRMAR].', 'As bases legais são: execução do contrato para conta, autenticação, publicação e exibição; legítimo interesse para segurança e moderação; consentimento para comunicações não essenciais; e cumprimento de obrigação legal quando aplicável.')
WHERE slug = 'PRIVACY_POLICY';

UPDATE application.content_documents
SET body = replace(body, 'Fornecedores, finalidades, localização e eventual transferência internacional: [A CONFIRMAR].', 'Usamos Azure Container Apps, Azure Static Web Apps, Azure Database for PostgreSQL Flexible Server, Azure Storage Account/Blob Storage, Azure Log Analytics e Azure Communication Services. GitHub Actions é usado para CI/CD e o validador de imagens é um serviço próprio separado. Transferência internacional ainda não confirmada.')
WHERE slug = 'PRIVACY_POLICY';

UPDATE application.content_documents
SET body = replace(body, 'Períodos ou critérios para conta, publicações, imagens, sessões, logs e backups: [A CONFIRMAR].', 'A conta e seu conteúdo permanecem até exclusão manual pelo usuário, moderação ou obrigação legal. Não há exclusão automática por inatividade implementada. A infraestrutura está configurada com backup automático do PostgreSQL por 7 dias, soft delete de blobs por 30 dias e retenção padrão do Log Analytics por 30 dias; essas retenções técnicas não substituem a política de retenção dos dados da aplicação.')
WHERE slug = 'PRIVACY_POLICY';

UPDATE application.content_documents
SET body = replace(body, 'Regras de idade mínima, tratamento de dados de menores, cookies, armazenamento local e analytics: [A CONFIRMAR].', 'A idade mínima prevista é 18 anos. A data de nascimento será solicitada no cadastro e tratada como dado pessoal de acesso restrito. Não foram identificados cookies de rastreamento, analytics ou pixels; o navegador armazena dados técnicos necessários ao funcionamento, como sessão, preferências, dispositivo e rascunhos.')
WHERE slug = 'PRIVACY_POLICY';

UPDATE application.content_documents
SET body = replace(body, 'Dúvidas e solicitações: [A CONFIRMAR — canal de privacidade].', 'Dúvidas e solicitações: rumeniggxx@gmail.com.')
WHERE slug = 'PRIVACY_POLICY';

INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'a6ed8b40-6f9a-4be1-9f65-5d1f4a8a7c22',
    'Política de privacidade atualizada com dados da infraestrutura',
    'A minuta foi atualizada com o canal de contato, serviços Azure, retenções configuradas e regra planejada de idade mínima de 18 anos.',
    now()
)
ON CONFLICT (id) DO NOTHING;

UPDATE application.content_documents
SET body = replace(body, 'Transferência internacional ainda não confirmada.', 'Pode haver processamento ou transferência internacional: o frontend usa Azure Static Web Apps em East US 2, o código e CI/CD usam GitHub.com/GitHub Actions, e o Azure Communication Services informa que dados podem transitar ou ser processados por endpoints globais. A infraestrutura principal de API, banco, imagens e logs está em Brazil South.')
WHERE slug = 'PRIVACY_POLICY';
