-- Resumen de transacción persistido post-pago (idempotente por reserva).
CREATE TABLE IF NOT EXISTS comprobante_pago (
    id_comprobante BIGSERIAL PRIMARY KEY,
    id_reserva BIGINT NOT NULL,
    id_transaccion BIGINT NOT NULL,
    id_tutor_usuario BIGINT NOT NULL,
    snapshot_json TEXT NOT NULL,
    correo_enviado_en TIMESTAMP,
    fecha_generacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comprobante_pago_reserva UNIQUE (id_reserva),
    CONSTRAINT fk_comprobante_pago_transaccion FOREIGN KEY (id_transaccion)
        REFERENCES transaccion (id_transaccion)
);

CREATE INDEX IF NOT EXISTS idx_comprobante_pago_id_tutor_usuario ON comprobante_pago (id_tutor_usuario);
