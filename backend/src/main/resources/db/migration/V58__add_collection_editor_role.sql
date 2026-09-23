ALTER TABLE application.collection_follows
    ADD COLUMN access_role varchar(16) NOT NULL DEFAULT 'VIEWER';

ALTER TABLE application.collection_follows
    ADD CONSTRAINT collection_follows_access_role_ck
    CHECK (access_role IN ('VIEWER', 'EDITOR'));
