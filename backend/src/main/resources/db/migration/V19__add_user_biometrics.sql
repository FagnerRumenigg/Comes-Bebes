CREATE TABLE application.user_biometric (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    device_id uuid NOT NULL REFERENCES application.user_device(id) ON DELETE CASCADE,
    biometric_type varchar(20) NOT NULL,
    credential_id bytea NOT NULL,
    public_key_cose bytea NOT NULL,
    signature_count bigint NOT NULL DEFAULT 0,
    registered_at timestamptz NOT NULL DEFAULT now(),
    last_used_at timestamptz,
    is_active boolean NOT NULL DEFAULT true,
    CONSTRAINT user_biometric_credential_id_uk UNIQUE (credential_id)
);

CREATE INDEX user_biometric_user_active_idx
    ON application.user_biometric (user_id, is_active);

CREATE INDEX user_biometric_device_active_idx
    ON application.user_biometric (device_id, is_active);
