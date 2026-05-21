package com.patiperro.pagos.reserva.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Espejo de {@code ReservaComprobanteInternoDto} en reserva-service (mismo JSON de
 * {@code GET /api/reserva/interno/{id}/comprobante}). Los campos de estado son opcionales
 * para pagos; se documentan para paridad del contrato interno.
 */
public record ReservaComprobanteDto(
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

