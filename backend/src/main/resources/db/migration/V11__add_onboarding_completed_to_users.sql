DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'application'
          AND table_name = 'users'
          AND column_name = 'onboarding_completed'
    ) THEN
        ALTER TABLE application.users
        ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;
