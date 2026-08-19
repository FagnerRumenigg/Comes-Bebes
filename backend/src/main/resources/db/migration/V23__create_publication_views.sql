CREATE TABLE application.publication_views (
    user_id        uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    publication_id uuid NOT NULL REFERENCES application.publications(id) ON DELETE CASCADE,
    viewed_at      timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, publication_id)
);
