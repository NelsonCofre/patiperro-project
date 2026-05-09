package com.patiperro.notification_service.service;

import com.patiperro.notification_service.dto.NotificacionEventoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    public static final String TIPO_EVENTO_REEMBOLSO_RESERVA = "REEMBOLSO_RESERVA";

    /** Resumen de transacción / comprobante informativo al tutor tras pago aprobado. */
    public static final String TIPO_EVENTO_RESUMEN_COMPROBANTE_TUTOR = "RESUMEN_COMPROBANTE_TUTOR";

    private final NotificationService notificationService;

    private final boolean reembolsoProcesadoEnabled;

    public PagoNotificacionService(
            NotificationService notificationService,
            @Value("${patiperro.notification.reembolso-procesado.enabled:true}") boolean reembolsoProcesadoEnabled) {
        this.notificationService = notificationService;
        this.reembolsoProcesadoEnabled = reembolsoProcesadoEnabled;
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

    /**
     * Correo al tutor con el HTML del resumen de transacción (plantilla Brevo: variable {@code cuerpoHtml} en {@code params}, típicamente HTML sin escapar).
     *
     * @return {@code false} si hay datos inválidos esenciales o falla el envío a Brevo; {@code true} si no había nada que enviar (sin correo) o el disparo terminó bien
     */
    public boolean procesarResumenComprobanteTutor(Integer idReserva, String emailDestino, String cuerpoHtml) {
        if (idReserva == null) {
            log.warn("Resumen comprobante tutor: idReserva nulo; se ignora");
            return false;
        }
        if (!StringUtils.hasText(emailDestino)) {
            log.info("Resumen comprobante tutor: sin email destino; omitido (idReserva={})", idReserva);
            return true;
        }

        NotificacionEventoRequest req = new NotificacionEventoRequest();
        req.setEmailDestino(emailDestino.trim());
        req.setTipoEvento(TIPO_EVENTO_RESUMEN_COMPROBANTE_TUTOR);
        Map<String, Object> vars = new HashMap<>();
        vars.put("idReserva", idReserva);
        vars.put("cuerpoHtml", cuerpoHtml != null ? cuerpoHtml : "");
        req.setVariables(vars);

        try {
            notificationService.procesarEventoUniversal(req);
            return true;
        } catch (RuntimeException e) {
            log.warn("Resumen comprobante tutor: fallo Brevo (idReserva={}, destinatario parcial={})",
                    idReserva, enmascararEmail(emailDestino), e);
            return false;
        }
    }

    /**
     * @param emailDestino correo del tutor; vacío → solo log, sin Brevo
     */
    public void procesarReembolsoTutor(Integer idReserva, String emailDestino) {
        dispararEventoReembolsoReserva(idReserva, emailDestino, null, "reembolso-tutor");
    }

    /**
     * Paso servidor-a-servidor explícito “reembolso procesado”; mismo {@link #TIPO_EVENTO_REEMBOLSO_RESERVA} y plantilla Brevo.
     * Si {@code patiperro.notification.reembolso-procesado.enabled=false}, no hace envío (204 desde controller igualmente).
     *
     * @param idTutor opcional para variables de plantilla (este servicio no resuelve correo desde id tutores-service).
     */
    public void procesarReembolsoProcesado(Integer idReserva, String emailDestino, Integer idTutor) {
        if (!reembolsoProcesadoEnabled) {
            log.info("Reembolso procesado: integración deshabilitada por propiedad; omitido (idReserva={})", idReserva);
            return;
        }
        dispararEventoReembolsoReserva(idReserva, emailDestino, idTutor, "reembolso-procesado");
    }

    private void dispararEventoReembolsoReserva(Integer idReserva, String emailDestino, Integer idTutor, String origenLog) {
        if (idReserva == null) {
            log.warn("{}: idReserva nulo; se ignora", origenLog);
            return;
        }
        if (!StringUtils.hasText(emailDestino)) {
            log.info("{}: sin email destino; omitido envío Brevo (idReserva={}, idTutor={})", origenLog, idReserva, idTutor);
            return;
        }

        NotificacionEventoRequest req = new NotificacionEventoRequest();
        req.setEmailDestino(emailDestino.trim());
        req.setTipoEvento(TIPO_EVENTO_REEMBOLSO_RESERVA);
        Map<String, Object> vars = new HashMap<>();
        vars.put("idReserva", idReserva);
        vars.put("idTutor", idTutor != null ? idTutor : "");
        vars.put("mensaje",
                "Te confirmamos que procesamos la devolución del pago de tu reserva en Mercado Pago. "
                        + "El reintegro puede tardar según tu banco o medio de pago.");
        req.setVariables(vars);

        try {
            notificationService.procesarEventoUniversal(req);
        } catch (RuntimeException e) {
            log.warn("{}: fallo al disparar notificación Brevo (idReserva={}, destinatario parcial={})",
                    origenLog, idReserva, enmascararEmail(emailDestino), e);
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
