-- Catálogo estado_reserva (com.patiperro.reserva.model.EstadoReservaCatalogo).
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (1, 'SOLICITADA'),
       (2, 'ACEPTADA'),
       (3, 'RECHAZADA'),
       (4, 'EN CURSO'),
       (5, 'FINALIZADA'),
       (6, 'CANCELADA'),
       (7, 'PENDIENTE_PAGO'),
       (8, 'PAGADA'),
       (9, 'EXPIRADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('estado_reserva', 'id_estado_reserva'),
    COALESCE((SELECT MAX(id_estado_reserva) FROM estado_reserva), 1));
