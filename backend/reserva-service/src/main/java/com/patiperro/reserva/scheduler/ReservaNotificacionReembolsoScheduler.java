package com.patiperro.reserva.scheduler;

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
 * Reenvía correo de reembolso al tutor cuando MP ya quedó marcado pero la llamada HTTP a notification-service falló.
 */
@Component
@ConditionalOnProperty(name = "patiperro.reserva.notificacion-reembolso.scheduler.enabled", havingValue = "true")
public class ReservaNotificacionReembolsoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaNotificacionReembolsoScheduler.class);

    private final ReservaRepository reservaRepository;
    private final ReservaReembolsoService reservaReembolsoService;
    private final NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient;
    private final int maxBatchSize;

    public ReservaNotificacionReembolsoScheduler(
            ReservaRepository reservaRepository,
            ReservaReembolsoService reservaReembolsoService,
            NotificacionReembolsoIntegracionClient notificacionReembolsoIntegracionClient,
            @Value("${patiperro.reserva.notificacion-reembolso.scheduler.max-batch-size:30}") int maxBatchSize) {
        this.reservaRepository = reservaRepository;
        this.reservaReembolsoService = reservaReembolsoService;
        this.notificacionReembolsoIntegracionClient = notificacionReembolsoIntegracionClient;
        this.maxBatchSize = Math.max(1, maxBatchSize);
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
                PageRequest.of(0, maxBatchSize));
        if (ids.isEmpty()) {
            return;
        }
        int fallos = 0;
        for (Integer id : ids) {
            try {
                reservaReembolsoService.reintentarNotificacionReembolsoTutorSync(id);
            } catch (RuntimeException e) {
                fallos++;
                log.warn("Job notificación reembolso: fallo idReserva={}", id, e);
            }
        }
        long duracionMs = (System.nanoTime() - t0) / 1_000_000L;
        log.info(
                "Job notificación reembolso: candidatos={} fallos={} maxLote={} duracionMs={}",
                ids.size(),
                fallos,
                maxBatchSize,
                duracionMs);
    }
}
