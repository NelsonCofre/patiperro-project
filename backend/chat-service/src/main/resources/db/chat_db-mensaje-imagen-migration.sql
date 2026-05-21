-- Migración incremental: mensajes multimedia en chat_db (ejecutar conectado a chat_db).
-- Idempotente para re-ejecución en dev.

ALTER TABLE mensaje ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'TEXTO';
ALTER TABLE mensaje ADD COLUMN IF NOT EXISTS url_media VARCHAR(512);

ALTER TABLE mensaje ALTER COLUMN contenido DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_mensaje_imagen_conversacion
    ON mensaje (conversacion_id_conversacion, fecha_envio)
    WHERE tipo = 'IMAGEN';
