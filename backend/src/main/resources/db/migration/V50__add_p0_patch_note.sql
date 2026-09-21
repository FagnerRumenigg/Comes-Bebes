INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'c7a1e2f4-9b63-4d85-a017-6e4f2c8b9d31',
    'Mais segurança e previsibilidade nas áreas críticas',
    'Concluímos uma rodada de melhorias prioritárias para tornar o Comes&Bebes mais seguro e confiável. Rascunhos agora podem ser retomados com mais clareza, o fluxo de publicação informa melhor seu progresso, e o cadastro passou a validar a idade mínima de 18 anos. Também fortalecemos a proteção de sessões e dispositivos, publicamos a política de privacidade atualizada e aprimoramos a moderação: denúncias e evidências ficam mais claras, decisões destrutivas exigem confirmação e os casos mais urgentes aparecem primeiro na fila administrativa.',
    now()
)
ON CONFLICT (id) DO NOTHING;
