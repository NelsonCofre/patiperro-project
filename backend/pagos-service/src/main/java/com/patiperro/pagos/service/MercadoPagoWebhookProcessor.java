package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import com.patiperro.pagos.support.ReservaPagosIntegracionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Procesamiento pesado del webhook: consulta MP y avisa a reserva-service.
 * Ejecutado en hilo aparte para responder 200 rápido al webhook.
 */
@Component
public class MercadoPagoWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookProcessor.class);

    private final MercadoPagoApiClient mercadoPagoApiClient;
    private final ReservaPagosIntegracionClient reservaPagosIntegracionClient;
    private final TransaccionRepository transaccionRepository;
    private final PagoExternoService pagoExternoService;

    public MercadoPagoWebhookProcessor(
            MercadoPagoApiClient mercadoPagoApiClient,
            ReservaPagosIntegracionClient reservaPagosIntegracionClient,
            TransaccionRepository transaccionRepository,
            PagoExternoService pagoExternoService) {
        this.mercadoPagoApiClient = mercadoPagoApiClient;
        this.reservaPagosIntegracionClient = reservaPagosIntegracionClient;
        this.transaccionRepository = transaccionRepository;
        this.pagoExternoService = pagoExternoService;
    }

    @Async
    public void procesar(String topic, String paymentId) {
        try {
            if (!StringUtils.hasText(paymentId)) {
                return;
            }
            if (StringUtils.hasText(topic) && !"payment".equalsIgnoreCase(topic.trim())) {
                log.info("Webhook MP ignorado: topic distinto de payment (topic={}, paymentId={})", topic, paymentId);
                return;
            }

            Optional<MercadoPagoPaymentDto> pagoOpt = mercadoPagoApiClient.obtenerPago(paymentId);
            if (pagoOpt.isEmpty()) {
                log.warn("Webhook MP: no se pudo obtener pago {} desde API", paymentId);
                return;
            }

            MercadoPagoPaymentDto pago = pagoOpt.get();
            String status = pago.status();
            String mpId = StringUtils.hasText(pago.idAsString()) ? pago.idAsString() : MercadoPagoApiClient.normalizarPaymentId(paymentId);

            if (!"approved".equalsIgnoreCase(status != null ? status : "")) {
                log.info("Webhook MP: pago {} con status={}, no se marca reserva PAGADA", mpId, status);
                return;
            }

            Integer idReserva = resolverIdReserva(pago);
            if (idReserva == null) {
                log.warn("Webhook MP: pago aprobado {} sin external_reference reconocible para id reserva", mpId);
                return;
            }

            Long idTransaccionParaReserva = null;
            try {
                Optional<Transaccion> pendiente = transaccionRepository
                        .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                                idReserva.longValue(), EstadoPago.PENDIENTE);
                if (pendiente.isPresent()) {
                    Transaccion tx = pendiente.get();
                    pagoExternoService.upsertMercadoPagoPagoExterno(tx, pago, null);
                    tx.setEstadoPago(EstadoPago.APROBADO);
                    if (pago.id() != null) {
                        tx.setIdPago(pago.id());
                    }
                    transaccionRepository.save(tx);
                    idTransaccionParaReserva = tx.getIdTransaccion();
                } else {
                    idTransaccionParaReserva = transaccionRepository
                            .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                                    idReserva.longValue(), EstadoPago.APROBADO)
                            .map(Transaccion::getIdTransaccion)
                            .orElse(null);
                }
            } catch (RuntimeException ex) {
                log.warn("Webhook MP: no se pudo persistir pago externo / transacción (idReserva={})", idReserva, ex);
            }

            if (idTransaccionParaReserva != null) {
                reservaPagosIntegracionClient.notificarPagoAprobado(idReserva, idTransaccionParaReserva, mpId);
                log.info("Webhook MP: notificado pago aprobado a reserva-service (idReserva={}, idTransaccion={}, mpPaymentId={})",
                        idReserva, idTransaccionParaReserva, mpId);
            } else {
                log.warn("Webhook MP: pago aprobado {} sin transacción local (idReserva={}); no se notifica reserva-service",
                        mpId, idReserva);
            }
        } catch (RuntimeException e) {
            log.error("Webhook MP: error procesando notificación (topic={}, paymentId={})", topic, paymentId, e);
        }
    }

    private static Integer resolverIdReserva(MercadoPagoPaymentDto pago) {
        String ref = pago.externalReference();
        if (!StringUtils.hasText(ref)) {
            return null;
        }
        ref = ref.trim();
        try {
            return Integer.parseInt(ref);
        } catch (NumberFormatException ignored) {
            int colon = ref.lastIndexOf(':');
            if (colon >= 0 && colon < ref.length() - 1) {
                try {
                    return Integer.parseInt(ref.substring(colon + 1).trim());
                } catch (NumberFormatException ignored2) {
                    return null;
                }
            }
            return null;
        }
    }
}
