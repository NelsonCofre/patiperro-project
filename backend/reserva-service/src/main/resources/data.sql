-- Estados adicionales alineados con EstadoReservaCatalogo (id 4 y 5).
-- Idempotente: no falla si las filas ya existen.
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (4, 'EN_CURSO')
ON CONFLICT (id_estado_reserva) DO NOTHING;
INSERT INTO estado_reserva (id_estado_reserva, nombre_estado)
VALUES (5, 'FINALIZADA')
ON CONFLICT (id_estado_reserva) DO NOTHING;
