package com.patiperro.pagos.service;

import com.patiperro.pagos.config.PagosWebhookProperties;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Procesamiento pesado del webhook: consulta MP y avisa a reserva-service.
 * Ejecutado en hilo aparte para responder 200 rápido al webhook.
 * <p>Fusiona: persistencia transacción/pago externo solo en {@code approved} (V1) más notificación de
 * rechazados e ignorar estados intermedios (V2).</p>
 */
@Component
public class MercadoPagoWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookProcessor.class);

    private final MercadoPagoApiClient mercadoPagoApiClient;
    private final ReservaPagosIntegracionClient reservaPagosIntegracionClient;
    private final TransaccionRepository transaccionRepository;
    private final PagoExternoService pagoExternoService;
    private final NetoTransaccionService netoTransaccionService;
    private final PagosWebhookProperties pagosWebhookProperties;

    public MercadoPagoWebhookProcessor(
            MercadoPagoApiClient mercadoPagoApiClient,
            ReservaPagosIntegracionClient reservaPagosIntegracionClient,
            TransaccionRepository transaccionRepository,
            PagoExternoService pagoExternoService,
            NetoTransaccionService netoTransaccionService,
            PagosWebhookProperties pagosWebhookProperties) {
        this.mercadoPagoApiClient = mercadoPagoApiClient;
        this.reservaPagosIntegracionClient = reservaPagosIntegracionClient;
        this.transaccionRepository = transaccionRepository;
        this.pagoExternoService = pagoExternoService;
        this.netoTransaccionService = netoTransaccionService;
        this.pagosWebhookProperties = pagosWebhookProperties;
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

            Integer idReserva = resolverIdReserva(pago);

            if ("approved".equalsIgnoreCase(status != null ? status : "")) {
                if (idReserva == null) {
                    log.warn("Webhook MP: pago aprobado {} sin external_reference reconocible para id reserva", mpId);
                    return;
                }
                if (pagosWebhookProperties.isLogApprovedDetails()) {
                    log.info(
                            "Webhook MP: pago approved detalle (mpPaymentId={}, idReserva={}, transaction_amount={}, currency_id={}, date_approved={}, payment_type_id={})",
                            mpId,
                            idReserva,
                            pago.transactionAmount(),
                            pago.currencyId(),
                            pago.dateApproved(),
                            pago.paymentTypeId());
                }
                advertirSiMonedaInesperada(pago, mpId, idReserva);

                Long idTransaccionPagos = null;
                boolean transicionDesdePendiente = false;
                try {
                    Optional<Transaccion> pendiente = transaccionRepository
                            .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                                    idReserva.longValue(), EstadoPago.PENDIENTE);
                    if (pendiente.isPresent()) {
                        Transaccion tx = pendiente.get();
                        BigDecimal brutoCheckoutPrevio = tx.getMontoBruto();
                        aplicarMontosNetoDesdePago(tx, pago);
                        advertirSiMontoCheckoutDifiereDeMp(brutoCheckoutPrevio, pago, idReserva, mpId);
                        pagoExternoService.upsertMercadoPagoPagoExterno(tx, pago, null);
                        tx.setEstadoPago(EstadoPago.APROBADO);
                        transaccionRepository.save(tx);
                        idTransaccionPagos = tx.getIdTransaccion();
                        transicionDesdePendiente = true;
                    }
                } catch (RuntimeException ex) {
                    log.warn("Webhook MP: no se pudo persistir pago externo / transacción (idReserva={})", idReserva, ex);
                }
                if (idTransaccionPagos == null) {
                    idTransaccionPagos = transaccionRepository
                            .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                                    idReserva.longValue(), EstadoPago.APROBADO)
                            .map(Transaccion::getIdTransaccion)
                            .orElse(null);
                }
                if (!transicionDesdePendiente && idTransaccionPagos != null) {
                    log.debug(
                            "Webhook MP: pago {} sin fila PENDIENTE (ya aprobado o reintento); se notifica reserva por idempotencia (idReserva={}, idTransaccion={})",
                            mpId, idReserva, idTransaccionPagos);
                }
                if (idTransaccionPagos != null) {
                    reservaPagosIntegracionClient.notificarPagoAprobado(idReserva, idTransaccionPagos, mpId);
                    log.info("Webhook MP: notificado pago aprobado a reserva-service (idReserva={}, idTransaccion={}, mpPaymentId={})",
                            idReserva, idTransaccionPagos, mpId);
                } else {
                    log.warn("Webhook MP: pago aprobado {} sin transacción PENDIENTE ni APROBADA (idReserva={})", mpId,
                            idReserva);
                }
                return;
            }

            if (!StringUtils.hasText(status)) {
                log.warn("Webhook MP: pago {} sin status en respuesta MP; sin acción en reserva", mpId);
                return;
            }

            if (esEstadoIntermedioMercadoPago(status)) {
                log.info("Webhook MP: pago {} status={} (intermedio); no se notifica reserva-service", mpId, status);
                return;
            }

            if (idReserva == null) {
                log.warn("Webhook MP: pago no aprobado {} sin external_reference reconocible para id reserva", mpId);
                return;
            }

            String detail = pago.statusDetail();
            reservaPagosIntegracionClient.notificarPagoNoAprobado(idReserva, mpId, status, detail);
            log.info("Webhook MP: notificado pago no aprobado a reserva-service (idReserva={}, mpPaymentId={}, status={})",
                    idReserva, mpId, status);
        } catch (RuntimeException e) {
            log.error("Webhook MP: error procesando notificación (topic={}, paymentId={})", topic, paymentId, e);
        }
    }

    /**
     * Persiste bruto (MP o checkout), comisión y neto. Ante fallo inesperado mantiene comportamiento seguro (sin comisión, neto ≈ bruto).
     */
    private void advertirSiMonedaInesperada(MercadoPagoPaymentDto pago, String mpId, Integer idReserva) {
        if (!pagosWebhookProperties.isWarnOnCurrencyMismatch()) {
            return;
        }
        String esperada = pagosWebhookProperties.getExpectedCurrencyId();
        String actual = pago.currencyId();
        if (!StringUtils.hasText(esperada) || !StringUtils.hasText(actual)) {
            return;
        }
        if (actual.trim().equalsIgnoreCase(esperada.trim())) {
            return;
        }
        log.warn(
                "Webhook MP: currency_id del pago ({}) distinto del esperado ({}) — sin abortar (mpPaymentId={}, idReserva={})",
                actual.trim(),
                esperada.trim(),
                mpId,
                idReserva);
    }

    /**
     * Solo observabilidad: no modifica montos (ya aplicados desde MP / checkout).
     */
    private void advertirSiMontoCheckoutDifiereDeMp(
            BigDecimal montoBrutoCheckoutPrevio,
            MercadoPagoPaymentDto pago,
            Integer idReserva,
            String mpId) {
        if (!pagosWebhookProperties.isWarnOnCheckoutMpAmountMismatch()) {
            return;
        }
        if (montoBrutoCheckoutPrevio == null || montoBrutoCheckoutPrevio.signum() <= 0) {
            return;
        }
        BigDecimal mpAmount = pago.transactionAmount();
        if (mpAmount == null || mpAmount.signum() <= 0) {
            return;
        }
        BigDecimal tol = pagosWebhookProperties.getAmountMismatchToleranceClp();
        if (tol == null || tol.signum() < 0) {
            tol = BigDecimal.ONE;
        }
        tol = tol.setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = montoBrutoCheckoutPrevio.subtract(mpAmount).abs().setScale(2, RoundingMode.HALF_UP);
        if (diff.compareTo(tol) > 0) {
            log.warn(
                    "Webhook MP: monto checkout {} vs transaction_amount MP {} (diff={}) — idReserva={}, mpPaymentId={}",
                    montoBrutoCheckoutPrevio,
                    mpAmount,
                    diff,
                    idReserva,
                    mpId);
        }
    }

    private void aplicarMontosNetoDesdePago(Transaccion tx, MercadoPagoPaymentDto pago) {
        try {
            BigDecimal bruto = netoTransaccionService.resolverMontoBruto(pago, tx);
            NetoTransaccionService.ResultadoNeto netos = netoTransaccionService.calcularConFallbackSinComision(bruto);
            tx.setMontoBruto(bruto);
            tx.setComisionApp(netos.comisionApp());
            tx.setMontoNeto(netos.montoNeto());
        } catch (RuntimeException ex) {
            log.warn("Webhook MP: no se aplicaron netos desde MP; se conserva bruto de transacción sin comisión (idReserva={})",
                    tx.getIdReserva(), ex);
            BigDecimal fb = tx.getMontoBruto() != null ? tx.getMontoBruto() : BigDecimal.ZERO;
            tx.setComisionApp(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            tx.setMontoNeto(fb.setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * Estados donde el cobro puede seguir evolucionando; no persistimos intento fallido todavía.
     */
    private static boolean esEstadoIntermedioMercadoPago(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String s = status.trim().toLowerCase();
        return "pending".equals(s)
                || "in_process".equals(s)
                || "authorized".equals(s);
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
