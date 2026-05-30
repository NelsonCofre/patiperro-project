package com.patiperro.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/notificaciones/push/suscripciones}.
 * El {@code idUsuario} se resuelve desde el JWT (tutorId o paseadorId), no se envía aquí.
 */
public record PushSuscripcionRequest(
        @NotBlank @Size(max = 2048) String endpoint,
        @NotBlank @Size(max = 512) String p256dh,
        @NotBlank @Size(max = 256) String auth,
        @Size(max = 512) String userAgent) {
}
