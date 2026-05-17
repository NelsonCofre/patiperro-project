package com.patiperro.notification_service.controller;

import com.patiperro.notification_service.service.PagoNotificacionService;
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

import java.util.Optional;

/**
 * Endpoints internos ligados a pagos confirmados (servidor a servidor).
 * Se protegen con cabecera {@link #HEADER_INTERNO}, no con JWT de usuario.
 */
@RestController
@RequestMapping("/internal/pagos")
public class InternalPagosController {

    private static final Logger log = LoggerFactory.getLogger(InternalPagosController.class);

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    /**
     * Opcional en respuestas de error para distinguir causas sin romper cuerpo vacío ({@code Void});
     * los clientes pueden ignorarla.
     */
    public static final String HEADER_INTERNAL_REASON = "X-Patiperro-Internal-Reason";

    /** Eco opcional desde pagos-service para correlacionar logs. */
    public static final String HEADER_CORRELATION = "X-Patiperro-Comprobante-Correlation";

    private static final String REASON_SECRET_NOT_CONFIGURED = "secret-not-configured";

    private static final String REASON_RESUMEN_DISABLED = "resumen-comprobante-tutor-disabled";

    private static final String REASON_PAYLOAD_TOO_LARGE = "payload-too-large";

    private static final String REASON_DOWNSTREAM_FAILURE = "downstream-failure";

    private static final String REASON_LIBERACION_CONSOLIDADA_DISABLED = "liberacion-consolidada-disabled";

    /** Validación de payload (correo largo, monto textual enorme). */
    private static final String REASON_VALIDATION = "validation-failed";

    private final PagoNotificacionService pagoNotificacionService;

    @Value("${patiperro.notification.interno.secret:}")
    private String internoSecret;

    @Value("${patiperro.notification.resumen-comprobante-tutor.enabled:true}")
    private boolean resumenComprobanteTutorEnabled;

    @Value("${patiperro.notification.resumen-comprobante-tutor.max-html-chars:200000}")
    private int resumenComprobanteTutorMaxHtmlChars;

    @Value("${patiperro.notification.liberacion-fondos-consolidada-paseador.enabled:true}")
    private boolean liberacionFondosConsolidadaPaseadorEnabled;

    public InternalPagosController(PagoNotificacionService pagoNotificacionService) {
        this.pagoNotificacionService = pagoNotificacionService;
    }

