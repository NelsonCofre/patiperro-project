package com.patiperro.chat.support;

/**
 * Subconjunto de {@code ReservaComprobanteInternoDto} (reserva-service) para resolver destinatario push.
 */
public record ReservaParticipantesDto(
        Integer idTutorUsuario,
        Integer idPaseadorUsuario) {
}
