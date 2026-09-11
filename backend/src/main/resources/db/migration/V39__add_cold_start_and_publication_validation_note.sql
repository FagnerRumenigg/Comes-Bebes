INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '4e9c1a72-b6d5-4f03-8c21-7a5d2e9b6f48',
    'Publicações mais seguras e loading mais preciso',
    'A tela de servidor dormindo agora só aparece quando o health check confirma o cold start. Também reforçamos as validações de receitas e campos condicionais antes do envio da publicação.',
    now()
)
ON CONFLICT (id) DO NOTHING;
