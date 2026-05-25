-- Historia 2: fotos en chat. Idempotente para BD creada antes de Flyway o con chat_db-init.sql antiguo.

ALTER TABLE mensaje
    ADD COLUMN IF NOT EXISTS tipo VARCHAR(20);

UPDATE mensaje
SET tipo = 'TEXTO'
WHERE tipo IS NULL
   OR TRIM(tipo) NOT IN ('TEXTO', 'IMAGEN');

ALTER TABLE mensaje
    ALTER COLUMN tipo SET DEFAULT 'TEXTO';

ALTER TABLE mensaje
    ALTER COLUMN tipo SET NOT NULL;

ALTER TABLE mensaje
    ADD COLUMN IF NOT EXISTS url_media VARCHAR(512);

ALTER TABLE mensaje
    ALTER COLUMN contenido DROP NOT NULL;
