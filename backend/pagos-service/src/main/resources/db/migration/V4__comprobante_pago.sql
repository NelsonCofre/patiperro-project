-- Resumen de transacción (comprobante informativo) por reserva.
-- Una fila por id_reserva para idempotencia post-pago.
CREATE TABLE IF NOT EXISTS comprobante_pago (
    id_comprobante         BIGSERIAL PRIMARY KEY,
    id_reserva             BIGINT NOT NULL,
    id_transaccion_pagos   BIGINT,
    id_transaccion_externa VARCHAR(128),
    fecha_hora_operacion   TIMESTAMP,
    paseador_nombre        VARCHAR(256),
    mascota_nombre         VARCHAR(256),
    fecha_paseo            DATE,
    hora_inicio            TIMESTAMP,
    hora_final             TIMESTAMP,
    duracion_minutos       BIGINT,
    moneda                 VARCHAR(8) NOT NULL DEFAULT 'CLP',
    monto_total            NUMERIC(14, 2),
    comision_app           NUMERIC(14, 2),
    monto_neto             NUMERIC(14, 2),
    estado_fondos          VARCHAR(512),
    tipo_documento         VARCHAR(64) NOT NULL DEFAULT 'RESUMEN_TRANSACCION',
    disclaimer_legal       VARCHAR(512),
    html_resumen           TEXT,
    json_snapshot          TEXT,
    creado_en              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_comprobante_pago_id_reserva UNIQUE (id_reserva)
);

CREATE INDEX IF NOT EXISTS idx_comprobante_pago_reserva
    ON comprobante_pago (id_reserva);

CREATE INDEX IF NOT EXISTS idx_comprobante_pago_creado_en
    ON comprobante_pago (creado_en DESC);
