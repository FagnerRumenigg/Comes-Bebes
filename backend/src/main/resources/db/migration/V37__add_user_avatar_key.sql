-- Avatares de cozinha (docs/PROGRESSO_TELAS.md) — desenho escolhido em vez da
-- inicial do nome. Nulo = continua mostrando a inicial (comportamento atual).
ALTER TABLE application.users
    ADD COLUMN avatar_key varchar(30);

ALTER TABLE application.users
    ADD CONSTRAINT users_avatar_key_ck
        CHECK (avatar_key IS NULL OR avatar_key IN (
            'PANELA', 'COLHER_DE_PAU', 'XICARA', 'BOLO', 'PAO', 'TOMATE',
            'MILHO', 'LIMAO', 'TALHERES', 'PIMENTA', 'OVO', 'ABACAXI'
        ));
