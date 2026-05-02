package com.patiperro.reserva.scheduler;

import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.service.ReservaReembolsoService;
import com.patiperro.reserva.support.NotificacionReembolsoIntegracionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Paso 6 (solo backend): reenvío de correo al tutor cuando el reembolso MP ya quedó marcado en reserva pero
 * la llamada HTTP a notification-service falló (at-least-once).
 */
@Component
@ConditionalOnProperty(name = "patiperro.reserva.notificacion-reembolso.scheduler.enabled", havingValue = "true")
public class ReservaNotificacionReembolsoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaNotificacionReembolsoScheduler.class);

    private final ReservaRepository reservaRepository;
    private final ReservaReembolsoService reservaReembolsoService;
    private final NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient;
    private final int maxBatchSize;
    private final long pauseBetweenItemsMs;

    public ReservaNotificacionReembolsoScheduler(
            ReservaRepository reservaRepository,
            ReservaReembolsoService reservaReembolsoService,
            NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient,
            @Value("${patiperro.reserva.notificacion-reembolso.scheduler.max-batch-size:30}") int maxBatchSize,
            @Value("${patiperro.reserva.notificacion-reembolso.scheduler.pause-between-items-ms:0}") long pauseBetweenItemsMs) {
        this.reservaRepository = reservaRepository;
        this.reservaReembolsoService = reservaReembolsoService;
        this.notificacionReembolsoIntegracionClient = notificacionReembolsoIntegracionClient;
        this.maxBatchSize = Math.max(1, maxBatchSize);
        this.pauseBetweenItemsMs = Math.max(0, pauseBetweenItemsMs);
    }

    @Scheduled(
            initialDelayString = "${patiperro.reserva.notificacion-reembolso.scheduler.initial-delay-ms:90000}",
            fixedDelayString = "${patiperro.reserva.notificacion-reembolso.scheduler.fixed-delay-ms:300000}")
    public void reintentarNotificacionesPendientes() {
        if (!notificacionReembolsoIntegracionClient.isEnabled()) {
            return;
        }
        long t0 = System.nanoTime();
        List<Integer> ids = reservaRepository.findIdReservasPendientesNotificacionReembolso(
                EstadoReservaCatalogo.IDS_ESTADO_REEMBOLSO_MERCADOPAGO,
                PageRequest.of(0, maxBatchSize));
        if (ids.isEmpty()) {
            return;
        }
        int fallos = 0;
        for (int i = 0; i < ids.size(); i++) {
            Integer id = ids.get(i);
            try {
                reservaReembolsoService.reintentarNotificacionReembolsoTutorSync(id);
            } catch (RuntimeException e) {
                fallos++;
                log.warn("Job notificación reembolso: fallo idReserva={}", id, e);
            }
            dormirEntreItemsSiAplica(i, ids.size());
        }
        long duracionMs = (System.nanoTime() - t0) / 1_000_000L;
        log.info(
                "Job notificación reembolso: candidatos={} fallos={} maxLote={} duracionMs={}",
                ids.size(),
                fallos,
                maxBatchSize,
                duracionMs);
    }

    private void dormirEntreItemsSiAplica(int indice, int total) {
        if (pauseBetweenItemsMs <= 0 || indice >= total - 1) {
            return;
        }
        try {
            Thread.sleep(pauseBetweenItemsMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Job notificación reembolso: interrupción en pausa entre ítems");
        }
    }
}
