package com.patiperro.notification_service.dto;

import java.time.Instant;

/**
 * Respuesta tras registrar una suscripción push.
 * No incluye {@code p256dh} ni {@code auth} (secretos de la suscripción del dispositivo).
 */
public record PushSuscripcionResponse(
        Integer idSuscripcion,
        Integer idUsuario,
        String endpoint,
        Instant fechaAlta) {
}
