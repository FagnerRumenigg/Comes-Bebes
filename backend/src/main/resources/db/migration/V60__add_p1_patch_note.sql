INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'c1f8d2a4-8b67-4d3e-a921-5e7c0b6f4a18',
    'Uma experiência mais clara do começo ao fim',
    'Nesta atualização, avançamos em toda a frente P1 do Comes&Bebes: navegação, boas-vindas e login mais claros; feed com filtros compactos e busca integrada à descoberta culinária; salvos e coleções mais fáceis de organizar, incluindo colaboração por convite; perfil, configurações, cadastro e publicação com mais segurança; detalhes e edição de publicações com ações hierarquizadas e confirmação de alterações; tela Seguindo orientada à descoberta; Termos de Serviço com sumário e âncoras; página 404 com caminhos úteis; e fila administrativa de feedback com categorias, status, filtros e triagem direta. Também reforçamos acessibilidade, estados vazios, mensagens de erro, confirmação de ações e consistência visual em diferentes tamanhos de tela.',
    now()
)
ON CONFLICT (id) DO NOTHING;
