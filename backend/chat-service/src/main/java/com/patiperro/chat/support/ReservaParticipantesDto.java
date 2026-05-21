package com.patiperro.chat.support;

/**
 * Subconjunto de {@code com.patiperro.reserva.dto.interno.ReservaComprobanteInternoDto}
 * (mismo {@code GET /api/reserva/interno/{id}/comprobante}).
 * Usado para push (participantes) y validación de fotos ({@code nombreEstadoReserva} = EN CURSO).
 */
public record ReservaParticipantesDto(
        Integer idTutorUsuario,
        Integer idPaseadorUsuario,
        Integer idEstadoReserva,
        String nombreEstadoReserva) {
}
