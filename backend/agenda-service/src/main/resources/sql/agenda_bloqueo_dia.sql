-- Referencia PostgreSQL para agenda_bloqueo_dia (bloqueo personal de día completo).
-- Con spring.jpa.hibernate.ddl-auto=update Hibernate suele crear/actualizar la tabla sola.
-- Usa este script si prefieres DDL explícito o entornos sin auto-DDL.

CREATE TABLE IF NOT EXISTS agenda_bloqueo_dia (
    id_bloqueo   SERIAL PRIMARY KEY,
    id_usuario   INTEGER NOT NULL,
    fecha        DATE NOT NULL,
    motivo       VARCHAR(120),
    creado_en    TIMESTAMP NOT NULL,
    CONSTRAINT uk_agenda_bloqueo_dia_usuario_fecha UNIQUE (id_usuario, fecha)
);

CREATE INDEX IF NOT EXISTS idx_agenda_bloqueo_dia_usuario_fecha
    ON agenda_bloqueo_dia (id_usuario, fecha);
