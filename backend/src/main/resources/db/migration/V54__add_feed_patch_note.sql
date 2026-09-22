INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '8b1d6f3a-2c74-4e90-a5b8-1f6c9d2e7043',
    'Feed mais claro para descobrir e participar',
    'O feed agora apresenta melhor a intenção de descoberta, resume a organização dos filtros e oferece um caminho para ver tudo quando uma combinação não encontra publicações. Os estados vazios ficaram mais acolhedores e mantêm a publicação como convite opcional, sem alterar a busca ou o carregamento do conteúdo.',
    now()
)
ON CONFLICT (id) DO NOTHING;
