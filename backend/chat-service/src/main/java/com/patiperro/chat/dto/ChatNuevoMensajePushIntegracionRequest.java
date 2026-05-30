package com.patiperro.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo enviado a notification-service ({@code POST /internal/chat/nuevo-mensaje}).
 * Contrato JSON idéntico a {@code com.patiperro.notification_service.dto.ChatNuevoMensajePushRequest}.
 */
public record ChatNuevoMensajePushIntegracionRequest(
        @NotNull @Positive Integer idUsuarioDestino,
        @NotNull @Positive Integer idReserva,
        @NotNull @Positive Integer idConversacion,
        @NotNull @Positive Integer idMensaje,
        @NotBlank @Size(max = 160) String remitenteNombre,
        @NotBlank @Size(max = 500) String contenidoPreview,
        @Size(max = 512) String urlDeepLink) {

    /** Alineado con el recorte previo en {@code ChatRealtimeController} y bajo el tope del DTO. */
    private static final int PREVIEW_MAX_CHARS = 120;

    private static final String DEEP_LINK_PREFIX = "/chat/reserva/";

    /**
     * Construye el payload de integración a partir de un mensaje realtime ya persistido.
     *
     * @return {@code null} si faltan datos obligatorios o no se puede cumplir {@code @NotBlank}
     */
    public static ChatNuevoMensajePushIntegracionRequest desdeMensajeRealtime(
            Integer idUsuarioDestino,
            ChatMessageOutbound outbound) {
        if (idUsuarioDestino == null
                || idUsuarioDestino <= 0
                || outbound == null
                || outbound.getIdReserva() == null
                || outbound.getIdReserva() <= 0
                || outbound.getIdConversacion() == null
                || outbound.getIdConversacion() <= 0
                || outbound.getIdMensaje() == null
                || outbound.getIdMensaje() <= 0) {
            return null;
        }
        String remitente = outbound.getSender() != null ? outbound.getSender().trim() : "";
        if (remitente.isEmpty()) {
            return null;
        }
        String preview = previewDesdeOutbound(outbound);
        if (preview.isEmpty()) {
            return null;
        }
        return new ChatNuevoMensajePushIntegracionRequest(
                idUsuarioDestino,
                outbound.getIdReserva(),
                outbound.getIdConversacion(),
                outbound.getIdMensaje(),
                remitente,
                preview,
                DEEP_LINK_PREFIX + outbound.getIdReserva());
    }

    private static String previewDesdeOutbound(ChatMessageOutbound outbound) {
        if ("IMAGEN".equalsIgnoreCase(outbound.getTipo())) {
            String comentario = truncarPreview(outbound.getContent());
            return comentario.isEmpty() ? "📷 Foto del paseo" : comentario;
        }
        return truncarPreview(outbound.getContent());
    }

    private static String truncarPreview(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= PREVIEW_MAX_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, PREVIEW_MAX_CHARS - 1) + "…";
    }
}
