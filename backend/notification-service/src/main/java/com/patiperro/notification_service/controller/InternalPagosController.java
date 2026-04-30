package com.patiperro.notification_service.controller;

import com.patiperro.notification_service.service.PagoNotificacionService;
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
 * Endpoints internos ligados a pagos confirmados (servidor a servidor).
 * Se protegen con cabecera {@link #HEADER_INTERNO}, no con JWT de usuario.
 */
@RestController
@RequestMapping("/internal/pagos")
public class InternalPagosController {

    public static final String HEADER_INTERNO = "X-Patiperro-Interno-Secret";

    private final PagoNotificacionService pagoNotificacionService;

    @Value("${patiperro.notification.interno.secret:}")
    private String internoSecret;

    public InternalPagosController(PagoNotificacionService pagoNotificacionService) {
        this.pagoNotificacionService = pagoNotificacionService;
    }

    /**
     * Evento de pago confirmado (p. ej. invocado por {@code reserva-service} tras webhook Mercado Pago).
     * Body: {@code idReserva} obligatorio; {@code idPaseador} y {@code emailDestino} opcionales (sin correo no se envía Brevo).
     */
    @PostMapping("/confirmado")
    public ResponseEntity<Void> pagoConfirmado(
            @RequestHeader(value = HEADER_INTERNO, required = false) String secretoHeader,
            @RequestBody(required = false) PagoConfirmadoRequest body) {
        if (!StringUtils.hasText(internoSecret)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!internoSecret.equals(secretoHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (body == null || body.idReserva() == null) {
            return ResponseEntity.badRequest().build();
        }
        pagoNotificacionService.procesarPagoConfirmado(body.idReserva(), body.idPaseador(), body.emailDestino());
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code emailDestino}: correo del paseador (opcional); sin él no se envía Brevo pero el endpoint sigue siendo 204.
     */
    public record PagoConfirmadoRequest(Integer idReserva, Integer idPaseador, String emailDestino) {}
}
