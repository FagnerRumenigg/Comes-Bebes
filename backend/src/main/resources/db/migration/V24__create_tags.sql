CREATE TABLE application.tags (
    id uuid PRIMARY KEY,
    name varchar(40) NOT NULL,
    slug varchar(40) NOT NULL UNIQUE,
    official boolean NOT NULL DEFAULT false,
    created_by uuid REFERENCES application.users(id) ON DELETE SET NULL,
    -- Preparado para merge futuro de tags duplicadas: quando preenchido, esta tag foi
    -- substituída pela apontada aqui e deixa de ser sugerida no autocomplete.
    merged_into_tag_id uuid REFERENCES application.tags(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT tags_name_ck CHECK (char_length(btrim(name)) > 0),
    CONSTRAINT tags_slug_ck CHECK (char_length(btrim(slug)) > 0)
);

CREATE INDEX tags_slug_prefix_idx ON application.tags (slug varchar_pattern_ops) WHERE merged_into_tag_id IS NULL;

CREATE TABLE application.publication_tags (
    publication_id uuid NOT NULL REFERENCES application.publications(id) ON DELETE CASCADE,
    tag_id uuid NOT NULL REFERENCES application.tags(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (publication_id, tag_id)
);

CREATE INDEX publication_tags_tag_idx ON application.publication_tags (tag_id);
