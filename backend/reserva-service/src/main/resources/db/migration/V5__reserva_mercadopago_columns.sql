-- Alinea tablas existentes con el modelo JPA (BD creadas sin V1 completo o dumps antiguos).

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS mercadopago_payment_id VARCHAR(64);

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS mercadopago_reembolso_procesado_en TIMESTAMP;
