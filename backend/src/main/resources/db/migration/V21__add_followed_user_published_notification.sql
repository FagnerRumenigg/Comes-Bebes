ALTER TABLE application.users
    ADD COLUMN notify_on_followed_publish boolean NOT NULL DEFAULT true;

ALTER TABLE application.user_notifications
    DROP CONSTRAINT user_notifications_type_ck;

ALTER TABLE application.user_notifications
    ADD CONSTRAINT user_notifications_type_ck
        CHECK (type IN ('REPORT_REJECTED_WARNING', 'NEW_DEVICE_LOGIN', 'NEW_FOLLOWER', 'FOLLOWED_USER_PUBLISHED'));
