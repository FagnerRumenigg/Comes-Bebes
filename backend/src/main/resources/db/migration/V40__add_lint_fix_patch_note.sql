INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '6d2f9b41-c8e7-4a05-b316-9f72d4e1a850',
    'Ajuste no pipeline do frontend',
    'Corrigimos uma configuração de lint que impedia a validação automática do frontend durante o deploy.',
    now()
)
ON CONFLICT (id) DO NOTHING;
