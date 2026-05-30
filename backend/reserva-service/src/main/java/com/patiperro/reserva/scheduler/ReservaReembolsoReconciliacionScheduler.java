package com.patiperro.reserva.scheduler;

import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.service.ReservaReembolsoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Paso 6 (solo backend): reconciliación — reintenta devolución MP + correo tutor cuando la reserva ya está
 * en estado que amerita reembolso ({@link com.patiperro.reserva.model.EstadoReservaCatalogo#IDS_ESTADO_REEMBOLSO_MERCADOPAGO}),
 * tiene {@code mercadopago_payment_id} y aún no tiene {@code mercadopago_reembolso_procesado_en}
 * (p. ej. fallo tras commit, caída del proceso o error transitorio de pagos).
 */
@Component
@ConditionalOnProperty(name = "patiperro.reserva.reembolso.reconciliacion.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReservaReembolsoReconciliacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaReembolsoReconciliacionScheduler.class);

    private final ReservaRepository reservaRepository;
    private final ReservaReembolsoService reservaReembolsoService;
    private final int maxBatchSize;
    private final long pauseBetweenItemsMs;

    public ReservaReembolsoReconciliacionScheduler(
            ReservaRepository reservaRepository,
            ReservaReembolsoService reservaReembolsoService,
            @Value("${patiperro.reserva.reembolso.reconciliacion.scheduler.max-batch-size:50}") int maxBatchSize,
            @Value("${patiperro.reserva.reembolso.reconciliacion.scheduler.pause-between-items-ms:0}") long pauseBetweenItemsMs) {
        this.reservaRepository = reservaRepository;
        this.reservaReembolsoService = reservaReembolsoService;
        this.maxBatchSize = Math.max(1, maxBatchSize);
        this.pauseBetweenItemsMs = Math.max(0, pauseBetweenItemsMs);
    }

    @Scheduled(
            initialDelayString = "${patiperro.reserva.reembolso.reconciliacion.scheduler.initial-delay-ms:120000}",
            fixedDelayString = "${patiperro.reserva.reembolso.reconciliacion.scheduler.fixed-delay-ms:600000}")
    public void reconciliarReembolsosPendientes() {
        long t0 = System.nanoTime();
        List<Integer> ids = reservaRepository.findIdReservasConCobroEnEstados(
                EstadoReservaCatalogo.IDS_ESTADO_REEMBOLSO_MERCADOPAGO,
                PageRequest.of(0, maxBatchSize));
        if (ids.isEmpty()) {
            return;
        }
        int fallos = 0;
        for (int i = 0; i < ids.size(); i++) {
            Integer id = ids.get(i);
            try {
                reservaReembolsoService.procesarReembolsoYNotificarSync(id);
            } catch (RuntimeException e) {
                fallos++;
                log.warn("Reconciliación reembolso MP: fallo idReserva={}", id, e);
            }
            dormirEntreItemsSiAplica(i, ids.size());
        }
        long duracionMs = (System.nanoTime() - t0) / 1_000_000L;
        log.info(
                "Reconciliación reembolso MP: candidatos={} fallos={} maxLote={} duracionMs={}",
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
            log.warn("Reconciliación reembolso MP: interrupción en pausa entre ítems");
        }
    }
}
