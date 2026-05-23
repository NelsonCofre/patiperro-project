-- Reparación idempotente si V1 se aplicó en una versión anterior sin normalización previa al CHECK,
-- o si Hibernate volvió a dejar NULL/valores fuera del enum. Seguro re-ejecutar (no-op si ya está bien).

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
