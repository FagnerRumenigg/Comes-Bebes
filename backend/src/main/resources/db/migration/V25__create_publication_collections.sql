CREATE TABLE application.publication_collections (
    id          uuid PRIMARY KEY,
    author_id   uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    name        varchar(80) NOT NULL,
    description varchar(280),
    visibility  varchar(10) NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    deleted_at  timestamptz,
    CONSTRAINT publication_collections_name_ck CHECK (char_length(btrim(name)) > 0),
    CONSTRAINT publication_collections_visibility_ck CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

CREATE INDEX publication_collections_author_idx
    ON application.publication_collections (author_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- Sem soft-delete (mesmo padrão de application.publication_tags): remoção de
-- uma publicação da coleção é só deletar a linha.
CREATE TABLE application.collection_publications (
    collection_id  uuid NOT NULL REFERENCES application.publication_collections(id) ON DELETE CASCADE,
    publication_id uuid NOT NULL REFERENCES application.publications(id) ON DELETE CASCADE,
    position       smallint NOT NULL,
    added_at       timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (collection_id, publication_id)
);

CREATE INDEX collection_publications_order_idx
    ON application.collection_publications (collection_id, position);

CREATE TABLE application.collection_follows (
    follower_id   uuid NOT NULL REFERENCES application.users(id) ON DELETE CASCADE,
    collection_id uuid NOT NULL REFERENCES application.publication_collections(id) ON DELETE CASCADE,
    created_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz,
    PRIMARY KEY (follower_id, collection_id)
);

CREATE INDEX collection_follows_collection_idx
    ON application.collection_follows (collection_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX collection_follows_follower_idx
    ON application.collection_follows (follower_id, created_at DESC)
    WHERE deleted_at IS NULL;
