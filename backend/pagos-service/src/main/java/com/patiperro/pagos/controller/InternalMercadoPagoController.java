package com.patiperro.pagos.controller;

import com.patiperro.pagos.service.MercadoPagoReembolsoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos Mercado Pago (servidor a servidor). Protegidos con cabecera secreta, no JWT.
 */
@RestController
@RequestMapping("/api/pagos/interno/mercadopago")
public class InternalMercadoPagoController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    private final MercadoPagoReembolsoService mercadoPagoReembolsoService;

    @Value("${patiperro.pagos.interno.secret:}")
    private String internoSecret;

    public InternalMercadoPagoController(MercadoPagoReembolsoService mercadoPagoReembolsoService) {
        this.mercadoPagoReembolsoService = mercadoPagoReembolsoService;
    }

    /**
     * Reembolso total del pago. Body: {@code { "mpPaymentId": "123", "idReserva": 1 }}.
     * Si {@code mpPaymentId} viene vacío, se resuelve desde la transacción aprobada de la reserva en BD local.
     */
    @PostMapping("/reembolso")
    public ResponseEntity<Void> reembolsoTotal(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestHeader(value = HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @RequestBody(required = false) ReembolsoRequest body) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        String mp = body.mpPaymentId() != null ? body.mpPaymentId().trim() : "";
        if (!StringUtils.hasText(mp) && body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }

        int code = mercadoPagoReembolsoService.procesarReembolsoTotal(
                body.idReserva(), mp, idempotencyKey);
        HttpStatus st = HttpStatus.resolve(code);
        if (st == null) {
            return ResponseEntity.status(code).build();
        }
        return ResponseEntity.status(st).build();
    }

    public record ReembolsoRequest(Integer idReserva, String mpPaymentId) {
    }
}
