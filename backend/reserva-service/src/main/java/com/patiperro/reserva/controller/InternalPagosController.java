package com.patiperro.reserva.controller;

import com.patiperro.reserva.service.ReservaPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos (servidor-a-servidor) para notificaciones de pago.
 * Cabecera interna validada por {@link JwtAuthenticationFilter} (rutas {@code /api/reserva/interno/**}).
 */
@RestController
@RequestMapping("/api/reserva/interno/pagos")
public class InternalPagosController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final ReservaPagoService reservaPagoService;

    public InternalPagosController(ReservaPagoService reservaPagoService) {
        this.reservaPagoService = reservaPagoService;
    }

    /**
     * Confirma pago aprobado desde pasarela (ej. Mercado Pago webhook procesado en pagos-service).
     * Body: { "idReserva": 123, "mpPaymentId": "999999999" }
     */
    @PostMapping("/mercadopago/pago-aprobado")
    public ResponseEntity<Void> marcarPagadaMercadoPago(
            @RequestBody(required = false) PagoAprobadoRequest body) {
        if (body == null || body.idReserva() == null || !StringUtils.hasText(body.mpPaymentId())) {
            return ResponseEntity.badRequest().build();
        }
        reservaPagoService.marcarReservaComoPagada(body.idReserva(), body.mpPaymentId().trim());
        return ResponseEntity.noContent().build();
    }

    public record PagoAprobadoRequest(Integer idReserva, String mpPaymentId) {
    }
}
