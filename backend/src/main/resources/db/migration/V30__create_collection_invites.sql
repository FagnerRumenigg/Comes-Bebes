-- Fase 3 do plano de redefinição de UX (docs/PLANO_BACKEND_UX.md, item 3.1).
-- Terceiro nível de coleção: "Para quem eu escolher" (produto5.md v5 §6.3, impl10.md v10 §13.9).
ALTER TABLE application.publication_collections DROP CONSTRAINT publication_collections_visibility_ck;
ALTER TABLE application.publication_collections ADD CONSTRAINT publication_collections_visibility_ck
    CHECK (visibility IN ('PUBLIC', 'SHARED', 'PRIVATE'));

-- Convite por link secreto, não por @usuário (impl10.md v10 §13.4-C). "Gerar novo link"
-- revoga o anterior em vez de acumular; aceitar o convite vira um CollectionFollow comum,
-- reaproveitando a mesma lista de "quem pode ver" que já existe para coleções públicas.
CREATE TABLE application.collection_invites (
    id uuid PRIMARY KEY,
    collection_id uuid NOT NULL REFERENCES application.publication_collections(id),
    token varchar(43) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz
);

CREATE UNIQUE INDEX collection_invites_token_idx ON application.collection_invites (token);
CREATE INDEX collection_invites_collection_id_idx ON application.collection_invites (collection_id);
