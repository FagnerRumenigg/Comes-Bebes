INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '4f6b6b4b-0bd2-4dc6-b5d4-37c2a8c2d1a9',
    'Ajuste no monitoramento do validador',
    'Ajustamos o deploy do validador, que possui acesso interno e não pode ser testado por HTTP externo pelo GitHub Actions.',
    now()
)
ON CONFLICT (id) DO NOTHING;
