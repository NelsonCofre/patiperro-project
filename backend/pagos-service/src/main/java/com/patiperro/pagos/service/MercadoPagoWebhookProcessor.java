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
 * <p>
 * Fusiona: persistencia transacción/pago externo solo en {@code approved} (V1)
 * más notificación de
 * rechazados e ignorar estados intermedios (V2).
 * </p>
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
            ejecutarProcesamientoMercadoPago(topic, paymentId);
        } catch (RuntimeException e) {
            log.error("Webhook MP: error procesando notificación (topic={}, paymentId={})", topic, paymentId, e);
        }
    }

    /**
     * Misma lógica que el webhook, en hilo actual (p. ej. retorno Checkout Pro en local sin IPN).
     */
    public void procesarSincrono(String paymentId) {
        ejecutarProcesamientoMercadoPago("payment", paymentId);
    }

    private void ejecutarProcesamientoMercadoPago(String topic, String paymentId) {
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
            String mpId = StringUtils.hasText(pago.idAsString()) ? pago.idAsString()
                    : MercadoPagoApiClient.normalizarPaymentId(paymentId);

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
                    log.warn("Webhook MP: no se pudo persistir pago externo / transacción (idReserva={})", idReserva,
                            ex);
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
                    log.info(
                            "Webhook MP: notificado pago aprobado a reserva-service (idReserva={}, idTransaccion={}, mpPaymentId={})",
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
            log.info(
                    "Webhook MP: notificado pago no aprobado a reserva-service (idReserva={}, mpPaymentId={}, status={})",
                    idReserva, mpId, status);
        } catch (RuntimeException e) {
            log.error("Webhook MP: error en procesamiento (topic={}, paymentId={})", topic, paymentId, e);
            throw e;
        }
    }

    /**
     * Estados donde el cobro puede seguir evolucionando; no persistimos intento
     * fallido todavía.
     */
    public static boolean esEstadoIntermedioMercadoPago(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String s = status.trim().toLowerCase();
        return "pending".equals(s)
                || "in_process".equals(s)
                || "authorized".equals(s);
    }

    public static Integer resolverIdReserva(MercadoPagoPaymentDto pago) {
        String ref = pago.externalReference();
        if (!StringUtils.hasText(ref)) {
            return null;
        }
        ref = ref.trim();
        try {
            return Integer.parseInt(ref);
        } catch (NumberFormatException ignored) {
            if (ref.regionMatches(true, 0, "reserva-", 0, 8) && ref.length() > 8) {
                try {
                    return Integer.parseInt(ref.substring(8).trim());
                } catch (NumberFormatException ignored2) {
                    // sigue con estrategia por ':'
                }
            }
            int colon = ref.lastIndexOf(':');
            if (colon >= 0 && colon < ref.length() - 1) {
                try {
                    return Integer.parseInt(ref.substring(colon + 1).trim());
                } catch (NumberFormatException ignored3) {
                    return null;
                }
            }
            return null;
        }
    }

    private void aplicarMontosNetoDesdePago(Transaccion tx, MercadoPagoPaymentDto pago) {
        if (tx == null) {
            return;
        }
        BigDecimal bruto = netoTransaccionService.resolverMontoBruto(pago, tx);
        tx.setMontoBruto(bruto);
        NetoTransaccionService.ResultadoNeto netos = netoTransaccionService.calcularConFallbackSinComision(bruto);
        tx.setComisionApp(netos.comisionApp());
        tx.setMontoNeto(netos.montoNeto());
    }

    private void advertirSiMontoCheckoutDifiereDeMp(
            BigDecimal montoBrutoCheckoutPrevio,
            MercadoPagoPaymentDto pago,
            Integer idReserva,
            String mpPaymentId) {
        if (!pagosWebhookProperties.isWarnOnCheckoutMpAmountMismatch()) {
            return;
        }
        if (montoBrutoCheckoutPrevio == null || pago == null || pago.transactionAmount() == null) {
            return;
        }
        BigDecimal previo = montoBrutoCheckoutPrevio.setScale(2, RoundingMode.HALF_UP);
        BigDecimal mp = pago.transactionAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = previo.subtract(mp).abs();
        BigDecimal tolerancia = Optional.ofNullable(pagosWebhookProperties.getAmountMismatchToleranceClp())
                .orElse(BigDecimal.ZERO)
                .abs()
                .setScale(2, RoundingMode.HALF_UP);
        if (diff.compareTo(tolerancia) > 0) {
            log.warn(
                    "Webhook MP: monto checkout difiere de MP sobre tolerancia (idReserva={}, mpPaymentId={}, brutoCheckoutPrevio={}, transaction_amount={}, diff={}, tolerancia={})",
                    idReserva, mpPaymentId, previo, mp, diff, tolerancia);
        }
    }

    private void advertirSiMonedaInesperada(MercadoPagoPaymentDto pago, String mpPaymentId, Integer idReserva) {
        if (!pagosWebhookProperties.isWarnOnCurrencyMismatch()) {
            return;
        }
        if (pago == null) {
            return;
        }
        String actual = pago.currencyId();
        String esperada = pagosWebhookProperties.getExpectedCurrencyId();
        if (!StringUtils.hasText(esperada) || !StringUtils.hasText(actual)) {
            return;
        }
        if (!esperada.trim().equalsIgnoreCase(actual.trim())) {
            log.warn(
                    "Webhook MP: moneda inesperada (idReserva={}, mpPaymentId={}, currency_id={}, expectedCurrencyId={})",
                    idReserva, mpPaymentId, actual, esperada);
        }
    }
}
