-- Último resultado conocido de Mercado Pago cuando el cobro no está aprobado (reintento / UX tutor).

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS mercadopago_ultimo_estado VARCHAR(32);

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS mercadopago_ultimo_estado_detalle VARCHAR(120);

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS mercadopago_ultimo_estado_en TIMESTAMP;
