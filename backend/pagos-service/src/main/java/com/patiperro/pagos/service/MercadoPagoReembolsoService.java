package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.dto.MercadoPagoRefundResponseDto;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.PagoExterno;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Reembolso total vía Mercado Pago con idempotencia (BD local + estado del pago en MP).
 */
@Service
@RequiredArgsConstructor
public class MercadoPagoReembolsoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoReembolsoService.class);

    private final MercadoPagoApiClient mercadoPagoApiClient;
    private final PagoExternoRepository pagoExternoRepository;
    private final TransaccionRepository transaccionRepository;

    /**
     * @param idReserva opcional, solo para logs y clave de idempotencia hacia MP
     * @param idempotencyKey cabecera opcional {@code Idempotency-Key}; si viene vacía se deriva de reserva + pago (estable entre reintentos)
     * @return código HTTP sugerido para el controller (204, 400, 409, 502, …)
     */
    @Transactional
    public int procesarReembolsoTotal(Integer idReserva, String mpPaymentIdRaw, String idempotencyKey) {
        String mpPaymentId = MercadoPagoApiClient.normalizarPaymentId(mpPaymentIdRaw);
        if (!StringUtils.hasText(mpPaymentId) && idReserva != null) {
            mpPaymentId = resolverMpPaymentIdPorIdReserva(idReserva)
                    .map(MercadoPagoApiClient::normalizarPaymentId)
                    .filter(StringUtils::hasText)
                    .orElse(null);
        }
        if (!StringUtils.hasText(mpPaymentId)) {
            return 400;
        }

        Optional<PagoExterno> optExt = pagoExternoRepository.findByProviderAndProviderPaymentId(
                PagoExternoService.PROVIDER_MERCADOPAGO, mpPaymentId);
        if (optExt.isPresent() && StringUtils.hasText(optExt.get().getRefundProviderId())) {
            log.info("Reembolso idempotente: ya registrado en pago_externo (mpPaymentId={}, idReserva={}, refundId={})",
                    mpPaymentId, idReserva, optExt.get().getRefundProviderId());
            return 204;
        }

        Optional<MercadoPagoPaymentDto> pago0 = mercadoPagoApiClient.obtenerPago(mpPaymentId);
        if (pago0.isEmpty()) {
            log.warn("Reembolso: no se pudo consultar pago {} en MP (idReserva={})", mpPaymentId, idReserva);
            return 502;
        }

        MercadoPagoPaymentDto pago = pago0.get();
        String status = safe(pago.status());

        if (yaConstituyeReembolsoEnMp(pago)) {
            sincronizarRefundEnPagoExterno(optExt, pago);
            log.info("Reembolso idempotente: MP ya marca devolución (mpPaymentId={}, idReserva={}, status={})",
                    mpPaymentId, idReserva, status);
            return 204;
        }

        if (!"approved".equalsIgnoreCase(status != null ? status : "")) {
            log.warn("Reembolso rechazado: pago {} no está approved (status={}, idReserva={})", mpPaymentId, status, idReserva);
            return 409;
        }

        String keyMp = MercadoPagoApiClient.sanitizeIdempotencyKey(idempotencyKey);
        if (!StringUtils.hasText(keyMp)) {
            String rid = idReserva != null ? String.valueOf(idReserva) : "na";
            keyMp = MercadoPagoApiClient.sanitizeIdempotencyKey(
                    "patiperro-reembolso-reserva-" + rid + "-mp-" + mpPaymentId);
        }

        Optional<MercadoPagoRefundResponseDto> refundResp = mercadoPagoApiClient.crearReembolsoTotal(mpPaymentId, keyMp);
        if (refundResp.isPresent()) {
            MercadoPagoRefundResponseDto r = refundResp.get();
            optExt.ifPresent(e -> aplicarRefundDesdeRespuesta(e, r));
            log.info("Reembolso MP creado (mpPaymentId={}, idReserva={}, refundId={}, status={})",
                    mpPaymentId, idReserva, r.idAsString(), r.status());
            return 204;
        }

        Optional<MercadoPagoPaymentDto> pagoRefresh = mercadoPagoApiClient.obtenerPago(mpPaymentId);
        if (pagoRefresh.isPresent() && yaConstituyeReembolsoEnMp(pagoRefresh.get())) {
            sincronizarRefundEnPagoExterno(optExt, pagoRefresh.get());
            log.info("Reembolso idempotente tras POST fallido: MP muestra devolución (mpPaymentId={}, idReserva={})",
                    mpPaymentId, idReserva);
            return 204;
        }

        log.warn("Reembolso: MP no devolvió éxito ni estado reembolsado tras intento (mpPaymentId={}, idReserva={})",
                mpPaymentId, idReserva);
        return 502;
    }

    private static boolean yaConstituyeReembolsoEnMp(MercadoPagoPaymentDto pago) {
        String status = safe(pago.status());
        if ("refunded".equalsIgnoreCase(status)) {
            return true;
        }
        return pago.tieneReembolsosRegistrados();
    }

    private void aplicarRefundDesdeRespuesta(PagoExterno e, MercadoPagoRefundResponseDto r) {
        e.setRefundProviderId(r.idAsString());
        e.setRefundStatus(safe(r.status()));
        e.setRefundFecha(LocalDateTime.now());
        pagoExternoRepository.save(e);
    }

    private void sincronizarRefundEnPagoExterno(Optional<PagoExterno> optExt, MercadoPagoPaymentDto pago) {
        if (optExt.isEmpty()) {
            return;
        }
        PagoExterno e = optExt.get();
        if (StringUtils.hasText(e.getRefundProviderId())) {
            return;
        }
        String refundId = extraerPrimerRefundId(pago);
        if (refundId != null) {
            e.setRefundProviderId(refundId);
        }
        e.setRefundStatus(safe(pago.status()));
        e.setRefundFecha(LocalDateTime.now());
        pagoExternoRepository.save(e);
    }

    private static String extraerPrimerRefundId(MercadoPagoPaymentDto pago) {
        if (pago.refunds() == null || pago.refunds().isEmpty()) {
            return null;
        }
        Object first = pago.refunds().get(0);
        if (first instanceof Map<?, ?> m && m.get("id") != null) {
            return String.valueOf(m.get("id"));
        }
        return null;
    }

    private Optional<String> resolverMpPaymentIdPorIdReserva(Integer idReserva) {
        if (idReserva == null) {
            return Optional.empty();
        }
        Optional<Transaccion> txOpt = transaccionRepository.findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(
                idReserva.longValue(), EstadoPago.APROBADO);
        if (txOpt.isEmpty()) {
            return Optional.empty();
        }
        Transaccion tx = txOpt.get();
        Optional<PagoExterno> peOpt = pagoExternoRepository.findByTransaccion_IdTransaccion(tx.getIdTransaccion());
        if (peOpt.isPresent()) {
            PagoExterno pe = peOpt.get();
            if (StringUtils.hasText(pe.getProviderPaymentId())) {
                return Optional.of(pe.getProviderPaymentId().trim());
            }
        }
        if (tx.getIdPago() != null) {
            return Optional.of(String.valueOf(tx.getIdPago()));
        }
        return Optional.empty();
    }

    private static String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
