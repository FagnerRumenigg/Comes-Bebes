INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'c2e7a914-5b36-4d80-9f21-6a4c8e1d7350',
    'Salvos agora são sua biblioteca pessoal',
    'A tela de salvos agora coloca suas publicações guardadas no centro da experiência, com coleções próprias e seguidas organizadas como apoio. Os textos reforçam a ideia de memória culinária e o estado vazio orienta a descobrir e guardar algo para voltar depois, sem alterar as ações existentes.',
    now()
)
ON CONFLICT (id) DO NOTHING;
