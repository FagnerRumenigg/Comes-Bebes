INSERT INTO application.reaction_types (code, label, display_order)
VALUES
    ('WOULD_EAT', 'Eu comeria', 1),
    ('WANT_TO_MAKE', 'Quero fazer', 2),
    ('COMFORT_FOOD', 'Comida afetiva', 3);

INSERT INTO application.report_reasons (code, label, display_order)
VALUES
    ('NOT_FOOD', 'Não é conteúdo de comida', 1),
    ('IDENTIFIABLE_PERSON', 'Contém pessoa identificável', 2),
    ('OFFENSIVE_OR_DISCRIMINATORY', 'Conteúdo ofensivo ou discriminatório', 3),
    ('UNAUTHORIZED_ADVERTISING', 'Spam ou propaganda não autorizada', 4),
    ('AUTHORSHIP_VIOLATION', 'Violação de autoria', 5),
    ('DANGEROUS_OR_ILLEGAL', 'Conteúdo perigoso ou ilegal', 6);

INSERT INTO application.app_config (id) VALUES (1);
