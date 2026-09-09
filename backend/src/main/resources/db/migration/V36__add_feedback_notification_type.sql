-- Aviso interno pra quem administra o app quando chega uma mensagem em
-- "Falar com a gente" (docs/PROGRESSO_TELAS.md) — reaproveita o mesmo
-- sistema de avisos da tela 12, sem preferência de usuário: é sempre ligado,
-- só é criado pra quem tem role ADMIN.
ALTER TABLE application.user_notifications
    DROP CONSTRAINT user_notifications_type_ck;

ALTER TABLE application.user_notifications
    ADD CONSTRAINT user_notifications_type_ck
        CHECK (type IN (
            'REPORT_REJECTED_WARNING', 'NEW_DEVICE_LOGIN', 'NEW_FOLLOWER', 'FOLLOWED_USER_PUBLISHED',
            'SAVED_YOUR_PUBLICATION', 'REACTED_TO_YOUR_PUBLICATION', 'MADE_YOUR_VERSION',
            'NEW_ITEM_IN_FOLLOWED_COLLECTION', 'COLLECTION_SHARED_WITH_YOU', 'NEW_FEEDBACK_RECEIVED'
        ));
