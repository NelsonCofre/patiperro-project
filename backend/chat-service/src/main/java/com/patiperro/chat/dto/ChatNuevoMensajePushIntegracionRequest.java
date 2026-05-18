package com.patiperro.chat.dto;

/**
 * Cuerpo enviado a notification-service ({@code POST /internal/chat/nuevo-mensaje}).
 * Mismos nombres de campo que {@code ChatNuevoMensajePushRequest} en notification-service.
 */
public record ChatNuevoMensajePushIntegracionRequest(
        Integer idUsuarioDestino,
        Integer idReserva,
        Integer idConversacion,
        Integer idMensaje,
        String remitenteNombre,
        String contenidoPreview,
        String urlDeepLink) {
}
