package com.patiperro.reserva.controller;

import com.patiperro.reserva.service.ReservaPagoService;
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
 * Endpoints internos (servidor-a-servidor) para notificaciones de pago.
 * Se protegen con cabecera X-Patiperro-Interno-Secret (no JWT de usuario).
 */
@RestController
@RequestMapping("/api/reserva/interno/pagos")
public class InternalPagosController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final ReservaPagoService reservaPagoService;

    @Value("${patiperro.reserva.interno.secret:}")
    private String internoSecret;

    public InternalPagosController(ReservaPagoService reservaPagoService) {
        this.reservaPagoService = reservaPagoService;
    }

    /**
     * Confirma pago aprobado desde pasarela (ej. Mercado Pago webhook procesado en pagos-service).
     * Body: { "idReserva": 123, "mpPaymentId": "999999999" }
     */
    @PostMapping("/mercadopago/pago-aprobado")
    public ResponseEntity<Void> marcarPagadaMercadoPago(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) PagoAprobadoRequest body) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body == null || body.idReserva() == null || !StringUtils.hasText(body.mpPaymentId())) {
            return ResponseEntity.badRequest().build();
        }
        reservaPagoService.marcarReservaComoPagada(body.idReserva(), body.mpPaymentId().trim());
        return ResponseEntity.noContent().build();
    }

    public record PagoAprobadoRequest(Integer idReserva, String mpPaymentId) {
    }
}
