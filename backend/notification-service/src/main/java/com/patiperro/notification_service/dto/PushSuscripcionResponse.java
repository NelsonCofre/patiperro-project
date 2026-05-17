package com.patiperro.notification_service.dto;

import java.time.Instant;

/**
 * Respuesta tras registrar una suscripción. No incluye {@code p256dh} ni {@code auth}.
 */
public record PushSuscripcionResponse(
        Integer idSuscripcion,
        Integer idUsuario,
        String endpoint,
        Instant fechaAlta) {
}
