package com.patiperro.reserva.controller;

import com.patiperro.reserva.dto.interno.InternoPagosErrorResponse;
import com.patiperro.reserva.service.ReservaPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos (servidor-a-servidor) para notificaciones de pago.
 * Cabecera interna validada por {@link JwtAuthenticationFilter} (rutas {@code /api/reserva/interno/**}).
 */
@RestController
@RequestMapping("/api/reserva/interno/pagos")
public class InternalPagosController {

    private static final Logger log = LoggerFactory.getLogger(InternalPagosController.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    /** Opcional: correlación servidor-a-servidor (logs / trazas); puede enviarla pagos-service. */
    public static final String HEADER_CORRELATION_ID = "X-Patiperro-Correlation-Id";

    private final ReservaPagoService reservaPagoService;

    public InternalPagosController(ReservaPagoService reservaPagoService) {
        this.reservaPagoService = reservaPagoService;
    }

    /**
     * Confirma pago aprobado desde pasarela (ej. Mercado Pago webhook procesado en pagos-service).
     * Body: {@code { "idReserva": 123, "idTransaccionPagos": 456, "mpPaymentId": "999999999" }}
     */
    /**
     * Al iniciar checkout en pagos-service: persiste {@code reserva.id_pago} = id transacción local.
     * Body: {@code { "idReserva": 123, "idTransaccionPagos": 456 }}
     */
    @PostMapping(value = "/mercadopago/vinculo-transaccion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> vincularTransaccionMercadoPago(
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId,
            @RequestBody(required = false) VinculoTransaccionRequest body) {
        logCorrelation(correlationId, "vinculo-transaccion");
        if (body == null || body.idReserva() == null || body.idTransaccionPagos() == null) {
            return badRequest("Faltan idReserva o idTransaccionPagos.");
        }
        reservaPagoService.vincularTransaccionPagos(body.idReserva(), body.idTransaccionPagos());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/mercadopago/pago-aprobado", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> marcarPagadaMercadoPago(
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId,
            @RequestBody(required = false) PagoAprobadoRequest body) {
        logCorrelation(correlationId, "pago-aprobado");
        if (body == null || body.idReserva() == null || body.idTransaccionPagos() == null) {
            return badRequest("Faltan idReserva o idTransaccionPagos.");
        }
        String mpId = body.mpPaymentId() != null ? body.mpPaymentId().trim() : null;
        reservaPagoService.marcarReservaComoPagada(body.idReserva(), body.idTransaccionPagos(), mpId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cobro no aprobado en Mercado Pago (webhook tras GET /payments).
     * Body: {@code { "idReserva": 123, "mpPaymentId": "...", "mpStatus": "rejected", "mpStatusDetail": "..." }}
     */
    @PostMapping(value = "/mercadopago/pago-no-aprobado", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registrarPagoNoAprobadoMercadoPago(
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId,
            @RequestBody(required = false) PagoNoAprobadoRequest body) {
        logCorrelation(correlationId, "pago-no-aprobado");
        if (body == null || body.idReserva() == null
                || !StringUtils.hasText(body.mpPaymentId())
                || !StringUtils.hasText(body.mpStatus())) {
            return badRequest("Faltan idReserva, mpPaymentId o mpStatus.");
        }
        reservaPagoService.registrarMercadoPagoNoAprobado(
                body.idReserva(),
                body.mpPaymentId().trim(),
                body.mpStatus().trim(),
                body.mpStatusDetail() != null ? body.mpStatusDetail().trim() : null);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<InternoPagosErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Petición inválida.";
        if (msg.contains("no encontrada")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new InternoPagosErrorResponse("RECURSO_NO_ENCONTRADO", msg));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new InternoPagosErrorResponse("SOLICITUD_INVALIDA", msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<InternoPagosErrorResponse> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Conflicto de estado.";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new InternoPagosErrorResponse("CONFLICTO_ESTADO", msg));
    }

    private static ResponseEntity<InternoPagosErrorResponse> badRequest(String mensaje) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new InternoPagosErrorResponse("SOLICITUD_INVALIDA", mensaje));
    }

    private static void logCorrelation(String correlationId, String operacion) {
        if (StringUtils.hasText(correlationId)) {
            log.debug("Interno pagos {} correlationId={}", operacion, correlationId.trim());
        }
    }

    public record VinculoTransaccionRequest(Integer idReserva, Long idTransaccionPagos) {
    }

    public record PagoAprobadoRequest(Integer idReserva, Long idTransaccionPagos, String mpPaymentId) {
    }

    public record PagoNoAprobadoRequest(Integer idReserva, String mpPaymentId, String mpStatus, String mpStatusDetail) {
    }
}
