-- Verificación de identidad del paseador (cédula). Idempotente para BD ya migrada por Hibernate ddl-auto=update.
ALTER TABLE paseador
    ADD COLUMN IF NOT EXISTS estado_verificacion_identidad VARCHAR(20) NOT NULL DEFAULT 'SIN_ENVIAR',
    ADD COLUMN IF NOT EXISTS archivo_cedula_frontal VARCHAR(255),
    ADD COLUMN IF NOT EXISTS archivo_cedula_reverso VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verificacion_identidad_enviada_en TIMESTAMP,
    ADD COLUMN IF NOT EXISTS verificacion_identidad_revisada_en TIMESTAMP,
    ADD COLUMN IF NOT EXISTS motivo_rechazo_verificacion_identidad VARCHAR(500);

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
