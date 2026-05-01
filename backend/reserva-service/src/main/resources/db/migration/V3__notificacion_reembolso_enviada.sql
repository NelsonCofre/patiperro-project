-- Marca envío at-least-once del correo de reembolso al tutor (notification-service).

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS notificacion_reembolso_enviada_en TIMESTAMP;
