CREATE TABLE application.patch_notes (
    id uuid PRIMARY KEY,
    title varchar(150) NOT NULL,
    body text NOT NULL,
    published_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT patch_notes_title_ck CHECK (char_length(btrim(title)) > 0),
    CONSTRAINT patch_notes_body_ck CHECK (char_length(btrim(body)) > 0)
);

CREATE INDEX patch_notes_published_at_idx
    ON application.patch_notes (published_at);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'application' AND table_name = 'users' AND column_name = 'last_seen_patch_note_at'
    ) THEN
        ALTER TABLE application.users ADD COLUMN last_seen_patch_note_at timestamptz;
        UPDATE application.users SET last_seen_patch_note_at = now() WHERE last_seen_patch_note_at IS NULL;
        ALTER TABLE application.users ALTER COLUMN last_seen_patch_note_at SET NOT NULL;
    END IF;
END $$;
