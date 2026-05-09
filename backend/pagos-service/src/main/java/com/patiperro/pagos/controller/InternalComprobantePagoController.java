package com.patiperro.pagos.controller;

import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.service.ComprobantePagoGeneracionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Generación de comprobante post-PAGADA (invocado por {@code reserva-service}). Secreto interno, sin JWT.
 * <p>Misma cabecera que {@link InternalMercadoPagoController} para coherencia operativa.</p>
 */
@RestController
@RequestMapping("/api/pagos/interno/comprobante")
public class InternalComprobantePagoController {

    /** Alias documentado; valor idéntico a {@link InternalMercadoPagoController#HEADER_INTERNO}. */
    public static final String HEADER_INTERNO = InternalMercadoPagoController.HEADER_INTERNO;

    /** Opcional; propagado desde reserva-service para correlacionar con logs downstream (notification-service). */
    public static final String HEADER_CORRELATION = "X-Patiperro-Comprobante-Correlation";

    private static final Logger log = LoggerFactory.getLogger(InternalComprobantePagoController.class);

    private final ComprobantePagoGeneracionService comprobantePagoGeneracionService;

    @Value("${patiperro.pagos.interno.secret:}")
    private String internoSecret;

    public InternalComprobantePagoController(ComprobantePagoGeneracionService comprobantePagoGeneracionService) {
        this.comprobantePagoGeneracionService = comprobantePagoGeneracionService;
    }

    /**
     * Body: {@code { "idReserva": 10, "reenviarCorreo": false }}.
     */
    @PostMapping("/generar")
    public ResponseEntity<Void> generar(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestHeader(value = HEADER_CORRELATION, required = false) String correlationId,
            @RequestBody(required = false) GenerarComprobanteRequest body) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        Integer idReserva = body.idReserva();
        if (log.isDebugEnabled() && StringUtils.hasText(correlationId)) {
            log.debug("Interno comprobante generar: correlation={} (idReserva={})", correlationId.trim(), idReserva);
        }
        boolean reenviar = Boolean.TRUE.equals(body.reenviarCorreo());
        try {
            comprobantePagoGeneracionService.generarPostPago(idReserva, reenviar);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return mapIllegalArgument(ex, idReserva);
        } catch (IllegalStateException ex) {
            return mapIllegalState(ex, idReserva);
        }
    }

    private ResponseEntity<Void> mapIllegalArgument(IllegalArgumentException ex, Integer idReserva) {
        String msg = ex.getMessage();
        if (ReservaConsultaClient.MSG_RESERVA_NO_ENCONTRADA_COMPROBANTE.equals(msg)) {
            log.debug("Interno comprobante generar: reserva no encontrada (idReserva={})", idReserva);
            return ResponseEntity.notFound().build();
        }
        if (ReservaConsultaClient.MSG_ID_RESERVA_OBLIGATORIO.equals(msg)) {
            log.warn("Interno comprobante generar: idReserva obligatorio inesperado (idReserva={})", idReserva);
            return ResponseEntity.badRequest().build();
        }
        log.warn(
                "Interno comprobante generar: argumento inválido (idReserva={}, tipo={})",
                idReserva,
                msg != null ? msg : "");
        return ResponseEntity.badRequest().build();
    }

    private ResponseEntity<Void> mapIllegalState(IllegalStateException ex, Integer idReserva) {
        String msg = ex.getMessage();
        if (ComprobantePagoGeneracionService.MSG_NO_PAGO_APROBADO.equals(msg)) {
            log.debug("Interno comprobante generar: sin pago aprobado (idReserva={})", idReserva);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if (ReservaConsultaClient.MSG_INTEGRACION_RESERVA_NO_CONFIGURADA.equals(msg)) {
            log.warn("Interno comprobante generar: integración reserva no configurada en pagos-service");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (ComprobantePagoGeneracionService.MSG_SNAPSHOT_JSON_ERROR.equals(msg)) {
            log.warn("Interno comprobante generar: error serializando snapshot (idReserva={})", idReserva, ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        if (ComprobantePagoGeneracionService.MSG_SIN_ID_TUTOR_EN_RESERVA.equals(msg)) {
            log.warn("Interno comprobante generar: reserva sin id tutor (idReserva={})", idReserva);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        if (ComprobantePagoGeneracionService.MSG_UPSERT_SIN_RESULTADO.equals(msg)) {
            log.warn("Interno comprobante generar: upsert sin resultado (idReserva={})", idReserva);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        if (ReservaConsultaClient.MSG_CONFLICTO_COMPROBANTE_INTERNO.equals(msg)) {
            log.warn("Interno comprobante generar: conflicto comprobante en reserva-service (idReserva={})", idReserva);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        if (ReservaConsultaClient.MSG_RESERVA_RED_NO_DISPONIBLE.equals(msg)) {
            log.warn("Interno comprobante generar: red reserva-service (idReserva={})", idReserva, ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        if (msg != null && msg.startsWith(ReservaConsultaClient.MSG_PREFIJO_RESERVA_HTTP_ERROR)) {
            log.warn("Interno comprobante generar: HTTP reserva-service (idReserva={}, detalle={})", idReserva, msg);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        log.warn("Interno comprobante generar: estado inesperado (idReserva={}, msg={})", idReserva, msg, ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    public record GenerarComprobanteRequest(Integer idReserva, Boolean reenviarCorreo) {}
}
