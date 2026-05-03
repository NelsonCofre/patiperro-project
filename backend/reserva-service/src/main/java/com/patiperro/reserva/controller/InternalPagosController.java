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
 * Cabecera interna validada por {@link com.patiperro.reserva.config.JwtAuthenticationFilter} (rutas {@code /api/reserva/interno/**}).
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
     * Enlaza la reserva con {@code transaccion.id_transaccion} en pagos-service (tras crear/actualizar transacción al iniciar checkout).
     */
    @PostMapping("/mercadopago/vinculo-transaccion")
    public ResponseEntity<Void> vincularTransaccion(@RequestBody(required = false) VinculoTransaccionRequest body) {
        if (body == null || body.idReserva() == null || body.idTransaccionPagos() == null) {
            return ResponseEntity.badRequest().build();
        }
        reservaPagoService.vincularTransaccionPagos(body.idReserva(), body.idTransaccionPagos());
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirma pago aprobado desde pasarela (webhook en pagos-service).
     * Body: {@code { "idReserva": 123, "idTransaccionPagos": 456, "mpPaymentId": "..." }} (mpPaymentId opcional).
     */
    @PostMapping("/mercadopago/pago-aprobado")
    public ResponseEntity<Void> marcarPagadaMercadoPago(
            @RequestBody(required = false) PagoAprobadoRequest body) {
        if (body == null || body.idReserva() == null || body.idTransaccionPagos() == null) {
            return ResponseEntity.badRequest().build();
        }
        String mp = StringUtils.hasText(body.mpPaymentId()) ? body.mpPaymentId().trim() : null;
        reservaPagoService.marcarReservaComoPagada(body.idReserva(), body.idTransaccionPagos(), mp);
        return ResponseEntity.noContent().build();
    }

    public record VinculoTransaccionRequest(Integer idReserva, Long idTransaccionPagos) {
    }

    public record PagoAprobadoRequest(Integer idReserva, Long idTransaccionPagos, String mpPaymentId) {
    }
}
