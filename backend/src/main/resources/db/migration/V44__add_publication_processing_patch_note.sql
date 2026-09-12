INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'e6a3b9e0-5fb8-4ce3-a4dc-8e4a64e0ef83',
    'Processamento claro ao publicar',
    'Enquanto a foto é analisada, mostramos uma tela de processamento com o bule e curiosidades de culinária. Falhas transitórias do validador também são tentadas novamente automaticamente.',
    now()
)
ON CONFLICT (id) DO NOTHING;
