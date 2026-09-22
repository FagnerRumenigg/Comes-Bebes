INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '6d3f8a21-4c79-45b0-ae62-9f1c7d5038b4',
    'Perfis agora destacam melhor a história culinária',
    'A tela de perfil passou a priorizar publicações e descobertas culinárias antes das métricas sociais. O cabeçalho reforça o perfil como um caderno de descobertas, mantendo claras as ações de editar, seguir e consultar vínculos.',
    now()
)
ON CONFLICT (id) DO NOTHING;
