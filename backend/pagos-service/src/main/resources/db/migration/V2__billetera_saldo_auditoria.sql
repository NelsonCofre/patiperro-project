-- Auditoría opción A: VERIFICACION → DISPONIBLE, referencia al cobro original (transaccion.id_transaccion).
CREATE TABLE IF NOT EXISTS billetera_saldo_auditoria (
    id_auditoria             BIGSERIAL PRIMARY KEY,
    id_tracking              BIGINT NOT NULL,
    id_reserva               INTEGER NOT NULL,
    id_usuario_paseador      BIGINT NOT NULL,
    id_transaccion           BIGINT NOT NULL,
    monto_neto               NUMERIC(14, 2) NOT NULL,
    saldo_verificacion_antes NUMERIC(14, 2) NOT NULL,
    saldo_actual_antes       NUMERIC(14, 2) NOT NULL,
    saldo_verificacion_despues NUMERIC(14, 2) NOT NULL,
    saldo_actual_despues       NUMERIC(14, 2) NOT NULL,
    creado_en                TIMESTAMP NOT NULL,
    CONSTRAINT uk_billetera_saldo_aud_id_tracking UNIQUE (id_tracking),
    CONSTRAINT fk_billetera_saldo_aud_tracking FOREIGN KEY (id_tracking) REFERENCES billetera_reserva_tracking (id_tracking),
    CONSTRAINT fk_billetera_saldo_aud_tx FOREIGN KEY (id_transaccion) REFERENCES transaccion (id_transaccion)
);

CREATE INDEX IF NOT EXISTS idx_billetera_saldo_aud_usuario_creado
    ON billetera_saldo_auditoria (id_usuario_paseador, creado_en DESC);

CREATE INDEX IF NOT EXISTS idx_billetera_saldo_aud_tx
    ON billetera_saldo_auditoria (id_transaccion);
