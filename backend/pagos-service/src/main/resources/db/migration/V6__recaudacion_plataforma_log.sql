CREATE TABLE IF NOT EXISTS recaudacion_plataforma_log (
    id_log BIGSERIAL PRIMARY KEY,
    id_transaccion BIGINT NOT NULL,
    id_reserva INTEGER NOT NULL,
    tipo_evento VARCHAR(40) NOT NULL,
    monto_bruto NUMERIC(14, 2) NOT NULL,
    comision_app NUMERIC(14, 2) NOT NULL,
    monto_neto NUMERIC(14, 2) NOT NULL,
    fecha_evento TIMESTAMP NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_recaudacion_tx FOREIGN KEY (id_transaccion)
        REFERENCES transaccion (id_transaccion),
    CONSTRAINT uk_recaudacion_tx_evento UNIQUE (id_transaccion, tipo_evento)
);

CREATE INDEX IF NOT EXISTS idx_recaudacion_fecha_evento
    ON recaudacion_plataforma_log (fecha_evento);

CREATE INDEX IF NOT EXISTS idx_recaudacion_tipo_fecha
    ON recaudacion_plataforma_log (tipo_evento, fecha_evento);
