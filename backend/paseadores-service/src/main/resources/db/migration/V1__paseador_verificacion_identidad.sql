-- Verificación de identidad del paseador (cédula). Idempotente para BD ya migrada por Hibernate ddl-auto=update.
-- Orden seguro: columnas → normalizar datos → NOT NULL/DEFAULT → CHECK (evita fallo si Hibernate dejó NULL/valores inválidos).

ALTER TABLE paseador
    ADD COLUMN IF NOT EXISTS estado_verificacion_identidad VARCHAR(20),
    ADD COLUMN IF NOT EXISTS archivo_cedula_frontal VARCHAR(255),
    ADD COLUMN IF NOT EXISTS archivo_cedula_reverso VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verificacion_identidad_enviada_en TIMESTAMP,
    ADD COLUMN IF NOT EXISTS verificacion_identidad_revisada_en TIMESTAMP,
    ADD COLUMN IF NOT EXISTS motivo_rechazo_verificacion_identidad VARCHAR(500);

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
