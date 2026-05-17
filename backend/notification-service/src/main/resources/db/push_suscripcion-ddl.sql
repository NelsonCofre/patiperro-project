-- Referencia para patiperro_notifications (perfil prod con spring.jpa.hibernate.ddl-auto=validate).
-- En dev Hibernate crea/actualiza la tabla con ddl-auto=update; ejecutar este script solo si arrancáis con perfil prod.

CREATE TABLE IF NOT EXISTS push_suscripcion (
    id_suscripcion SERIAL PRIMARY KEY,
    id_usuario     INTEGER NOT NULL,
    endpoint       TEXT NOT NULL,
    p256dh_key     TEXT NOT NULL,
    auth_key       TEXT NOT NULL,
    user_agent     VARCHAR(512),
    activa         BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta     TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_ultimo_uso TIMESTAMP,
    CONSTRAINT uq_push_suscripcion_endpoint UNIQUE (endpoint)
);

CREATE INDEX IF NOT EXISTS idx_push_suscripcion_usuario_activa
    ON push_suscripcion (id_usuario, activa);
