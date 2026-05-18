-- Disputa por reserva: bloquea liberación N+2 → disponible mientras disputa_activa = TRUE.
-- Una sola fila por id_reserva (PK); portable y alineado con JPA (sin índice único parcial).
CREATE TABLE IF NOT EXISTS billetera_disputa_reserva (
    id_reserva       INTEGER PRIMARY KEY,
    disputa_activa   BOOLEAN NOT NULL DEFAULT FALSE,
    motivo           VARCHAR(512),
    abierto_en       TIMESTAMP,
    cerrado_en       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_billetera_disputa_reserva_activa
    ON billetera_disputa_reserva (disputa_activa)
    WHERE disputa_activa = TRUE;
