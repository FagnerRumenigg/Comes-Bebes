-- Fase 2 do plano de redefinição de UX (docs/PLANO_BACKEND_UX.md, item 2.1).
-- Terceiro nível de visibilidade de publicação: "Só para mim" (produto5.md v5 §6.4).
ALTER TABLE application.publications DROP CONSTRAINT publications_visibility_ck;
ALTER TABLE application.publications ADD CONSTRAINT publications_visibility_ck
    CHECK (visibility IN ('PUBLIC', 'INTERNAL', 'PRIVATE'));
