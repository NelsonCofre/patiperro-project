package com.patiperro.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /internal/chat/nuevo-mensaje} (servidor a servidor, cabecera interna).
 */
public record ChatNuevoMensajePushRequest(
        @NotNull Integer idUsuarioDestino,
        @NotNull Integer idReserva,
        @NotNull Integer idConversacion,
        @NotNull Integer idMensaje,
        @NotBlank String remitenteNombre,
        @NotBlank String contenidoPreview,
        String urlDeepLink) {
}
