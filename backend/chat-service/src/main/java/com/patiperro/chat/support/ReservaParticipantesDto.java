package com.patiperro.chat.support;

/**
 * Subconjunto de {@code com.patiperro.reserva.dto.interno.ReservaComprobanteInternoDto}
 * para resolver el destinatario del push (campos {@code idTutorUsuario}, {@code idPaseadorUsuario}).
 * Campos adicionales del JSON de comprobante se ignoran en la deserialización.
 */
public record ReservaParticipantesDto(
        Integer idTutorUsuario,
        Integer idPaseadorUsuario) {
}
