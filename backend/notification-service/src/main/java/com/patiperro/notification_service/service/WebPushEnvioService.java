package com.patiperro.notification_service.service;

import com.patiperro.notification_service.config.WebPushProperties;
import com.patiperro.notification_service.dto.ChatNuevoMensajePushRequest;
import com.patiperro.notification_service.model.PushSuscripcion;
import com.patiperro.notification_service.repository.PushSuscripcionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;

/**
 * Envío Web Push tras mensajes de chat (best-effort).
 * Invocado desde {@code POST /internal/chat/nuevo-mensaje}; no propaga excepciones al llamador.
 */
@Service
public class WebPushEnvioService {

    private static final Logger log = LoggerFactory.getLogger(WebPushEnvioService.class);

    private final PushSuscripcionRepository pushSuscripcionRepository;
    private final WebPushProperties webPushProperties;

    public WebPushEnvioService(
            PushSuscripcionRepository pushSuscripcionRepository,
            WebPushProperties webPushProperties) {
        this.pushSuscripcionRepository = pushSuscripcionRepository;
        this.webPushProperties = webPushProperties;
    }

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
            log.error("Web Push: claves VAPID inválidas; no se envía (reserva={})", e.getMessage());
            return null;
        }
    }

    private String buildPayload(ChatNuevoMensajePushRequest req) {
        int max = Math.max(20, webPushProperties.getPayloadPreviewChars());
        String body = req.contenidoPreview() != null ? req.contenidoPreview() : "";
        if (body.length() > max) {
            body = body.substring(0, max - 1) + "…";
        }

        String remitente = req.remitenteNombre() != null ? req.remitenteNombre() : "";

        StringBuilder data = new StringBuilder();
        data.append("{\"idReserva\":").append(req.idReserva());
        data.append(",\"idConversacion\":").append(req.idConversacion());
        data.append(",\"idMensaje\":").append(req.idMensaje());
        if (StringUtils.hasText(req.urlDeepLink())) {
            data.append(",\"url\":\"").append(escapeJson(req.urlDeepLink().trim())).append('"');
        }
        data.append('}');

        return "{"
                + "\"title\":\"" + escapeJson(remitente) + "\","
                + "\"body\":\"" + escapeJson(body) + "\","
                + "\"tag\":\"chat-" + req.idReserva() + "\","
                + "\"data\":" + data
                + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static boolean esSuscripcionExpirada(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("410") || msg.contains("404") || msg.contains("Gone");
    }
}
