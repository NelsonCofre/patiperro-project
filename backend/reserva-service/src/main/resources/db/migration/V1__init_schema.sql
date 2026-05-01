-- Esquema inicial alineado con JPA (EstadoReserva, Reserva).

CREATE TABLE estado_reserva (
    id_estado_reserva SERIAL PRIMARY KEY,
    nombre_estado VARCHAR(60) NOT NULL
);

CREATE TABLE reserva (
    id_reserva SERIAL PRIMARY KEY,
    id_tutor_usuario INTEGER NOT NULL,
    id_mascota INTEGER NOT NULL,
    id_agenda_bloque INTEGER NOT NULL,
    id_tarifa INTEGER NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    fecha_aceptacion TIMESTAMP,
    monto_total NUMERIC(10, 2) NOT NULL,
    id_pago INTEGER,
    mercadopago_payment_id VARCHAR(64),
    mercadopago_reembolso_procesado_en TIMESTAMP,
    estado_reserva_id_estado_reserva INTEGER NOT NULL REFERENCES estado_reserva (id_estado_reserva),
    fecha_inicio_real TIMESTAMP,
    fecha_fin TIMESTAMP,
    codigo_encuentro INTEGER,
    codigo_encuentro_expira_en TIMESTAMP,
    codigo_intentos_fallidos INTEGER,
    codigo_bloqueado_hasta TIMESTAMP,
    motivo_rechazo VARCHAR(120),
    detalle_rechazo VARCHAR(500)
);

CREATE INDEX idx_reserva_estado_fecha_solicitud ON reserva (estado_reserva_id_estado_reserva, fecha_solicitud);

CREATE INDEX idx_reserva_aceptada_codigo_expira ON reserva (estado_reserva_id_estado_reserva, codigo_encuentro_expira_en)
    WHERE codigo_encuentro IS NOT NULL;

CREATE INDEX idx_reserva_reconciliacion_reembolso ON reserva (estado_reserva_id_estado_reserva)
    WHERE mercadopago_payment_id IS NOT NULL
      AND mercadopago_reembolso_procesado_en IS NULL
      AND trim(mercadopago_payment_id) <> '';
