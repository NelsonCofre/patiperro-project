-- Índices para historial de liberaciones por paseador y job N+2 (Postgres).
-- Idempotente: seguro si Hibernate u otro proceso ya creó índices equivalentes.

CREATE INDEX IF NOT EXISTS idx_billetera_tracking_paseador_liberado
    ON billetera_reserva_tracking (id_usuario_paseador, liberado_en DESC);

CREATE INDEX IF NOT EXISTS idx_billetera_tracking_fase_liberado
    ON billetera_reserva_tracking (fase, liberado_en);
