package com.patiperro.notification_service.controller;

import com.patiperro.notification_service.dto.ChatNuevoMensajePushRequest;
import com.patiperro.notification_service.service.WebPushEnvioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Web Push del chat invocado por otros microservicios (p. ej. chat-service).
 * Rutas mutables protegidas con cabecera {@link #HEADER_INTERNO}, no con JWT de usuario.
 * El secreto debe coincidir con {@code patiperro.notification.interno.secret} y con el del emisor.
 */
@RestController
@RequestMapping("/internal/chat")
@RequiredArgsConstructor
public class InternalChatPushController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final WebPushEnvioService webPushEnvioService;

    @Value("${patiperro.notification.interno.secret:}")
    private String internoSecret;

    /**
     * Dispara notificación push al destinatario (best-effort). Responde 202 aunque falle el envío a algún dispositivo.
     */
    @PostMapping("/nuevo-mensaje")
    public ResponseEntity<Void> nuevoMensaje(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @Valid @RequestBody ChatNuevoMensajePushRequest body) {
        return rechazarSiSecretoInvalido(secretoHeader)
                .orElseGet(() -> {
                    webPushEnvioService.enviarNuevoMensajeChat(body);
                    return ResponseEntity.accepted().build();
                });
    }

    private Optional<ResponseEntity<Void>> rechazarSiSecretoInvalido(String secretoHeader) {
        if (!StringUtils.hasText(internoSecret)) {
            return Optional.of(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }
        String header = secretoHeader != null ? secretoHeader.trim() : "";
        if (!internoSecret.equals(header)) {
            return Optional.of(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        return Optional.empty();
    }
}
