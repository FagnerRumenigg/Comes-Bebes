ALTER TABLE application.users
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE application.users
    RENAME COLUMN blocked_email_hmac TO blocked_username_hmac;

ALTER INDEX application.users_blocked_email_hmac_uq
    RENAME TO users_blocked_username_hmac_uq;
