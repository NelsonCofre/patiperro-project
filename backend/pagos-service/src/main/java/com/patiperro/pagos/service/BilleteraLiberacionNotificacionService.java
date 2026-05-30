package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.billetera.LiberacionBatchOutcome;
import com.patiperro.pagos.dto.billetera.LiberacionBatchOutcome.LiberacionLineaPaseador;
import com.patiperro.pagos.support.NotificationLiberacionConsolidadaClient;
import com.patiperro.pagos.support.PaseadorCorreoInternoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Opcional: correo consolidado al paseador tras el job de liberación. Fallos aquí no afectan saldos (ya persistidos).
 */
@Service
public class BilleteraLiberacionNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(BilleteraLiberacionNotificacionService.class);

    private final boolean notificarHabilitado;
    private final PaseadorCorreoInternoClient paseadorCorreoInternoClient;
    private final NotificationLiberacionConsolidadaClient notificationLiberacionConsolidadaClient;

    public BilleteraLiberacionNotificacionService(
            @Value("${patiperro.pagos.billetera.liberacion.notificar-consolidado.enabled:false}") boolean notificarHabilitado,
            PaseadorCorreoInternoClient paseadorCorreoInternoClient,
            NotificationLiberacionConsolidadaClient notificationLiberacionConsolidadaClient) {
        this.notificarHabilitado = notificarHabilitado;
        this.paseadorCorreoInternoClient = paseadorCorreoInternoClient;
        this.notificationLiberacionConsolidadaClient = notificationLiberacionConsolidadaClient;
    }

    /**
     * Intenta enviar un aviso por paseador con filas liberadas en esta corrida. Errores solo en log.
     */
    public void notificarConsolidadoSiCorresponde(LiberacionBatchOutcome outcome) {
        if (!notificarHabilitado || outcome == null || outcome.totalReservasLiberadas() <= 0) {
            return;
        }
        if (!notificationLiberacionConsolidadaClient.isConfigured()) {
            log.debug("Notificación liberación: integración notification deshabilitada o incompleta; omitido");
            return;
        }
        if (!paseadorCorreoInternoClient.isConfigured()) {
            log.warn(
                    "Notificación liberación: integración paseadores (correo interno) deshabilitada; "
                            + "active patiperro.pagos.integracion.paseadores.enabled y secret/url");
            return;
        }
        for (LiberacionLineaPaseador linea : outcome.porPaseador()) {
            try {
                enviarUnaLinea(linea);
            } catch (RuntimeException e) {
                log.warn(
                        "Notificación liberación: fallo no esperado (usuario={})",
                        linea.idUsuarioPaseador(),
                        e);
            }
        }
    }

    private void enviarUnaLinea(LiberacionLineaPaseador linea) {
        Long uid = linea.idUsuarioPaseador();
        String correo = paseadorCorreoInternoClient.obtenerCorreo(uid);
        if (!StringUtils.hasText(correo)) {
            log.info("Notificación liberación: sin correo para usuario {}; omitido envío", uid);
            return;
        }
        boolean ok = notificationLiberacionConsolidadaClient.enviar(
                uid, correo, linea.montoNetoTotal(), linea.cantidadReservas());
        if (!ok) {
            log.warn("Notificación liberación: notification no confirmó envío (usuario={})", uid);
        }
    }
}
