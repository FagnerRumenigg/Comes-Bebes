-- Fase 1 do plano de redefinição de UX (docs/PLANO_BACKEND_UX.md, item 1.4).
-- Canal mínimo de "acha que erramos" para foto recusada na validação (impl10.md v10 §21.2).
CREATE TABLE application.photo_validation_feedback (
    id uuid PRIMARY KEY,
    reporter_id uuid NOT NULL REFERENCES application.users(id),
    reason_code varchar(50) NOT NULL,
    comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT photo_validation_feedback_comment_length_ck CHECK (comment IS NULL OR char_length(comment) <= 1000)
);

CREATE INDEX photo_validation_feedback_reporter_id_idx ON application.photo_validation_feedback (reporter_id);
