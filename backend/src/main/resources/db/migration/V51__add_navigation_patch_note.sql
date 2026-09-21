INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '1f6b3d9a-2c84-4e71-a5f0-8d3c7b2e9146',
    'Menus de navegação sem recortes',
    'Corrigimos o comportamento do menu da conta na navegação web para garantir que dropdowns e outros menus posicionados sejam exibidos por completo, sem serem cortados pelos limites do header ou do container visual. A navegação mobile continua utilizando a folha inferior normalmente.',
    now()
)
ON CONFLICT (id) DO NOTHING;
