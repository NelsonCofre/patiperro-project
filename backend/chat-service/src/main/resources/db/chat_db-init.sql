-- =============================================================================
-- chat_db — crear base y tablas (alineado con entidades JPA de chat-service).
--
-- En pgAdmin:
--   1) Ejecutar solo el bloque "CREAR BASE" conectado a la base "postgres".
--   2) Ejecutar el resto conectado a "chat_db".
-- =============================================================================

-- ---------- CREAR BASE (ejecutar en postgres) ----------
CREATE DATABASE chat_db
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'Spanish_Chile.1252'
         LC_CTYPE = 'Spanish_Chile.1252'
         TEMPLATE = template0;
-- Si LC_COLLATE/LC_CTYPE fallan en tu instalación, usa:
-- CREATE DATABASE chat_db;

-- ---------- TABLAS (ejecutar en chat_db) ----------

CREATE TABLE IF NOT EXISTS estado_chat (
    id_estado   SERIAL PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL
);

CREATE TABLE IF NOT EXISTS estado_mensaje (
    id_estado_mensaje SERIAL PRIMARY KEY,
    nombre            VARCHAR(60) NOT NULL
);

CREATE TABLE IF NOT EXISTS conversacion (
    id_conversacion          SERIAL PRIMARY KEY,
    id_reserva               INTEGER NOT NULL,
    fecha_creacion           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado_chat_id_estado    INTEGER NOT NULL,
    CONSTRAINT fk_conversacion_estado_chat
        FOREIGN KEY (estado_chat_id_estado) REFERENCES estado_chat (id_estado)
);

CREATE TABLE IF NOT EXISTS mensaje (
    id_mensaje                        SERIAL PRIMARY KEY,
    id_usuario                        INTEGER NOT NULL,
    tipo                              VARCHAR(20) NOT NULL DEFAULT 'TEXTO',
    contenido                         VARCHAR(4000),
    url_media                         VARCHAR(512),
    fecha_envio                       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    conversacion_id_conversacion      INTEGER NOT NULL,
    estado_mensaje_id_estado_mensaje  INTEGER NOT NULL,
    CONSTRAINT fk_mensaje_conversacion
        FOREIGN KEY (conversacion_id_conversacion) REFERENCES conversacion (id_conversacion) ON DELETE CASCADE,
    CONSTRAINT fk_mensaje_estado_mensaje
        FOREIGN KEY (estado_mensaje_id_estado_mensaje) REFERENCES estado_mensaje (id_estado_mensaje)
);

CREATE INDEX IF NOT EXISTS idx_conversacion_id_reserva ON conversacion (id_reserva);

-- Catálogos mínimos (mismo contenido que ChatEstadosSeedRunner; opcional si arrancas el servicio)
INSERT INTO estado_chat (nombre)
SELECT 'ABIERTA' WHERE NOT EXISTS (SELECT 1 FROM estado_chat WHERE nombre = 'ABIERTA');
INSERT INTO estado_chat (nombre)
SELECT 'CERRADA' WHERE NOT EXISTS (SELECT 1 FROM estado_chat WHERE nombre = 'CERRADA');
INSERT INTO estado_mensaje (nombre)
SELECT 'ENVIADO' WHERE NOT EXISTS (SELECT 1 FROM estado_mensaje WHERE nombre = 'ENVIADO');
INSERT INTO estado_mensaje (nombre)
SELECT 'LEIDO' WHERE NOT EXISTS (SELECT 1 FROM estado_mensaje WHERE nombre = 'LEIDO');
