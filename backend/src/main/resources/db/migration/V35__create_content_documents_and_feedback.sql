-- Textos de Termos de Serviço / Política de Privacidade / FAQ (docs/telas/09-configuracoes.html,
-- seção "Ajuda e sobre") e o link "Ajuda" do rodapé de login/cadastro
-- (AuthLayout.vue) — sem endpoint de escrita, mesmo padrão de patch_notes
-- (V13): as linhas são mantidas direto no banco.
CREATE TABLE application.content_documents (
    id uuid PRIMARY KEY,
    slug varchar(40) NOT NULL,
    title varchar(150) NOT NULL,
    body text NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT content_documents_slug_ck CHECK (char_length(btrim(slug)) > 0),
    CONSTRAINT content_documents_slug_uk UNIQUE (slug)
);

-- Conteúdo é rascunho placeholder (parágrafos separados por linha em branco) —
-- precisa ser revisado e substituído pelo texto jurídico definitivo antes de
-- valer pra valer.
INSERT INTO application.content_documents (id, slug, title, body) VALUES
(gen_random_uuid(), 'TERMS_OF_SERVICE', 'Termos de Serviço', E'Estes Termos de Serviço (\"Termos\") regem o uso do Comes&Bebes. Ao criar uma conta ou usar o aplicativo, você concorda com eles.\n\nEsta é uma versão preliminar de rascunho, ainda não revisada juridicamente. Ela existe para que a tela de Termos de Serviço tenha conteúdo real enquanto o texto definitivo não é escrito.\n\n1. Sobre o serviço. O Comes&Bebes é uma rede social para publicar, guardar e descobrir receitas e fotos de comida.\n\n2. Sua conta. Você é responsável por manter sua senha em segurança e por tudo que acontece através da sua conta. Avise a gente se perceber acesso não autorizado.\n\n3. Conteúdo que você publica. Você continua dono do que publica, mas nos dá permissão para exibir, distribuir e promover esse conteúdo dentro do aplicativo. Não publique nada que você não tenha o direito de compartilhar.\n\n4. Comportamento na comunidade. Não é permitido assédio, discurso de ódio, spam ou conteúdo enganoso. Publicações denunciadas passam por moderação e podem ser removidas.\n\n5. Encerramento de conta. Você pode encerrar sua conta quando quiser. Podemos suspender ou encerrar contas que violem estes Termos.\n\n6. Alterações. Podemos atualizar estes Termos de tempos em tempos. Mudanças relevantes serão avisadas dentro do aplicativo.\n\n7. Contato. Dúvidas sobre estes Termos podem ser enviadas pela tela \"Falar com a gente\", em Configurações → Ajuda e sobre.'),
(gen_random_uuid(), 'PRIVACY_POLICY', 'Política de Privacidade', E'Esta Política de Privacidade explica quais dados o Comes&Bebes coleta e como eles são usados.\n\nEsta é uma versão preliminar de rascunho, ainda não revisada juridicamente. Ela existe para que a tela de Política de Privacidade tenha conteúdo real enquanto o texto definitivo não é escrito.\n\n1. Dados que coletamos. Nome de exibição, @usuário, e-mail (quando informado), senha (sempre armazenada de forma criptografada), fotos e textos que você publica, e informações técnicas básicas de acesso (como dispositivo e horário de login) para manter sua conta segura.\n\n2. Como usamos esses dados. Para operar o aplicativo, mostrar seu feed e perfil, avisar sobre atividade relevante para você (como quem reagiu ou guardou algo que você publicou) e, quando você autoriza, enviar um resumo por e-mail.\n\n3. Com quem compartilhamos. Não vendemos seus dados. Usamos provedores de infraestrutura (hospedagem, armazenamento de imagens, envio de e-mail) só para operar o serviço.\n\n4. Suas escolhas. Você pode editar seu perfil, mudar a visibilidade das suas publicações, desligar avisos específicos em Configurações → Avisos e apagar sua conta quando quiser.\n\n5. Segurança. Senhas são armazenadas com hash, sessões usam tokens com expiração, e você pode revogar o acesso de qualquer aparelho em Configurações → Entrar e aparelhos.\n\n6. Alterações. Podemos atualizar esta Política de tempos em tempos. Mudanças relevantes serão avisadas dentro do aplicativo.\n\n7. Contato. Dúvidas sobre privacidade podem ser enviadas pela tela \"Falar com a gente\", em Configurações → Ajuda e sobre.'),
(gen_random_uuid(), 'FAQ', 'Como usar o Comes&Bebes', E'Perguntas comuns sobre o Comes&Bebes, explicadas com calma.\n\nComo eu publico uma receita ou uma foto? Toque em \"Publicar\" no menu principal, escolha uma foto e preencha o que quiser contar sobre o prato.\n\nO que é \"minha versão\"? É quando você faz a receita de outra pessoa do seu jeito e publica a sua versão, mantendo a ligação com a receita original.\n\nComo funcionam as coleções? Coleções juntam publicações por tema (por exemplo, \"Doces de festa\"). Podem ser só suas, públicas, ou compartilhadas só com quem você escolher.\n\nQuem pode ver o que eu publico? Você escolhe a visibilidade de cada publicação: pública, só para quem você segue, ou só para você.\n\nComo eu paro de receber um tipo de aviso? Em Configurações → Avisos, cada tipo de aviso tem um interruptor próprio.\n\nComo eu apago minha conta? Em Configurações → Minha conta há a opção de encerrar a conta.\n\nNão achei resposta pra minha dúvida. Use \"Falar com a gente\", logo abaixo, pra escrever pra gente diretamente.');

-- Sugestões enviadas em "Falar com a gente" (docs/telas/09-configuracoes.html)
-- — sem tela de leitura por enquanto, só guardadas no banco.
CREATE TABLE application.feedback_submissions (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES application.users(id),
    message text NOT NULL,
    contact_email varchar(320),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT feedback_submissions_message_ck CHECK (char_length(btrim(message)) > 0)
);

CREATE INDEX feedback_submissions_created_at_idx
    ON application.feedback_submissions (created_at);
