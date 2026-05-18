package com.patiperro.reserva.scheduler;

import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.service.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Marca como EXPIRADA las reservas pendientes de aceptación del paseador tras superar el plazo configurado.
 * <p>Cada ejecución procesa como mucho {@code maxBatchSize} candidatos; el resto queda para ticks posteriores.</p>
 */
@Component
@ConditionalOnProperty(name = "patiperro.reserva.solicitud.expiracion.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReservaExpiracionSolicitudScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaExpiracionSolicitudScheduler.class);

    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;
    private final Clock clock;
    private final long horasExpiracionAceptacion;
    private final int maxBatchSize;

    public ReservaExpiracionSolicitudScheduler(
            ReservaRepository reservaRepository,
            ReservaService reservaService,
            Clock clock,
            @Value("${patiperro.reserva.solicitud.expiracion-aceptacion.horas:24}") long horasExpiracionAceptacion,
            @Value("${patiperro.reserva.solicitud.expiracion.scheduler.max-batch-size:100}") int maxBatchSize) {
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
        this.clock = clock;
        this.horasExpiracionAceptacion = horasExpiracionAceptacion;
        this.maxBatchSize = Math.max(1, maxBatchSize);
    }

    @Scheduled(
            initialDelayString = "${patiperro.reserva.solicitud.expiracion.scheduler.initial-delay-ms:0}",
            fixedDelayString = "${patiperro.reserva.solicitud.expiracion.scheduler.fixed-delay-ms:300000}")
    public void expirarSolicitudesAntiguas() {
        long t0 = System.nanoTime();
        LocalDateTime limite = LocalDateTime.now(clock).minusHours(Math.max(1, horasExpiracionAceptacion));
        List<Integer> ids = reservaRepository.findIdReservasParaExpiracionPorPlazoAceptacion(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_PENDIENTE_PAGO,
                EstadoReservaCatalogo.ID_PAGADA,
                limite,
                PageRequest.of(0, maxBatchSize));
        if (ids.isEmpty()) {
            return;
        }
        int ok = 0;
        int fallos = 0;
        for (Integer id : ids) {
            try {
                reservaService.expirarReservaPorPlazoAceptacionJobItem(id);
                ok++;
            } catch (RuntimeException e) {
                fallos++;
                log.warn("Expiración solicitudes: fallo idReserva={}", id, e);
            }
        }
        long duracionMs = (System.nanoTime() - t0) / 1_000_000L;
        log.info(
                "Expiración solicitudes por plazo: ok={} fallos={} tamañoLote={} maxLote={} duracionMs={}",
                ok,
                fallos,
                ids.size(),
                maxBatchSize,
                duracionMs);
    }
}
