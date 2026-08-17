CREATE TABLE application.user_device (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    device_hash varchar(64) NOT NULL,
    device_name varchar(100) NOT NULL,
    user_agent text,
    ip_address varchar(45),
    last_login_at timestamptz NOT NULL,
    last_activity_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    is_active boolean NOT NULL DEFAULT true,
    is_trusted boolean NOT NULL DEFAULT false,
    CONSTRAINT user_device_user_hash_uk UNIQUE (user_id, device_hash)
);

CREATE INDEX user_device_user_active_idx
    ON application.user_device (user_id, is_active);

ALTER TABLE application.refresh_tokens
    ADD COLUMN device_id uuid REFERENCES application.user_device(id) ON DELETE CASCADE;

CREATE INDEX refresh_tokens_device_idx
    ON application.refresh_tokens (device_id);

-- A notificação de novo dispositivo (IDEIA-019) não está ligada a um caso de moderação,
-- então essa coluna precisa deixar de ser obrigatória para o modelo continuar genérico.
ALTER TABLE application.user_notifications
    ALTER COLUMN moderation_case_id DROP NOT NULL;
