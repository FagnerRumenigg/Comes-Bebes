CREATE TABLE application.password_reset_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX password_reset_tokens_user_idx
    ON application.password_reset_tokens (user_id, expires_at);
