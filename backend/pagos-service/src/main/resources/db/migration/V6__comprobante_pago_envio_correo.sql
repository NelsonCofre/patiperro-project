-- Deduplicación de envío de correo del comprobante: evita spam por reintentos.
ALTER TABLE comprobante_pago
    ADD COLUMN IF NOT EXISTS email_destino VARCHAR(320);

ALTER TABLE comprobante_pago
    ADD COLUMN IF NOT EXISTS email_enviado_en TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_comprobante_pago_email_enviado_en
    ON comprobante_pago (email_enviado_en DESC);

