package com.patiperro.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /internal/chat/nuevo-mensaje} (servidor a servidor, cabecera interna).
 */
public record ChatNuevoMensajePushRequest(
        @NotNull @Positive Integer idUsuarioDestino,
        @NotNull @Positive Integer idReserva,
        @NotNull @Positive Integer idConversacion,
        @NotNull @Positive Integer idMensaje,
        @NotBlank @Size(max = 160) String remitenteNombre,
        @NotBlank @Size(max = 500) String contenidoPreview,
        @Size(max = 512) String urlDeepLink) {
}
