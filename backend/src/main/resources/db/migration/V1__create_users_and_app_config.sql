CREATE TABLE application.users (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL,
    blocked_email_hmac char(64),
    password_hash varchar(255) NOT NULL,
    username varchar(30) NOT NULL,
    display_name varchar(100) NOT NULL,
    role varchar(20) NOT NULL DEFAULT 'USER',
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    blocked_by uuid REFERENCES application.users(id) ON DELETE SET NULL,
    blocked_at timestamptz,
    block_reason text,
    show_reaction_counts boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT users_role_ck CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT users_status_ck CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED')),
    CONSTRAINT users_block_reason_length_ck CHECK (block_reason IS NULL OR char_length(block_reason) <= 2000),
    CONSTRAINT users_blocked_fields_ck CHECK (
        (status = 'BLOCKED' AND blocked_by IS NOT NULL AND blocked_at IS NOT NULL AND block_reason IS NOT NULL AND char_length(btrim(block_reason)) > 0 AND blocked_email_hmac IS NOT NULL)
        OR
        (status <> 'BLOCKED')
    )
);

CREATE TABLE application.app_config (
    id smallint PRIMARY KEY,
    report_threshold integer NOT NULL DEFAULT 3,
    daily_report_limit integer NOT NULL DEFAULT 10,
    updated_by uuid REFERENCES application.users(id) ON DELETE SET NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT app_config_singleton_ck CHECK (id = 1),
    CONSTRAINT app_config_threshold_ck CHECK (report_threshold > 0),
    CONSTRAINT app_config_daily_limit_ck CHECK (daily_report_limit > 0)
);
