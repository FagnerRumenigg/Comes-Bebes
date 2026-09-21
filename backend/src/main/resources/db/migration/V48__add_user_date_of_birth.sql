ALTER TABLE application.users
    ADD COLUMN IF NOT EXISTS date_of_birth date;

INSERT INTO application.patch_notes (id, title, body, created_at)
VALUES (
    'd9b7f3a2-8c4e-4a1b-9d6f-2e7c5b8a0f31',
    'Cadastro para maiores de 18 anos',
    'O cadastro agora solicita a data de nascimento e bloqueia contas de pessoas com menos de 18 anos.',
    now()
)
ON CONFLICT (id) DO NOTHING;
