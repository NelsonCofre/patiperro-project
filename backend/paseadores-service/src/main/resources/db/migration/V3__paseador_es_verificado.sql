-- Badge público de verificación (boolean). Idempotente; alinea todas las filas desde el enum existente.
-- Orden seguro: columna → backfill total → DEFAULT → NOT NULL (mismo patrón que V1/V2).

ALTER TABLE paseador
    ADD COLUMN IF NOT EXISTS es_verificado BOOLEAN;

UPDATE paseador
SET es_verificado = (estado_verificacion_identidad = 'APROBADO');

ALTER TABLE paseador
    ALTER COLUMN es_verificado SET DEFAULT false;

ALTER TABLE paseador
    ALTER COLUMN es_verificado SET NOT NULL;
