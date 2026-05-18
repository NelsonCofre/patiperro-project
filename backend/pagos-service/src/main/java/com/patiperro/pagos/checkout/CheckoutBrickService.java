package com.patiperro.pagos.checkout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.checkout.dto.BrickPagoRequest;
import com.patiperro.pagos.checkout.dto.PagoBrickResponseDto;
import com.patiperro.pagos.dto.MercadoPagoPaymentDto;
import com.patiperro.pagos.model.Destino;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Origen;
import com.patiperro.pagos.model.TipoTransaccion;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaConsultaDto;
import com.patiperro.pagos.service.MercadoPagoWebhookProcessor;
import com.patiperro.pagos.service.PagoExternoService;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import com.patiperro.pagos.support.MercadoPagoCrearPagoException;
import com.patiperro.pagos.support.ReservaPagosIntegracionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cobro con Payment Brick: {@code POST /v1/payments} con token (sin preferencia Checkout Pro).
 */
@Service
public class CheckoutBrickService {

    private static final String MONEDA_PAGO = "CLP";

    private final ReservaConsultaClient reservaConsultaClient;
    private final TransaccionRepository transaccionRepository;
    private final ReservaPagosIntegracionClient reservaPagosIntegracionClient;
    private final MercadoPagoApiClient mercadoPagoApiClient;
    private final PagoExternoService pagoExternoService;
    private final ObjectMapper objectMapper;
    private final String notificationUrl;

    public CheckoutBrickService(
            ReservaConsultaClient reservaConsultaClient,
            TransaccionRepository transaccionRepository,
            ReservaPagosIntegracionClient reservaPagosIntegracionClient,
            MercadoPagoApiClient mercadoPagoApiClient,
            PagoExternoService pagoExternoService,
            ObjectMapper objectMapper,
            @Value("${patiperro.pagos.checkout.notification-url:}") String notificationUrl) {
        this.reservaConsultaClient = reservaConsultaClient;
        this.transaccionRepository = transaccionRepository;
        this.reservaPagosIntegracionClient = reservaPagosIntegracionClient;
        this.mercadoPagoApiClient = mercadoPagoApiClient;
        this.pagoExternoService = pagoExternoService;
        this.objectMapper = objectMapper;
        this.notificationUrl = notificationUrl == null ? "" : notificationUrl.trim();
    }

