ALTER TABLE application.users
    ALTER COLUMN blocked_username_hmac TYPE varchar(64)
    USING btrim(blocked_username_hmac);
