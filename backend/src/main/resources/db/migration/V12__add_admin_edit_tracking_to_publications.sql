DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'application' AND table_name = 'publications' AND column_name = 'edited_by_admin_id'
    ) THEN
        ALTER TABLE application.publications ADD COLUMN edited_by_admin_id uuid REFERENCES application.users(id);
        ALTER TABLE application.publications ADD COLUMN edited_by_admin_at timestamptz;
    END IF;
END $$;
