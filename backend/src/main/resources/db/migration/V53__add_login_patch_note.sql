INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    '4a8e2c1f-7d53-49b6-a0e2-6c9f1b4d8357',
    'Login conectado às suas descobertas culinárias',
    'A tela de login agora reforça a continuidade da experiência: reencontrar o que foi salvo, registrar novas descobertas e manter uma memória culinária. A linguagem do painel editorial e do formulário foi aproximada, sem alterar os fluxos de senha, biometria, recuperação de acesso ou cadastro.',
    now()
)
ON CONFLICT (id) DO NOTHING;
