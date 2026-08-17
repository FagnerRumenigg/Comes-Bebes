-- Data/hora em que a foto foi tirada, lida do EXIF pelo image-validator antes de removê-lo.
-- Opcional: a maioria dos prints, PNGs e fotos reenviadas por apps que removem metadados
-- (ex.: WhatsApp) não carrega essa informação.
ALTER TABLE application.publications
    ADD COLUMN photo_taken_at timestamptz;
