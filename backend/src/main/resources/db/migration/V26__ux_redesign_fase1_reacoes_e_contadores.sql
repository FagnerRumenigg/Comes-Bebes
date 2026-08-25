-- Fase 1 do plano de redefinição de UX (docs/PLANO_BACKEND_UX.md).

-- 1.1: catálogo de reações fechado em 9 (produto5.md v5 §6.1 / impl10.md v10 §15.1).
-- Desativa, não apaga: publicações antigas podem ter reações registradas nestes tipos,
-- e quem reagiu antes continua podendo remover a própria reação (ver ReactionTypeRepository).
UPDATE application.reaction_types
SET active = false
WHERE code IN ('WOULD_EAT', 'WANT_TO_MAKE', 'COMFORT_FOOD');

-- 1.2: "nenhum contador público em lugar nenhum do produto" (produto5.md v5 §3.1).
-- A preferência de mostrar/ocultar a contagem de reações não faz mais sentido:
-- a contagem nunca aparece para ninguém, autor incluso.
ALTER TABLE application.users DROP COLUMN show_reaction_counts;
