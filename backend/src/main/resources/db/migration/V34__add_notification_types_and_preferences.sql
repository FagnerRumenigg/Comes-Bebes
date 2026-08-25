-- Preferências de aviso por tipo (docs/telas/09-configuracoes.html, seção "Avisos").
-- Segue o padrão de notify_on_followed_publish (V21): boolean direto em users.
ALTER TABLE application.users
    ADD COLUMN notify_on_saved boolean NOT NULL DEFAULT true,
    ADD COLUMN notify_on_reacted boolean NOT NULL DEFAULT true,
    ADD COLUMN notify_on_my_version boolean NOT NULL DEFAULT true,
    ADD COLUMN notify_on_collection_new_item boolean NOT NULL DEFAULT true,
    ADD COLUMN notify_on_collection_shared boolean NOT NULL DEFAULT true,
    ADD COLUMN notify_weekly_email boolean NOT NULL DEFAULT false;

-- A referência mostra "Quando alguém que você segue publica" desligado por padrão
-- (diferente do default true que V21 usou) — corrige só pra quem se cadastrar
-- daqui pra frente; quem já tem a preferência ativa continua como está.
ALTER TABLE application.users
    ALTER COLUMN notify_on_followed_publish SET DEFAULT false;

-- Aviso pode se referir a uma coleção (coisa nova numa que você segue, ou
-- compartilharam uma com você), não só a uma publicação.
ALTER TABLE application.user_notifications
    ADD COLUMN collection_id uuid REFERENCES application.publication_collections(id) ON DELETE CASCADE;

ALTER TABLE application.user_notifications
    DROP CONSTRAINT user_notifications_type_ck;

ALTER TABLE application.user_notifications
    ADD CONSTRAINT user_notifications_type_ck
        CHECK (type IN (
            'REPORT_REJECTED_WARNING', 'NEW_DEVICE_LOGIN', 'NEW_FOLLOWER', 'FOLLOWED_USER_PUBLISHED',
            'SAVED_YOUR_PUBLICATION', 'REACTED_TO_YOUR_PUBLICATION', 'MADE_YOUR_VERSION',
            'NEW_ITEM_IN_FOLLOWED_COLLECTION', 'COLLECTION_SHARED_WITH_YOU'
        ));
