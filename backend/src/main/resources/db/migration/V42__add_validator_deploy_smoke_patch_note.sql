INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '91d16e72-7f80-4a9e-a4dd-8e6e7a5e6db7',
    'Correção do deploy do validador de imagens',
    'Corrigimos o teste automático de disponibilidade do validador para usar o endpoint de saúde correto.',
    now()
)
ON CONFLICT (id) DO NOTHING;
