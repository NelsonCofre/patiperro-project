package com.patiperro.reserva.service;

import com.patiperro.reserva.support.PagosComprobanteIntegracionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Best-effort post-commit hacia pagos-service sin bloquear el hilo que cerró la transacción.
 */
@Service
public class ReservaComprobantePostCommitRunner {

    private static final Logger log = LoggerFactory.getLogger(ReservaComprobantePostCommitRunner.class);

    private final PagosComprobanteIntegracionClient pagosComprobanteIntegracionClient;

    public ReservaComprobantePostCommitRunner(PagosComprobanteIntegracionClient pagosComprobanteIntegracionClient) {
        this.pagosComprobanteIntegracionClient = pagosComprobanteIntegracionClient;
    }

    /** Evita registrar trabajo tras commit si la integración está desactivada o mal configurada. */
    public boolean isSchedulingEnabled() {
        return pagosComprobanteIntegracionClient.isEnabled();
    }

    @Async
    public void generarBestEffort(Integer idReserva) {
        if (idReserva == null || !pagosComprobanteIntegracionClient.isEnabled()) {
            return;
        }
        if (!pagosComprobanteIntegracionClient.generarYEnviarResumen(idReserva, false)) {
            log.warn(
                    "Post-commit comprobante: pagos-service no confirmó éxito (idReserva={}); "
                            + "el tutor puede consultar luego GET /api/pagos/comprobante/<idReserva>",
                    idReserva);
        }
    }
}
