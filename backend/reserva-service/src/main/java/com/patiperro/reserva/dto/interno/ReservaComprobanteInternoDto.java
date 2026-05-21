package com.patiperro.reserva.dto.interno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos internos mínimos para generar un resumen de transacción post-pago y validaciones
 * servidor-a-servidor (p. ej. fotos del paseo en chat-service).
 * Se expone solo vía rutas {@code /api/reserva/interno/**} (cabecera secreta interna).
 * <p>
 * Consumidores: pagos-service (comprobante), chat-service ({@code idEstadoReserva},
 * {@code nombreEstadoReserva} para validar paseo EN CURSO). Campos nuevos al final del JSON
 * son ignorados por clientes que no los declaran.
 */
public record ReservaComprobanteInternoDto(
        Integer idReserva,
        Integer idTutorUsuario,
        String tutorCorreo,
        Integer idMascota,
        String mascotaNombre,
        Integer idPaseadorUsuario,
        String paseadorNombre,
        Integer idAgendaBloque,
        LocalDate fechaPaseo,
        LocalDateTime horaInicio,
        LocalDateTime horaFinal,
        BigDecimal montoTotal,
        Long idTransaccionPagos,
        Integer idEstadoReserva,
        String nombreEstadoReserva) {
}

