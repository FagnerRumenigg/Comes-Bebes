UPDATE application.content_documents
SET body = replace(
        replace(
          replace(
            replace(body, 'POLÍTICA DE PRIVACIDADE — MINUTA PARA REVISÃO', 'POLÍTICA DE PRIVACIDADE'),
            'Versão: 0.2 — minuta', 'Versão: 1.0 — vigente'),
          'Este documento explica como o Comes&Bebes trata dados pessoais. Foi estruturado com base na LGPD e nas orientações da ANPD, mas precisa de validação factual e revisão jurídica antes de ser considerado a versão oficial.',
          'Este documento explica como o Comes&Bebes trata dados pessoais, com base na LGPD e nas orientações públicas da ANPD.'),
        'A idade mínima prevista é 18 anos.', 'A idade mínima para criar uma conta é 18 anos.'),
    updated_at = now()
WHERE slug = 'PRIVACY_POLICY';

INSERT INTO application.patch_notes (id, title, body, published_at)
VALUES (
    'f2c8a1d4-7b6e-4c3f-9a20-5d8e1b7c4f62',
    'Política de privacidade publicada',
    'A política de privacidade passou de minuta para a versão 1.0 vigente, com informações de contato, infraestrutura, retenção e idade mínima.',
    now()
)
ON CONFLICT (id) DO NOTHING;
