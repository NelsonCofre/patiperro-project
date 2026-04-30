package com.patiperro.notification_service.service;

import com.patiperro.notification_service.dto.NotificacionEventoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Notificación al paseador cuando el tutor completó el pago de una reserva (correo vía Brevo / {@link NotificationService}).
 */
@Service
public class PagoNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(PagoNotificacionService.class);

    public static final String TIPO_EVENTO_PAGO_CONFIRMADO = "PAGO_CONFIRMADO";

    private final NotificationService notificationService;

    public PagoNotificacionService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * @param emailDestino correo del paseador; si viene vacío solo se registra y no se llama a Brevo
     */
    public void procesarPagoConfirmado(Integer idReserva, Integer idPaseador, String emailDestino) {
        if (idReserva == null) {
            log.warn("Pago confirmado: idReserva nulo; se ignora");
            return;
        }
        if (!StringUtils.hasText(emailDestino)) {
            log.info("Pago confirmado: sin email destino; omitido envío Brevo (idReserva={}, idPaseador={})",
                    idReserva, idPaseador);
            return;
        }

        NotificacionEventoRequest req = new NotificacionEventoRequest();
        req.setEmailDestino(emailDestino.trim());
        req.setTipoEvento(TIPO_EVENTO_PAGO_CONFIRMADO);
        Map<String, Object> vars = new HashMap<>();
        vars.put("idReserva", idReserva);
        vars.put("idPaseador", idPaseador != null ? idPaseador : "");
        vars.put("mensaje",
                "El tutor completó el pago en Patiperro. La reserva cuenta con respaldo financiero hasta que el servicio finalice.");
        req.setVariables(vars);

        try {
            notificationService.procesarEventoUniversal(req);
        } catch (RuntimeException e) {
            log.warn("Pago confirmado: fallo al disparar notificación Brevo (idReserva={}, destinatario parcial={})",
                    idReserva, enmascararEmail(emailDestino), e);
        }
    }

    private static String enmascararEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
