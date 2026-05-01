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
 * Reintenta devolución MP + correo tutor cuando la reserva ya está en estado que amerita reembolso,
 * tiene {@code mercadopago_payment_id} y aún no tiene {@code mercadopago_reembolso_procesado_en}
 * (p. ej. fallo tras commit, caída del proceso o error transitorio de pagos).
 */
@Component
@ConditionalOnProperty(name = "patiperro.reserva.reembolso.reconciliacion.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReservaReembolsoReconciliacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaReembolsoReconciliacionScheduler.class);

    private static final List<Integer> IDS_ESTADO_REEMBOLSO = List.of(
            EstadoReservaCatalogo.ID_RECHAZADA,
            EstadoReservaCatalogo.ID_EXPIRADA,
            EstadoReservaCatalogo.ID_CANCELADA);

    private final ReservaRepository reservaRepository;
    private final ReservaReembolsoService reservaReembolsoService;
    private final int maxBatchSize;

    public ReservaReembolsoReconciliacionScheduler(
            ReservaRepository reservaRepository,
            ReservaReembolsoService reservaReembolsoService,
            @Value("${patiperro.reserva.reembolso.reconciliacion.scheduler.max-batch-size:50}") int maxBatchSize) {
        this.reservaRepository = reservaRepository;
        this.reservaReembolsoService = reservaReembolsoService;
        this.maxBatchSize = Math.max(1, maxBatchSize);
    }

    @Scheduled(
            initialDelayString = "${patiperro.reserva.reembolso.reconciliacion.scheduler.initial-delay-ms:120000}",
            fixedDelayString = "${patiperro.reserva.reembolso.reconciliacion.scheduler.fixed-delay-ms:600000}")
    public void reconciliarReembolsosPendientes() {
        long t0 = System.nanoTime();
        List<Integer> ids = reservaRepository.findIdReservasPendientesReconciliacionReembolsoMercadoPago(
                IDS_ESTADO_REEMBOLSO,
                PageRequest.of(0, maxBatchSize));
        if (ids.isEmpty()) {
            return;
        }
        int fallos = 0;
        for (Integer id : ids) {
            try {
                reservaReembolsoService.procesarReembolsoYNotificarSync(id);
            } catch (RuntimeException e) {
                fallos++;
                log.warn("Reconciliación reembolso MP: fallo idReserva={}", id, e);
            }
        }
        long duracionMs = (System.nanoTime() - t0) / 1_000_000L;
        log.info(
                "Reconciliación reembolso MP: candidatos={} fallos={} maxLote={} duracionMs={}",
                ids.size(),
                fallos,
                maxBatchSize,
                duracionMs);
    }
}
