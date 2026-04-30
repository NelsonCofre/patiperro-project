-- Catálogo estado_reserva alineado con com.patiperro.reserva.model.EstadoReservaCatalogo.
-- Idempotente (ON CONFLICT DO NOTHING): seguro en reinicios y BDs ya pobladas.
-- Nota: id 3 = rechazo paseador; id 6 = retiro tutor (CANCELADA); no mezclar significados.

INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (1, 'SOLICITADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (2, 'ACEPTADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (3, 'RECHAZADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (4, 'EN CURSO')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (5, 'FINALIZADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (6, 'CANCELADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (7, 'PENDIENTE_PAGO')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (8, 'PAGADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;
