package com.patiperro.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.notification_service.config.WebPushProperties;
import com.patiperro.notification_service.dto.ChatNuevoMensajePushRequest;
import com.patiperro.notification_service.model.PushSuscripcion;
import com.patiperro.notification_service.repository.PushSuscripcionRepository;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío Web Push tras mensajes de chat (best-effort).
 * Invocado desde {@code POST /internal/chat/nuevo-mensaje}; no propaga excepciones al llamador.
 */
@Service
@RequiredArgsConstructor
public class WebPushEnvioService {

    private static final Logger log = LoggerFactory.getLogger(WebPushEnvioService.class);

    private static final String DEEP_LINK_PREFIX = "/chat/";

    private final PushSuscripcionRepository pushSuscripcionRepository;
    private final WebPushProperties webPushProperties;
    private final ObjectMapper objectMapper;

    /**
     * Notifica al destinatario en todos sus dispositivos suscritos. Errores se registran en log;
     * el endpoint interno responde 202 aunque falle algún envío.
     */
    public void enviarNuevoMensajeChat(ChatNuevoMensajePushRequest req) {
        if (req == null || req.idUsuarioDestino() == null) {
            log.debug("Web Push: solicitud incompleta; omitido");
            return;
        }

        try {
            if (!webPushProperties.isReadyForSend()) {
                log.debug("Web Push deshabilitado o sin VAPID; omitido (destino={}, reserva={})",
                        req.idUsuarioDestino(), req.idReserva());
                return;
            }

            List<PushSuscripcion> suscripciones =
                    pushSuscripcionRepository.findByIdUsuarioAndActivaTrue(req.idUsuarioDestino());
            if (suscripciones.isEmpty()) {
                log.debug("Sin suscripciones push activas para usuario {}", req.idUsuarioDestino());
                return;
            }

            PushService pushService = buildPushService();
            if (pushService == null) {
                return;
            }

            String payload = buildPayload(req);
            if (payload == null) {
                log.warn("Web Push: no se pudo serializar payload (destino={}, reserva={})",
                        req.idUsuarioDestino(), req.idReserva());
                return;
            }

            for (PushSuscripcion s : suscripciones) {
                enviarUna(pushService, s, payload, req.idReserva());
            }
        } catch (Exception e) {
            log.warn("Web Push: error inesperado (destino={}, reserva={}): {}",
                    req.idUsuarioDestino(), req.idReserva(), e.getMessage());
        }
    }

    private void enviarUna(PushService pushService, PushSuscripcion s, String payload, Integer idReserva) {
        try {
            Subscription subscription = new Subscription(
                    s.getEndpoint(),
                    new Subscription.Keys(s.getP256dhKey(), s.getAuthKey()));
            Notification notification = new Notification(subscription, payload);
            pushService.send(notification);
            s.setFechaUltimoUso(Instant.now());
            pushSuscripcionRepository.save(s);
        } catch (Exception e) {
            if (esSuscripcionExpirada(e)) {
                log.info("Suscripción push expirada; eliminando endpoint (usuario={}, reserva={})",
                        s.getIdUsuario(), idReserva);
                pushSuscripcionRepository.delete(s);
            } else {
                log.warn("Fallo envío Web Push (usuario={}, reserva={}): {}",
                        s.getIdUsuario(), idReserva, e.getMessage());
            }
        }
    }

    private PushService buildPushService() {
        WebPushProperties.Vapid vapid = webPushProperties.getVapid();
        try {
            return new PushService(vapid.getPublicKey(), vapid.getPrivateKey(), vapid.getSubject());
        } catch (GeneralSecurityException e) {
            log.error("Web Push: claves VAPID inválidas; no se envía: {}", e.getMessage());
            return null;
        }
    }

    private String buildPayload(ChatNuevoMensajePushRequest req) {
        int max = webPushProperties.getPayloadPreviewChars();
        String body = req.contenidoPreview() != null ? req.contenidoPreview() : "";
        if (body.length() > max) {
            body = body.substring(0, max - 1) + "…";
        }

        String remitente = req.remitenteNombre() != null ? req.remitenteNombre() : "";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("idReserva", req.idReserva());
        data.put("idConversacion", req.idConversacion());
        data.put("idMensaje", req.idMensaje());
        String url = normalizarUrlDeepLink(req.urlDeepLink());
        if (url != null) {
            data.put("url", url);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("title", remitente);
        root.put("body", body);
        root.put("tag", "chat-" + req.idReserva());
        root.put("data", data);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.warn("Web Push: error serializando payload (reserva={}): {}", req.idReserva(), e.getMessage());
            return null;
        }
    }

    /**
     * Solo rutas relativas del chat; evita inyectar {@code javascript:} u otras URLs en el payload.
     */
    private static String normalizarUrlDeepLink(String urlDeepLink) {
        if (!StringUtils.hasText(urlDeepLink)) {
            return null;
        }
        String url = urlDeepLink.trim();
        if (!url.startsWith(DEEP_LINK_PREFIX) || url.contains("..")) {
            return null;
        }
        return url;
    }

    private static boolean esSuscripcionExpirada(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null
                    && (msg.contains("410") || msg.contains("404") || msg.contains("Gone"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