    /**
     * Aviso consolidado al paseador tras liberación nocturna (pagos-service).
     * <p>Sin {@code emailDestino} no hay envío Brevo (204). Fallo Brevo → 502 con {@link #HEADER_INTERNAL_REASON}.
     * {@code patiperro.notification.liberacion-fondos-consolidada-paseador.enabled=false} → 503.</p>
     */
    @PostMapping("/liberacion-fondos-consolidada-paseador")
    public ResponseEntity<Void> liberacionFondosConsolidadaPaseador(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) LiberacionFondosConsolidadaPaseadorRequest body) {
        Optional<ResponseEntity<Void>> secret = rechazarSiSecretoInvalido(secretoHeader);
        if (secret.isPresent()) {
            return secret.get();
        }
        if (body == null
                || body.idUsuarioPaseador() == null
                || body.cantidadReservasLiberadas() == null
                || body.cantidadReservasLiberadas() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        Optional<ResponseEntity<Void>> invalid = rechazarLiberacionPayloadInvalido(body);
        if (invalid.isPresent()) {
            return invalid.get();
        }
        if (!liberacionFondosConsolidadaPaseadorEnabled) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HEADER_INTERNAL_REASON, REASON_LIBERACION_CONSOLIDADA_DISABLED)
                    .build();
        }
        var resultado = pagoNotificacionService.procesarLiberacionFondosConsolidadaPaseador(
                body.idUsuarioPaseador(),
                body.emailDestino(),
                body.montoTotalNeto(),
                body.cantidadReservasLiberadas());
        if (!resultado.respuestaHttpExitosa()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .header(HEADER_INTERNAL_REASON, REASON_DOWNSTREAM_FAILURE)
                    .build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Evento de pago confirmado (p. ej. invocado por {@code reserva-service} tras webhook Mercado Pago).
     * Body: {@code idReserva} obligatorio; {@code idPaseador} y {@code emailDestino} opcionales (sin correo no se envía Brevo).
     */
    @PostMapping("/confirmado")
    public ResponseEntity<Void> pagoConfirmado(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) PagoConfirmadoRequest body) {
        return rechazarSiSecretoInvalido(secretoHeader)
                .or(() -> rechazarSiCuerpoInvalido(body == null || body.idReserva() == null))
                .orElseGet(() -> {
                    pagoNotificacionService.procesarPagoConfirmado(body.idReserva(), body.idPaseador(), body.emailDestino());
                    return ResponseEntity.noContent().build();
                });
    }

    /**
     * Aviso al tutor: devolución procesada en la pasarela. {@code emailDestino} opcional (sin correo no hay envío Brevo).
     */
    @PostMapping("/reembolso-tutor")
    public ResponseEntity<Void> reembolsoTutor(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) ReembolsoTutorRequest body) {
        return rechazarSiSecretoInvalido(secretoHeader)
                .or(() -> rechazarSiCuerpoInvalido(body == null || body.idReserva() == null))
                .orElseGet(() -> {
                    pagoNotificacionService.procesarReembolsoTutor(body.idReserva(), body.emailDestino());
                    return ResponseEntity.noContent().build();
                });
    }

    /**
     * Aviso al tutor: reembolso procesado en pasarela (mismo evento Brevo {@code REEMBOLSO_RESERVA} que {@code /reembolso-tutor};
     * permite incluir {@code idTutor} para variables de plantilla). Sin {@code emailDestino} no hay envío Brevo (204 igualmente).
     */
    @PostMapping("/reembolso-procesado")
    public ResponseEntity<Void> reembolsoProcesado(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) ReembolsoProcesadoRequest body) {
        return rechazarSiSecretoInvalido(secretoHeader)
                .or(() -> rechazarSiCuerpoInvalido(body == null || body.idReserva() == null))
                .orElseGet(() -> {
                    pagoNotificacionService.procesarReembolsoProcesado(body.idReserva(), body.emailDestino(), body.idTutor());
                    return ResponseEntity.noContent().build();
                });
    }

    /**
     * Resumen de transacción al tutor (HTML). Sin {@code emailDestino} no hay envío Brevo (204).
     * <p>{@code patiperro.notification.resumen-comprobante-tutor.enabled=false} → 503 con cabecera {@link #HEADER_INTERNAL_REASON}.
     * HTML mayor que {@code max-html-chars} → 413. Fallo Brevo → 502.</p>
     */
    @PostMapping("/resumen-comprobante-tutor")
    public ResponseEntity<Void> resumenComprobanteTutor(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestHeader(value = HEADER_CORRELATION, required = false) String correlationId,
            @RequestBody(required = false) ResumenComprobanteTutorRequest body) {
        Optional<ResponseEntity<Void>> secret = rechazarSiSecretoInvalido(secretoHeader);
        if (secret.isPresent()) {
            return secret.get();
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (log.isDebugEnabled() && StringUtils.hasText(correlationId)) {
            log.debug("Interno resumen comprobante: correlation={} (idReserva={})", correlationId.trim(), body.idReserva());
        }
        if (!resumenComprobanteTutorEnabled) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HEADER_INTERNAL_REASON, REASON_RESUMEN_DISABLED)
                    .build();
        }
        int maxChars = clampHtmlMaxChars(resumenComprobanteTutorMaxHtmlChars);
        String html = body.cuerpoHtml();
        if (html != null && html.length() > maxChars) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .header(HEADER_INTERNAL_REASON, REASON_PAYLOAD_TOO_LARGE)
                    .build();
        }
        boolean ok = pagoNotificacionService.procesarResumenComprobanteTutor(
                body.idReserva(), body.emailDestino(), body.cuerpoHtml());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .header(HEADER_INTERNAL_REASON, REASON_DOWNSTREAM_FAILURE)
                    .build();
        }
        return ResponseEntity.noContent().build();
    }

    private Optional<ResponseEntity<Void>> rechazarSiSecretoInvalido(String secretoHeader) {
        if (!StringUtils.hasText(internoSecret)) {
            return Optional.of(
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .header(HEADER_INTERNAL_REASON, REASON_SECRET_NOT_CONFIGURED)
                            .build());
        }
        if (!internoSecret.equals(secretoHeader)) {
            return Optional.of(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        return Optional.empty();
    }

    private static Optional<ResponseEntity<Void>> rechazarSiCuerpoInvalido(boolean invalido) {
        if (invalido) {
            return Optional.of(ResponseEntity.badRequest().build());
        }
        return Optional.empty();
    }

    private static int clampHtmlMaxChars(int raw) {
        return Math.max(10_000, Math.min(2_000_000, raw));
    }

    /**
     * Límites defensivos para evitar abuso por payload enorme (correo/monto).
     */
    private Optional<ResponseEntity<Void>> rechazarLiberacionPayloadInvalido(LiberacionFondosConsolidadaPaseadorRequest body) {
        String email = body.emailDestino();
        if (email != null && email.length() > 254) {
            return Optional.of(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).header(HEADER_INTERNAL_REASON, REASON_VALIDATION).build());
        }
        String monto = body.montoTotalNeto();
        if (monto != null && monto.length() > 48) {
            return Optional.of(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).header(HEADER_INTERNAL_REASON, REASON_VALIDATION).build());
        }
        if (body.cantidadReservasLiberadas() > 50_000) {
            return Optional.of(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).header(HEADER_INTERNAL_REASON, REASON_VALIDATION).build());
        }
        return Optional.empty();
    }

    /**
     * {@code emailDestino}: correo del paseador (opcional); sin él no se envía Brevo pero el endpoint sigue siendo 204.
     */
    public record PagoConfirmadoRequest(Integer idReserva, Integer idPaseador, String emailDestino) {}

    public record ReembolsoTutorRequest(Integer idReserva, String emailDestino) {}

    /** {@code idTutor} opcional (solo variables plantilla; el correo sigue siendo {@code emailDestino}). */
    public record ReembolsoProcesadoRequest(Integer idReserva, String emailDestino, Integer idTutor) {}

    /** {@code cuerpoHtml}: HTML completo del resumen (plantilla Brevo con variable sin escapar). */
    public record ResumenComprobanteTutorRequest(Integer idReserva, String emailDestino, String cuerpoHtml) {}

    /** {@code montoTotalNeto}: cadena decimal CLP (ej. desde {@link java.math.BigDecimal#toPlainString()}). */
    public record LiberacionFondosConsolidadaPaseadorRequest(
            Long idUsuarioPaseador, String emailDestino, String montoTotalNeto, Integer cantidadReservasLiberadas) {}
}
