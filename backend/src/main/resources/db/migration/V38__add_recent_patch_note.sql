INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '8a3b2d4e-7f61-4c92-a5e8-1d0b6f3c9a27',
    'Melhorias de publicação e estabilidade',
    'Corrigimos o enquadramento de fotos para se adaptar ao tamanho da tela, garantimos o salvamento do rascunho durante o aquecimento do servidor, adicionamos a exclusão de publicações diretamente pelo feed e melhoramos o aviso de reconexão.',
    now()
)
ON CONFLICT (id) DO NOTHING;
