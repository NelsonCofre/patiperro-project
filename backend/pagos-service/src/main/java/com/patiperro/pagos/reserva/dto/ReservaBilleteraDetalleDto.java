package com.patiperro.pagos.reserva.dto;

/** Respuesta de {@code POST /api/reserva/interno/billetera/detalles-paseador}. */
public record ReservaBilleteraDetalleDto(
        Integer idReserva,
        String mascotaNombre,
        String tutorNombre,
        String fechaAgenda,
        String horaInicio,
        String nombreEstadoReserva
) {
}
