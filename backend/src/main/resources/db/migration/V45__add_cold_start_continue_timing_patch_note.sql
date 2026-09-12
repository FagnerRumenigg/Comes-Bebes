INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'f3bf7d2e-a8a5-4db4-9ce0-5d90f7e2198a',
    'Mais tempo para continuar após o aquecimento',
    'A tela de aquecimento agora permanece disponível por 30 segundos depois que o servidor volta, permitindo continuar pelo botão ou aguardar a contagem terminar.',
    now()
)
ON CONFLICT (id) DO NOTHING;
