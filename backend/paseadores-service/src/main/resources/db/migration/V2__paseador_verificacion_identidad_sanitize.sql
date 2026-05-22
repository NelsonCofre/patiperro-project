-- Normaliza estado_verificacion_identidad si Hibernate creó la columna sin DEFAULT/CHECK
-- (V1 usa ADD COLUMN IF NOT EXISTS y puede omitir columnas ya existentes).
-- Idempotente: seguro en BD nueva (post-V1) y en BD migrada solo con Hibernate.

UPDATE paseador
SET estado_verificacion_identidad = 'SIN_ENVIAR'
WHERE estado_verificacion_identidad IS NULL
   OR TRIM(estado_verificacion_identidad) NOT IN (
       'SIN_ENVIAR',
       'EN_PROCESO',
       'APROBADO',
       'RECHAZADO'
   );

ALTER TABLE paseador
    ALTER COLUMN estado_verificacion_identidad SET DEFAULT 'SIN_ENVIAR';

ALTER TABLE paseador
    ALTER COLUMN estado_verificacion_identidad SET NOT NULL;

ALTER TABLE paseador
    DROP CONSTRAINT IF EXISTS chk_paseador_estado_verificacion_identidad;

ALTER TABLE paseador
    ADD CONSTRAINT chk_paseador_estado_verificacion_identidad
        CHECK (estado_verificacion_identidad IN (
            'SIN_ENVIAR',
            'EN_PROCESO',
            'APROBADO',
            'RECHAZADO'
        ));
