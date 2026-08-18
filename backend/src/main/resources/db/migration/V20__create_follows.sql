CREATE TABLE application.follows (
    follower_id uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    followed_id uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT follows_no_self_follow_ck CHECK (follower_id != followed_id)
);

CREATE INDEX follows_followers_idx ON application.follows (followed_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX follows_following_idx ON application.follows (follower_id, created_at DESC) WHERE deleted_at IS NULL;

-- Coluna genérica de "quem fez a ação", ausente até aqui porque só existiam notificações
-- sem ator (REPORT_REJECTED_WARNING, NEW_DEVICE_LOGIN). NEW_FOLLOWER precisa dela, e a
-- futura notificação de "publicou" (IDEIA-015) também vai reaproveitar.
ALTER TABLE application.user_notifications
    ADD COLUMN actor_id uuid REFERENCES application.users(id) ON DELETE SET NULL;

ALTER TABLE application.user_notifications
    DROP CONSTRAINT user_notifications_type_ck;

ALTER TABLE application.user_notifications
    ADD CONSTRAINT user_notifications_type_ck
        CHECK (type IN ('REPORT_REJECTED_WARNING', 'NEW_DEVICE_LOGIN', 'NEW_FOLLOWER'));
