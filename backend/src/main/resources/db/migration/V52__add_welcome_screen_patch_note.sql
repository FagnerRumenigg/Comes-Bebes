INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '8e4c1a7b-5d92-46f0-b3e8-1c7a9d2f6045',
    'Entrada mais direta para descobrir e guardar comida',
    'A tela de boas-vindas agora leva mais rapidamente à exploração do produto e apresenta com clareza a proposta de descobrir, guardar e registrar experiências culinárias. Também reforçamos o foco em comida e inspiração, sem transformar a experiência em uma rede social de plateia.',
    now()
)
ON CONFLICT (id) DO NOTHING;
