package com.patiperro.pagos.scheduler;

import com.patiperro.pagos.service.BilleteraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Libera fondos de verificación a disponible según regla calendario N+2 (desde {@code fecha_fin_servicio}).
 */
@Component
@ConditionalOnProperty(name = "patiperro.pagos.billetera.liberacion.enabled", havingValue = "true", matchIfMissing = true)
public class BilleteraLiberacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(BilleteraLiberacionScheduler.class);

    private final BilleteraService billeteraService;

    public BilleteraLiberacionScheduler(BilleteraService billeteraService) {
        this.billeteraService = billeteraService;
    }

    @Scheduled(cron = "${patiperro.pagos.billetera.liberacion.cron:0 0 * * * *}")
    public void liberarPendientes() {
        try {
            int n = billeteraService.ejecutarLiberacionesPendientes();
            if (n > 0) {
                log.info("Billetera liberación automática: {} reserva(s)", n);
            }
        } catch (RuntimeException e) {
            log.warn("Billetera liberación automática falló", e);
        }
    }
}
