package com.patiperro.reserva.dto.interno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos internos mínimos para generar un resumen de transacción post-pago.
 * Se expone solo vía rutas /api/reserva/interno/** (secreto interno).
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
        Long idTransaccionPagos) {
}

