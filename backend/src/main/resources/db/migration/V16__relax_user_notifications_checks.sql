-- V15 removeu o NOT NULL de moderation_case_id, mas duas CHECK constraints da V4 continuavam
-- barrando notificações sem caso de moderação (user_notifications_origin_ck) e sem o tipo
-- REPORT_REJECTED_WARNING (user_notifications_type_ck) — ambas ficaram esquecidas na V15.

ALTER TABLE application.user_notifications
    DROP CONSTRAINT user_notifications_origin_ck;

ALTER TABLE application.user_notifications
    DROP CONSTRAINT user_notifications_type_ck;

ALTER TABLE application.user_notifications
    ADD CONSTRAINT user_notifications_type_ck
        CHECK (type IN ('REPORT_REJECTED_WARNING', 'NEW_DEVICE_LOGIN'));
