-- =============================================================================
-- patiperro_notifications — suscripciones Web Push del chat (push_suscripcion).
--
-- Uso:
--   • Dev: spring.jpa.hibernate.ddl-auto=update crea/actualiza la tabla (no es obligatorio ejecutar esto).
--   • Prod: ejecutar este script en patiperro_notifications ANTES del primer arranque con perfil prod
--     (ddl-auto=validate). Idempotente: CREATE IF NOT EXISTS.
--
-- Alineado con entidad PushSuscripcion (InstaWnt → TIMESTAMPTZ, como chat_db-init.sql).
-- =============================================================================

CREATE TABLE IF NOT EXISTS push_suscripcion (
    id_suscripcion    SERIAL PRIMARY KEY,
    id_usuario        INTEGER NOT NULL,
    endpoint          TEXT NOT NULL,
    p256dh_key        TEXT NOT NULL,
    auth_key          TEXT NOT NULL,
    user_agent        VARCHAR(512),
    activa            BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_ultimo_uso  TIMESTAMPTZ,
    CONSTRAINT uq_push_suscripcion_endpoint UNIQUE (endpoint)
);

CREATE INDEX IF NOT EXISTS idx_push_suscripcion_usuario_activa
    ON push_suscripcion (id_usuario, activa);
