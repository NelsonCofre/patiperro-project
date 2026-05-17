package com.patiperro.notification_service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de {@code POST /api/notificaciones/push/suscripciones}.
 * El {@code idUsuario} se resuelve desde el JWT (tutorId o paseadorId), no se envía aquí.
 */
public record PushSuscripcionRequest(
        @NotBlank String endpoint,
        @NotBlank String p256dh,
        @NotBlank String auth,
        String userAgent) {
}