    @Transactional
    public PagoBrickResponseDto procesar(BrickPagoRequest req, long usuarioIdAutenticado) {
        if (req == null || req.idReserva() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idReserva es obligatorio");
        }
        if (!StringUtils.hasText(req.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token de pago ausente");
        }
        if (!StringUtils.hasText(req.paymentMethodId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentMethodId es obligatorio");
        }
        if (!StringUtils.hasText(req.payerEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email del pagador es obligatorio");
        }

        ReservaConsultaDto reserva;
        try {
            reserva = reservaConsultaClient.obtenerReservaParaPago(req.idReserva());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        if (!Long.valueOf(usuarioIdAutenticado).equals(reserva.idTutorUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La reserva no pertenece al tutor autenticado");
        }
        if (reserva.montoTotal() == null || reserva.montoTotal().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Monto inválido");
        }

        int cuotas = req.installments() != null && req.installments() > 0 ? req.installments() : 1;

        Transaccion tx = transaccionRepository
                .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(reserva.idReserva(), EstadoPago.PENDIENTE)
                .map(existing -> {
                    existing.setMontoBruto(reserva.montoTotal());
                    existing.setComisionApp(BigDecimal.ZERO);
                    existing.setMontoNeto(reserva.montoTotal());
                    return existing;
                })
                .orElseGet(() -> Transaccion.builder()
                        .idReserva(reserva.idReserva())
                        .montoBruto(reserva.montoTotal())
                        .comisionApp(BigDecimal.ZERO)
                        .montoNeto(reserva.montoTotal())
                        .origen(Origen.CLIENTE)
                        .destino(Destino.BANCO)
                        .estadoPago(EstadoPago.PENDIENTE)
                        .tipoTransaccion(TipoTransaccion.PAGO_CLIENTE)
                        .build());
        transaccionRepository.save(tx);
        reservaPagosIntegracionClient.vincularTransaccionReserva(reserva.idReserva().intValue(), tx.getIdTransaccion());

        BigDecimal montoClp = reserva.montoTotal().setScale(0, java.math.RoundingMode.HALF_UP);
        double transactionAmount = montoClp.doubleValue();

        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", req.payerEmail().trim());
        if (StringUtils.hasText(req.identificationType()) && StringUtils.hasText(req.identificationNumber())) {
            Map<String, String> idMap = new LinkedHashMap<>();
            idMap.put("type", req.identificationType().trim());
            idMap.put("number", req.identificationNumber().trim());
            payer.put("identification", idMap);
        }

        String extRef = MercadoPagoReservaExternalReference.fromReservaId(reserva.idReserva());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", transactionAmount);
        body.put("token", req.token().trim());
        body.put("description", "Reserva Patiperro n.º " + reserva.idReserva());
        body.put("installments", cuotas);
        body.put("payment_method_id", req.paymentMethodId().trim());
        body.put("payer", payer);
        body.put("external_reference", extRef);
        body.put("currency_id", MONEDA_PAGO);
        if (StringUtils.hasText(req.issuerId())) {
            body.put("issuer_id", req.issuerId().trim());
        }
        if (StringUtils.hasText(notificationUrl)) {
            body.put("notification_url", notificationUrl);
        }

        String idempotencyKey = "brick-reserva-" + reserva.idReserva() + "-tx-" + tx.getIdTransaccion();

        MercadoPagoPaymentDto pago;
        try {
            pago = mercadoPagoApiClient.crearPagoConToken(body, idempotencyKey);
        } catch (MercadoPagoCrearPagoException e) {
            if (e.getHttpStatus() >= 400 && e.getHttpStatus() < 500) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeMercadoPagoUsuario(e.getResponseBody()));
            }
            if (e.getHttpStatus() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo contactar a Mercado Pago.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mercado Pago no procesó el pago.");
        }

        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(pago);
        } catch (JsonProcessingException e) {
            rawJson = null;
        }

        String status = pago.status() != null ? pago.status().trim() : "";
        String mpId = Optional.ofNullable(pago.idAsString()).orElse("");

        if ("approved".equalsIgnoreCase(status)) {
            pagoExternoService.upsertMercadoPagoPagoExterno(tx, pago, rawJson);
            tx.setEstadoPago(EstadoPago.APROBADO);
            transaccionRepository.save(tx);
            reservaPagosIntegracionClient.notificarPagoAprobado(
                    reserva.idReserva().intValue(), tx.getIdTransaccion(), mpId);
            return new PagoBrickResponseDto(mpId, status, safeDetail(pago.statusDetail()));
        }

        if (MercadoPagoWebhookProcessor.esEstadoIntermedioMercadoPago(status)) {
            pagoExternoService.upsertMercadoPagoPagoExterno(tx, pago, rawJson);
            transaccionRepository.save(tx);
            return new PagoBrickResponseDto(mpId, status, safeDetail(pago.statusDetail()));
        }

        pagoExternoService.upsertMercadoPagoPagoExterno(tx, pago, rawJson);
        tx.setEstadoPago(EstadoPago.RECHAZADO);
        transaccionRepository.save(tx);
        reservaPagosIntegracionClient.notificarPagoNoAprobado(
                reserva.idReserva().intValue(), mpId, status, safeDetail(pago.statusDetail()));
        return new PagoBrickResponseDto(mpId, status, safeDetail(pago.statusDetail()));
    }

    private static String safeDetail(String d) {
        return d == null ? "" : d.trim();
    }

    private String mensajeMercadoPagoUsuario(String json) {
        if (!StringUtils.hasText(json)) {
            return "Mercado Pago rechazó los datos del pago.";
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("message") && root.get("message").isTextual()) {
                return root.get("message").asText();
            }
            if (root.has("cause") && root.get("cause").isArray() && !root.get("cause").isEmpty()) {
                JsonNode first = root.get("cause").get(0);
                if (first.has("description")) {
                    return first.get("description").asText();
                }
            }
        } catch (JsonProcessingException ignored) {
            // usar fallback
        }
        return "No se pudo procesar el pago. Revisa los datos e intenta de nuevo.";
    }

}
