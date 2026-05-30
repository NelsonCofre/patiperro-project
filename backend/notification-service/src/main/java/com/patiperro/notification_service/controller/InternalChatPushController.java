package com.patiperro.notification_service.controller;

import com.patiperro.notification_service.dto.ChatNuevoMensajePushRequest;
import com.patiperro.notification_service.service.WebPushEnvioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web Push del chat invocado por otros microservicios (p. ej. chat-service).
 * Se protege con cabecera {@link #HEADER_INTERNO}, no con JWT de usuario.
 * El valor del secreto debe coincidir con {@code patiperro.notification.interno.secret}
 * y con el configurado en el cliente de integración del emisor.
 */
@RestController
@RequestMapping("/internal/chat")
public class InternalChatPushController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final WebPushEnvioService webPushEnvioService;

    @Value("${patiperro.notification.interno.secret:}")
    private String internoSecret;

    public InternalChatPushController(WebPushEnvioService webPushEnvioService) {
        this.webPushEnvioService = webPushEnvioService;
    }

    /**
     * Dispara notificación push al destinatario (best-effort). Responde 202 aunque falle el envío a algún dispositivo.
     */
    @PostMapping("/nuevo-mensaje")
    public ResponseEntity<Void> nuevoMensaje(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @Valid @RequestBody ChatNuevoMensajePushRequest body) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        webPushEnvioService.enviarNuevoMensajeChat(body);
        return ResponseEntity.accepted().build();
    }
}
