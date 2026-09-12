INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'c1e5f20a-6a4c-4ebf-8d57-7d39f3e0a641',
    'Aquecimento do validador de fotos',
    'Após entrar na conta, o validador de imagens é aquecido para reduzir a espera na primeira publicação.',
    now()
)
ON CONFLICT (id) DO NOTHING;
