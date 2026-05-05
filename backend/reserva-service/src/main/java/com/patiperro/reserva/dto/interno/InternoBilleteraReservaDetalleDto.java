package com.patiperro.reserva.dto.interno;

/**
 * Vista mínima de una reserva para enriquecer ítems de billetera en pagos-service.
 */
public record InternoBilleteraReservaDetalleDto(
        Integer idReserva,
        String mascotaNombre,
        String tutorNombre,
        String fechaAgenda,
        String horaInicio,
        String nombreEstadoReserva
) {
}
