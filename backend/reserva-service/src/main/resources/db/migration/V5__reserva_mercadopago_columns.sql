-- Alinea tablas existentes con el modelo JPA (BD creadas sin V1 completo o dumps antiguos).
-- Versión V5: se renombró desde V4 para evitar conflicto con otro script V4 en el mismo módulo.

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS mercadopago_payment_id VARCHAR(64);

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS mercadopago_reembolso_procesado_en TIMESTAMP;
