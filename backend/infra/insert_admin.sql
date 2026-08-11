-- Insert admin user: username=admin, display_name=Alluka
-- Generated on 2026-08-10

INSERT INTO application.users (id, email, password_hash, username, display_name, role, status, show_reaction_counts, created_at, updated_at)
VALUES (
  '440a898a-7730-41b0-96d6-fa64f762007f',
  'admin@example.test',
  '$2b$12$F/jzfAo9ULtSb0wtFgtzlO3yPNqK6mIKCK3oBL3PJ8qrcZPIE3mmq',
  'admin',
  'Alluka',
  'ADMIN',
  'ACTIVE',
  TRUE,
  now(),
  now()
);
