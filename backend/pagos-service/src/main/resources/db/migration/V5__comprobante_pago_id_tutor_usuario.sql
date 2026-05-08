-- Complemento seguro: agregar owner para anti-IDOR sin red.
ALTER TABLE comprobante_pago
    ADD COLUMN IF NOT EXISTS id_tutor_usuario BIGINT;

CREATE INDEX IF NOT EXISTS idx_comprobante_pago_tutor
    ON comprobante_pago (id_tutor_usuario);

