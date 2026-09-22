INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'f4a82c19-6e35-4b70-9d24-1c7a5e803b62',
    'Coleções com ações mais claras e seguras',
    'A tela de coleção agora organiza as ações conforme o contexto: compartilhar e definir quem pode ver ficam em destaque, enquanto edição e exclusão permanecem separadas. O cabeçalho reforça a autoria e a curadoria, e as mensagens de privacidade explicam melhor o impacto de cada mudança de acesso.',
    now()
)
ON CONFLICT (id) DO NOTHING;
