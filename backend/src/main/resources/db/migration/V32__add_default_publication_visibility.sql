ALTER TABLE application.users
    ADD COLUMN default_publication_visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';
